-- =====================================================
-- VENDING EVENT MODE - DATABASE EXTENSIONS
-- =====================================================
-- Add to existing vending_schema.sql or run separately

-- =====================================================
-- 1. EVENTS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    venue_name TEXT NOT NULL,
    event_type TEXT NOT NULL CHECK (event_type IN ('WEDDING', 'CONFERENCE', 'CORPORATE', 'STADIUM', 'CONCERT', 'PRIVATE_PARTY', 'OTHER')),
    start_time BIGINT NOT NULL,
    end_time BIGINT NOT NULL,
    status TEXT NOT NULL DEFAULT 'UPCOMING' CHECK (status IN ('UPCOMING', 'ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    
    -- Service configuration (JSON)
    service_config JSONB NOT NULL,
    /*
    {
        "defaultServiceMode": "TABLE_SERVICE",
        "allowedServiceModes": ["SELF_SERVE", "TABLE_SERVICE"],
        "locationType": "TABLE",
        "locationRequired": false,
        "locationLabel": "Table Number",
        "locationPlaceholder": "e.g., 12",
        "locationOptions": ["1", "2", "3", ...],
        "codeExpiryMinutes": 15,
        "maxCupsPerSession": 10,
        "maxAlcoholPerSession": 6,
        "showActiveOrdersBoard": true,
        "busyModeEnabled": true,
        "requireStaffAuth": false
    }
    */
    
    -- Budget configuration (JSON, nullable)
    budget_config JSONB,
    /*
    {
        "budgetType": "OPEN_BAR" | "GUEST_ALLOWANCE" | "HYBRID",
        "totalBudget": 1000000,
        "spentAmount": 0,
        "guestAllowance": {
            "maxDrinks": 5,
            "maxAlcohol": 3,
            "categories": {"beer": 2, "cocktail": 1}
        },
        "pauseWhenDepleted": true
    }
    */
    
    -- Linked machines (array of UUIDs)
    machine_ids UUID[] NOT NULL DEFAULT '{}',
    
    -- Branding
    branding_image_url TEXT,
    primary_color TEXT,
    
    -- Ownership
    host_user_id UUID REFERENCES auth.users(id) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vending_events_host ON vending_events(host_user_id);
CREATE INDEX IF NOT EXISTS idx_vending_events_status ON vending_events(status);
CREATE INDEX IF NOT EXISTS idx_vending_events_time ON vending_events(start_time, end_time);

-- =====================================================
-- 2. EVENT GUESTS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_event_guests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID REFERENCES vending_events(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES auth.users(id) NOT NULL,
    guest_name TEXT NOT NULL,
    phone_last_four TEXT NOT NULL,
    
    -- Consumption tracking
    drinks_consumed INTEGER NOT NULL DEFAULT 0,
    alcohol_consumed INTEGER NOT NULL DEFAULT 0,
    amount_spent BIGINT NOT NULL DEFAULT 0,
    free_allowance_used BIGINT NOT NULL DEFAULT 0,
    
    -- Status
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'COMPLETED')),
    joined_at BIGINT NOT NULL,
    age_verified BOOLEAN NOT NULL DEFAULT false,
    
    UNIQUE(event_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_event_guests_event ON vending_event_guests(event_id);
CREATE INDEX IF NOT EXISTS idx_event_guests_user ON vending_event_guests(user_id);

-- =====================================================
-- 3. EVENT STAFF TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_event_staff (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID REFERENCES vending_events(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES auth.users(id) NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('WAITER', 'SUPERVISOR', 'BARTENDER', 'RUNNER')),
    pin TEXT,  -- Hashed PIN for staff auth
    allowed_categories TEXT[],  -- null = all categories allowed
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_event_staff_event ON vending_event_staff(event_id);

-- =====================================================
-- 4. EXTEND VENDING_ORDERS for Event Mode
-- =====================================================
ALTER TABLE vending_orders
ADD COLUMN IF NOT EXISTS event_id UUID REFERENCES vending_events(id),
ADD COLUMN IF NOT EXISTS service_mode TEXT DEFAULT 'SELF_SERVE' CHECK (service_mode IN ('SELF_SERVE', 'TABLE_SERVICE', 'ZONE_SERVICE', 'PICKUP')),
ADD COLUMN IF NOT EXISTS delivery_location JSONB,
/*
{
    "type": "TABLE" | "ZONE" | "SECTION" | "SEAT" | "PICKUP_POINT",
    "label": "Table" | "Zone" | "Section" | etc.,
    "value": "12" | "VIP West" | etc.
}
*/
ADD COLUMN IF NOT EXISTS staff_id UUID REFERENCES vending_event_staff(id),
ADD COLUMN IF NOT EXISTS quantity INTEGER NOT NULL DEFAULT 1,
ADD COLUMN IF NOT EXISTS remaining_serves INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS cups_included BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN IF NOT EXISTS funded_by TEXT DEFAULT 'WALLET' CHECK (funded_by IN ('WALLET', 'EVENT_BUDGET'));

CREATE INDEX IF NOT EXISTS idx_vending_orders_event ON vending_orders(event_id) WHERE event_id IS NOT NULL;

-- =====================================================
-- 5. FUNCTION: Create Event Order (Enhanced)
-- =====================================================
CREATE OR REPLACE FUNCTION create_event_vending_order(
    p_event_id UUID,
    p_user_id UUID,
    p_machine_id UUID,
    p_amount BIGINT,
    p_service_mode TEXT,
    p_delivery_location JSONB,
    p_quantity INTEGER,
    p_cups_included BOOLEAN,
    p_use_event_budget BOOLEAN
)
RETURNS TABLE(
    order_id UUID,
    order_status TEXT,
    code TEXT,
    code_expires_at BIGINT,
    wallet_balance BIGINT
) AS $$
DECLARE
    v_wallet_balance BIGINT;
    v_event RECORD;
    v_guest RECORD;
    v_new_order_id UUID;
    v_machine RECORD;
    v_product RECORD;
    v_code TEXT;
    v_code_hash TEXT;
    v_code_expires_at BIGINT;
    v_created_at BIGINT;
    v_expiry_minutes INTEGER;
BEGIN
    -- Get event details
    SELECT * INTO v_event
    FROM vending_events
    WHERE id = p_event_id;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Event not found';
    END IF;
    
    IF v_event.status != 'ACTIVE' THEN
        RAISE EXCEPTION 'Event is not active';
    END IF;
    
    -- Get guest record
    SELECT * INTO v_guest
    FROM vending_event_guests
    WHERE event_id = p_event_id AND user_id = p_user_id;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Guest not registered for this event';
    END IF;
    
    IF v_guest.status != 'ACTIVE' THEN
        RAISE EXCEPTION 'Guest is not active';
    END IF;
    
    -- Handle payment
    IF p_use_event_budget THEN
        -- Check event budget
        IF v_event.budget_config IS NULL THEN
            RAISE EXCEPTION 'Event budget not configured';
        END IF;
        
        IF (v_event.budget_config->>'spentAmount')::BIGINT + p_amount > (v_event.budget_config->>'totalBudget')::BIGINT THEN
            RAISE EXCEPTION 'Event budget exceeded';
        END IF;
        
        -- Update event budget
        UPDATE vending_events
        SET budget_config = jsonb_set(
            budget_config,
            '{spentAmount}',
            to_jsonb((budget_config->>'spentAmount')::BIGINT + p_amount)
        )
        WHERE id = p_event_id;
        
        -- Update guest tracking
        UPDATE vending_event_guests
        SET free_allowance_used = free_allowance_used + p_amount,
            drinks_consumed = drinks_consumed + p_quantity
        WHERE id = v_guest.id;
        
        v_wallet_balance := (SELECT total_tokens FROM wallets WHERE user_id = p_user_id);
    ELSE
        -- Use personal wallet (existing logic)
        SELECT total_tokens INTO v_wallet_balance
        FROM wallets
        WHERE user_id = p_user_id
        FOR UPDATE;
        
        IF v_wallet_balance IS NULL THEN
            RAISE EXCEPTION 'Wallet not found';
        END IF;
        
        IF v_wallet_balance < p_amount THEN
            RAISE EXCEPTION 'Insufficient balance';
        END IF;
        
        UPDATE wallets
        SET total_tokens = total_tokens - p_amount,
            updated_at = NOW()
        WHERE user_id = p_user_id;
        
        v_wallet_balance := v_wallet_balance - p_amount;
        
        -- Update guest tracking
        UPDATE vending_event_guests
        SET amount_spent = amount_spent + p_amount,
            drinks_consumed = drinks_consumed + p_quantity
        WHERE id = v_guest.id;
    END IF;
    
    -- Get machine and product details
    SELECT * INTO v_machine FROM vending_machines WHERE id = p_machine_id;
    SELECT * INTO v_product FROM vending_products WHERE id = v_machine.product_id;
    
    -- Generate code with event-specific expiry
    v_expiry_minutes := COALESCE((v_event.service_config->>'codeExpiryMinutes')::INTEGER, 15);
    v_code := generate_vending_code();
    v_code_hash := encode(digest(v_code, 'sha256'), 'hex');
    v_code_expires_at := (EXTRACT(EPOCH FROM NOW()) * 1000 + (v_expiry_minutes * 60 * 1000))::BIGINT;
    v_created_at := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    
    -- Create order
    INSERT INTO vending_orders (
        user_id, machine_id, machine_name, machine_location,
        product_name, product_size_ml, amount, status, created_at,
        event_id, service_mode, delivery_location, quantity,
        remaining_serves, cups_included, funded_by
    ) VALUES (
        p_user_id, p_machine_id, v_machine.name, v_machine.location,
        v_product.name, v_product.size_ml, p_amount, 'CODE_GENERATED', v_created_at,
        p_event_id, p_service_mode, p_delivery_location, p_quantity,
        p_quantity, p_cups_included, CASE WHEN p_use_event_budget THEN 'EVENT_BUDGET' ELSE 'WALLET' END
    ) RETURNING id INTO v_new_order_id;
    
    -- Create code
    INSERT INTO vending_codes (order_id, machine_id, code_hash, code_plain, expires_at)
    VALUES (v_new_order_id, p_machine_id, v_code_hash, v_code, v_code_expires_at);
    
    -- Record transaction if wallet was used
    IF NOT p_use_event_budget THEN
        INSERT INTO vending_transactions (
            order_id, user_id, amount, type, wallet_balance_before, wallet_balance_after
        ) VALUES (
            v_new_order_id, p_user_id, p_amount, 'DEBIT', v_wallet_balance + p_amount, v_wallet_balance
        );
    END IF;
    
    RETURN QUERY SELECT 
        v_new_order_id,
        'CODE_GENERATED'::TEXT,
        v_code,
        v_code_expires_at,
        v_wallet_balance;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 6. FUNCTION: Get Active Event Orders (For Machine UI)
-- =====================================================
CREATE OR REPLACE FUNCTION get_active_event_orders(
    p_event_id UUID,
    p_machine_id UUID,
    p_service_mode TEXT DEFAULT NULL
)
RETURNS TABLE(
    order_id UUID,
    order_number TEXT,
    phone_last_four TEXT,
    delivery_location JSONB,
    product_name TEXT,
    quantity INTEGER,
    remaining_serves INTEGER,
    cups_included BOOLEAN,
    status TEXT,
    created_at BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        vo.id AS order_id,
        '#E' || UPPER(SUBSTRING(vo.id::TEXT FROM 1 FOR 4)) AS order_number,
        SUBSTRING(u.phone FROM LENGTH(u.phone) - 3) AS phone_last_four,
        vo.delivery_location,
        vo.product_name,
        vo.quantity,
        vo.remaining_serves,
        vo.cups_included,
        vo.status,
        vo.created_at
    FROM vending_orders vo
    JOIN auth.users u ON vo.user_id = u.id
    WHERE vo.event_id = p_event_id
    AND vo.machine_id = p_machine_id
    AND vo.status IN ('CODE_GENERATED', 'PENDING')
    AND (p_service_mode IS NULL OR vo.service_mode = p_service_mode)
    ORDER BY vo.created_at ASC;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 7. FUNCTION: Serve Order (Staff Action)
-- =====================================================
CREATE OR REPLACE FUNCTION serve_event_order(
    p_order_id UUID,
    p_staff_id UUID,
    p_serves_count INTEGER DEFAULT 1
)
RETURNS TABLE(
    success BOOLEAN,
    remaining INTEGER
) AS $$
DECLARE
    v_order RECORD;
BEGIN
    -- Get order
    SELECT * INTO v_order
    FROM vending_orders
    WHERE id = p_order_id
    FOR UPDATE;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found';
    END IF;
    
    IF v_order.remaining_serves < p_serves_count THEN
        RAISE EXCEPTION 'Not enough serves remaining';
    END IF;
    
    -- Update remaining serves
    UPDATE vending_orders
    SET remaining_serves = remaining_serves - p_serves_count,
        staff_id = p_staff_id,
        status = CASE 
            WHEN remaining_serves - p_serves_count = 0 THEN 'DISPENSED'
            ELSE 'PENDING'
        END,
        updated_at = NOW()
    WHERE id = p_order_id;
    
    RETURN QUERY SELECT 
        true,
        v_order.remaining_serves - p_serves_count;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 8. FUNCTION: Join Event (Guest Registration)
-- =====================================================
CREATE OR REPLACE FUNCTION join_vending_event(
    p_event_code TEXT,
    p_user_id UUID,
    p_guest_name TEXT,
    p_phone_last_four TEXT
)
RETURNS TABLE(
    event_id UUID,
    guest_id UUID
) AS $$
DECLARE
    v_event UUID;
    v_guest_id UUID;
BEGIN
    -- Find event by code (you'll need to add event_code column)
    SELECT id INTO v_event
    FROM vending_events
    WHERE event_code = p_event_code
    AND status = 'ACTIVE';
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Invalid event code or event not active';
    END IF;
    
    -- Create or update guest
    INSERT INTO vending_event_guests (
        event_id, user_id, guest_name, phone_last_four, joined_at
    ) VALUES (
        v_event, p_user_id, p_guest_name, p_phone_last_four, 
        (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
    )
    ON CONFLICT (event_id, user_id) 
    DO UPDATE SET status = 'ACTIVE'
    RETURNING id INTO v_guest_id;
    
    RETURN QUERY SELECT v_event, v_guest_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 9. Add event_code to events table
-- =====================================================
ALTER TABLE vending_events
ADD COLUMN IF NOT EXISTS event_code TEXT UNIQUE;

-- Function to generate event code
CREATE OR REPLACE FUNCTION generate_event_code()
RETURNS TEXT AS $$
BEGIN
    RETURN UPPER(SUBSTRING(MD5(RANDOM()::TEXT) FROM 1 FOR 6));
END;
$$ LANGUAGE plpgsql;

-- Auto-generate event code on insert
CREATE OR REPLACE FUNCTION set_event_code()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.event_code IS NULL THEN
        NEW.event_code := generate_event_code();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_set_event_code
BEFORE INSERT ON vending_events
FOR EACH ROW EXECUTE FUNCTION set_event_code();

-- =====================================================
-- EVENT MODE SCHEMA COMPLETE ✅
-- =====================================================
