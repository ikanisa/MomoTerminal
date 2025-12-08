-- =====================================================
-- VENDING EVENT MODE DATABASE SCHEMA
-- =====================================================
-- Extends the base vending system with event capabilities
-- Supports: Weddings, Conferences, Stadiums, Corporate events
-- Features: Host budgets, table/zone service, staff management

-- =====================================================
-- 1. EVENTS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    venue_name VARCHAR(255) NOT NULL,
    venue_address TEXT,
    
    -- Event timing
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    
    -- Host information
    host_user_id UUID NOT NULL REFERENCES auth.users(id),
    host_organization VARCHAR(255),
    
    -- Event code for guest joining
    event_code VARCHAR(10) UNIQUE NOT NULL,
    qr_code_url TEXT,
    
    -- Service configuration
    service_config JSONB NOT NULL DEFAULT '{
        "defaultServiceMode": "SELF_SERVE",
        "allowedServiceModes": ["SELF_SERVE"],
        "locationType": "TABLE",
        "locationOptions": [],
        "busyMode": false,
        "sessionSettings": {
            "codeExpiryMinutes": 10,
            "maxQuantityPerSession": 10
        }
    }'::jsonb,
    
    -- Budget configuration
    budget_config JSONB NOT NULL DEFAULT '{
        "budgetType": "OPEN_BAR",
        "totalBudget": 0,
        "budgetSpent": 0,
        "pauseWhenDepleted": true
    }'::jsonb,
    
    -- Branding (optional)
    branding_config JSONB DEFAULT '{}'::jsonb,
    
    -- Metadata
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT valid_event_dates CHECK (ends_at > starts_at)
);

-- Indexes for events
CREATE INDEX idx_vending_events_event_code ON vending_events(event_code);
CREATE INDEX idx_vending_events_host ON vending_events(host_user_id);
CREATE INDEX idx_vending_events_status ON vending_events(status);
CREATE INDEX idx_vending_events_dates ON vending_events(starts_at, ends_at);

-- =====================================================
-- 2. EVENT GUESTS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_event_guests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES vending_events(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Guest identification
    phone_last_4 VARCHAR(4),
    guest_name VARCHAR(255),
    
    -- Consumption tracking
    total_orders INT DEFAULT 0,
    total_drinks INT DEFAULT 0,
    total_amount BIGINT DEFAULT 0,
    
    -- Allowances (if GUEST_ALLOWANCE budget type)
    allowance_config JSONB DEFAULT '{
        "maxDrinks": 0,
        "maxAlcohol": 0,
        "drinksByCategory": {}
    }'::jsonb,
    allowance_used JSONB DEFAULT '{
        "totalDrinks": 0,
        "totalAlcohol": 0,
        "drinksByCategory": {}
    }'::jsonb,
    
    -- Age verification (for alcohol)
    age_verified BOOLEAN DEFAULT false,
    age_verified_at TIMESTAMPTZ,
    
    -- Metadata
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT unique_event_guest UNIQUE(event_id, user_id)
);

-- Indexes for event guests
CREATE INDEX idx_vending_event_guests_event ON vending_event_guests(event_id);
CREATE INDEX idx_vending_event_guests_user ON vending_event_guests(user_id);

-- =====================================================
-- 3. EVENT STAFF TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_event_staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES vending_events(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Staff details
    staff_name VARCHAR(255) NOT NULL,
    staff_pin VARCHAR(6) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('WAITER', 'RUNNER', 'SUPERVISOR', 'BARTENDER')),
    
    -- Permissions
    allowed_categories JSONB DEFAULT '[]'::jsonb,
    can_serve_alcohol BOOLEAN DEFAULT false,
    
    -- Stats
    orders_served INT DEFAULT 0,
    last_served_at TIMESTAMPTZ,
    
    -- Metadata
    created_at TIMESTAMPTZ DEFAULT NOW(),
    is_active BOOLEAN DEFAULT true,
    
    -- Constraints
    CONSTRAINT unique_event_staff UNIQUE(event_id, user_id)
);

-- Indexes for event staff
CREATE INDEX idx_vending_event_staff_event ON vending_event_staff(event_id);
CREATE INDEX idx_vending_event_staff_pin ON vending_event_staff(event_id, staff_pin);

-- =====================================================
-- 4. EXTEND VENDING_ORDERS FOR EVENTS
-- =====================================================
-- Add event-specific columns to existing vending_orders table
ALTER TABLE vending_orders 
    ADD COLUMN IF NOT EXISTS event_id UUID REFERENCES vending_events(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS service_mode VARCHAR(50) CHECK (service_mode IN ('SELF_SERVE', 'TABLE_SERVICE', 'ZONE_SERVICE', 'PICKUP')),
    ADD COLUMN IF NOT EXISTS delivery_location JSONB,
    ADD COLUMN IF NOT EXISTS include_cups BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS cups_quantity INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS remaining_serves INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS served_by_staff_id UUID REFERENCES vending_event_staff(id),
    ADD COLUMN IF NOT EXISTS served_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS paid_by_event_budget BOOLEAN DEFAULT false;

-- Indexes for event orders
CREATE INDEX IF NOT EXISTS idx_vending_orders_event ON vending_orders(event_id) WHERE event_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_vending_orders_event_active ON vending_orders(event_id, status) WHERE status IN ('PENDING', 'ACTIVE');

-- =====================================================
-- 5. FUNCTION: Generate Unique Event Code
-- =====================================================
CREATE OR REPLACE FUNCTION generate_event_code()
RETURNS VARCHAR(10) AS $$
DECLARE
    new_code VARCHAR(10);
    code_exists BOOLEAN;
BEGIN
    LOOP
        -- Generate 6-character alphanumeric code (uppercase)
        new_code := upper(substring(md5(random()::text || clock_timestamp()::text) from 1 for 6));
        
        -- Check if code already exists
        SELECT EXISTS(SELECT 1 FROM vending_events WHERE event_code = new_code) INTO code_exists;
        
        EXIT WHEN NOT code_exists;
    END LOOP;
    
    RETURN new_code;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 6. FUNCTION: Create Event Vending Order
-- =====================================================
CREATE OR REPLACE FUNCTION create_event_vending_order(
    p_user_id UUID,
    p_event_id UUID,
    p_machine_id UUID,
    p_product_id UUID,
    p_quantity INT,
    p_service_mode VARCHAR(50),
    p_delivery_location JSONB,
    p_include_cups BOOLEAN,
    p_use_event_budget BOOLEAN
)
RETURNS JSONB AS $$
DECLARE
    v_event vending_events%ROWTYPE;
    v_guest vending_event_guests%ROWTYPE;
    v_product vending_products%ROWTYPE;
    v_wallet wallets%ROWTYPE;
    v_total_amount BIGINT;
    v_order_id UUID;
    v_code VARCHAR(6);
    v_expires_at TIMESTAMPTZ;
    v_budget_spent BIGINT;
BEGIN
    -- 1. Validate event
    SELECT * INTO v_event FROM vending_events WHERE id = p_event_id AND status = 'ACTIVE';
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Event not found or not active';
    END IF;
    
    -- Check event timing
    IF NOW() < v_event.starts_at OR NOW() > v_event.ends_at THEN
        RAISE EXCEPTION 'Event not currently active';
    END IF;
    
    -- 2. Validate guest
    SELECT * INTO v_guest FROM vending_event_guests WHERE event_id = p_event_id AND user_id = p_user_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'User not registered for this event';
    END IF;
    
    -- 3. Get product and calculate cost
    SELECT * INTO v_product FROM vending_products WHERE id = p_product_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Product not found';
    END IF;
    
    v_total_amount := v_product.price * p_quantity;
    
    -- 4. Handle payment
    IF p_use_event_budget THEN
        -- Check event budget
        v_budget_spent := (v_event.budget_config->>'budgetSpent')::BIGINT;
        IF (v_event.budget_config->>'totalBudget')::BIGINT - v_budget_spent < v_total_amount THEN
            RAISE EXCEPTION 'Insufficient event budget';
        END IF;
        
        -- Check guest allowances if applicable
        IF v_event.budget_config->>'budgetType' = 'GUEST_ALLOWANCE' THEN
            -- Validate allowances (simplified - expand based on categories)
            IF (v_guest.allowance_used->>'totalDrinks')::INT + p_quantity > (v_guest.allowance_config->>'maxDrinks')::INT THEN
                RAISE EXCEPTION 'Guest allowance exceeded';
            END IF;
        END IF;
        
        -- Update event budget
        UPDATE vending_events 
        SET budget_config = jsonb_set(
            budget_config,
            '{budgetSpent}',
            ((v_budget_spent + v_total_amount)::TEXT)::JSONB
        )
        WHERE id = p_event_id;
        
        -- Update guest consumption
        UPDATE vending_event_guests
        SET total_orders = total_orders + 1,
            total_drinks = total_drinks + p_quantity,
            total_amount = total_amount + v_total_amount,
            allowance_used = jsonb_set(
                allowance_used,
                '{totalDrinks}',
                (((allowance_used->>'totalDrinks')::INT + p_quantity)::TEXT)::JSONB
            )
        WHERE id = v_guest.id;
    ELSE
        -- Debit user wallet
        SELECT * INTO v_wallet FROM wallets WHERE user_id = p_user_id;
        IF NOT FOUND OR v_wallet.balance < v_total_amount THEN
            RAISE EXCEPTION 'Insufficient wallet balance';
        END IF;
        
        -- Create wallet debit ledger entry
        INSERT INTO wallet_ledger (wallet_id, amount, type, description, reference_id)
        VALUES (v_wallet.id, -v_total_amount, 'VENDING_PURCHASE', 
                'Event order: ' || v_event.name, p_event_id);
        
        -- Update wallet balance
        UPDATE wallets SET balance = balance - v_total_amount WHERE id = v_wallet.id;
    END IF;
    
    -- 5. Generate code
    v_code := upper(substring(md5(random()::text || clock_timestamp()::text) from 1 for 4));
    v_expires_at := NOW() + ((v_event.service_config->'sessionSettings'->>'codeExpiryMinutes')::INT || ' minutes')::INTERVAL;
    
    -- 6. Create order
    INSERT INTO vending_orders (
        user_id, machine_id, product_id, quantity, amount, status,
        event_id, service_mode, delivery_location, include_cups, 
        cups_quantity, remaining_serves, paid_by_event_budget
    ) VALUES (
        p_user_id, p_machine_id, p_product_id, p_quantity, v_total_amount, 'PENDING',
        p_event_id, p_service_mode, p_delivery_location, p_include_cups,
        CASE WHEN p_include_cups THEN p_quantity ELSE 0 END,
        p_quantity, p_use_event_budget
    )
    RETURNING id INTO v_order_id;
    
    -- 7. Create code
    INSERT INTO vending_codes (order_id, machine_id, code_hash, expires_at, status)
    VALUES (v_order_id, p_machine_id, v_code, v_expires_at, 'ACTIVE');
    
    -- 8. Return response
    RETURN jsonb_build_object(
        'orderId', v_order_id,
        'code', v_code,
        'expiresAt', v_expires_at,
        'amount', v_total_amount,
        'paidByEventBudget', p_use_event_budget
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 7. FUNCTION: Get Active Event Orders (Machine Queue)
-- =====================================================
CREATE OR REPLACE FUNCTION get_active_event_orders(
    p_event_id UUID,
    p_machine_id UUID DEFAULT NULL,
    p_service_mode VARCHAR(50) DEFAULT NULL
)
RETURNS TABLE (
    order_id UUID,
    order_number VARCHAR(10),
    phone_last_4 VARCHAR(4),
    service_mode VARCHAR(50),
    delivery_location JSONB,
    product_name VARCHAR(255),
    quantity INT,
    include_cups BOOLEAN,
    remaining_serves INT,
    code_hint VARCHAR(10),
    created_at TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        o.id,
        'E' || LPAD((ROW_NUMBER() OVER (ORDER BY o.created_at))::TEXT, 3, '0'),
        eg.phone_last_4,
        o.service_mode,
        o.delivery_location,
        p.name,
        o.quantity,
        o.include_cups,
        o.remaining_serves,
        '**** ' || RIGHT(u.phone, 4),
        o.created_at
    FROM vending_orders o
    JOIN vending_event_guests eg ON eg.event_id = o.event_id AND eg.user_id = o.user_id
    JOIN vending_products p ON p.id = o.product_id
    JOIN auth.users u ON u.id = o.user_id
    WHERE o.event_id = p_event_id
        AND o.status IN ('PENDING', 'ACTIVE')
        AND (p_machine_id IS NULL OR o.machine_id = p_machine_id)
        AND (p_service_mode IS NULL OR o.service_mode = p_service_mode)
        AND o.remaining_serves > 0
    ORDER BY o.created_at ASC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 8. FUNCTION: Serve Event Order (Staff Action)
-- =====================================================
CREATE OR REPLACE FUNCTION serve_event_order(
    p_order_id UUID,
    p_staff_id UUID,
    p_staff_pin VARCHAR(6),
    p_serves_count INT DEFAULT 1
)
RETURNS JSONB AS $$
DECLARE
    v_order vending_orders%ROWTYPE;
    v_staff vending_event_staff%ROWTYPE;
BEGIN
    -- 1. Validate staff
    SELECT * INTO v_staff FROM vending_event_staff WHERE id = p_staff_id AND staff_pin = p_staff_pin AND is_active = true;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Invalid staff credentials';
    END IF;
    
    -- 2. Get order
    SELECT * INTO v_order FROM vending_orders WHERE id = p_order_id AND event_id = v_staff.event_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found';
    END IF;
    
    IF v_order.remaining_serves < p_serves_count THEN
        RAISE EXCEPTION 'Not enough serves remaining';
    END IF;
    
    -- 3. Update order
    UPDATE vending_orders
    SET remaining_serves = remaining_serves - p_serves_count,
        served_by_staff_id = p_staff_id,
        served_at = NOW(),
        status = CASE WHEN remaining_serves - p_serves_count = 0 THEN 'COMPLETED' ELSE status END
    WHERE id = p_order_id;
    
    -- 4. Update staff stats
    UPDATE vending_event_staff
    SET orders_served = orders_served + 1,
        last_served_at = NOW()
    WHERE id = p_staff_id;
    
    RETURN jsonb_build_object(
        'success', true,
        'remainingServes', v_order.remaining_serves - p_serves_count
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 9. FUNCTION: Join Vending Event
-- =====================================================
CREATE OR REPLACE FUNCTION join_vending_event(
    p_user_id UUID,
    p_event_code VARCHAR(10),
    p_age_verified BOOLEAN DEFAULT false
)
RETURNS JSONB AS $$
DECLARE
    v_event vending_events%ROWTYPE;
    v_guest_id UUID;
    v_phone_last_4 VARCHAR(4);
BEGIN
    -- 1. Find event
    SELECT * INTO v_event FROM vending_events WHERE event_code = p_event_code AND status = 'ACTIVE';
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Invalid event code';
    END IF;
    
    -- 2. Get user phone
    SELECT RIGHT(phone, 4) INTO v_phone_last_4 FROM auth.users WHERE id = p_user_id;
    
    -- 3. Insert or update guest
    INSERT INTO vending_event_guests (
        event_id, user_id, phone_last_4, age_verified, age_verified_at
    ) VALUES (
        v_event.id, p_user_id, v_phone_last_4, p_age_verified, 
        CASE WHEN p_age_verified THEN NOW() ELSE NULL END
    )
    ON CONFLICT (event_id, user_id) 
    DO UPDATE SET 
        age_verified = EXCLUDED.age_verified,
        age_verified_at = EXCLUDED.age_verified_at
    RETURNING id INTO v_guest_id;
    
    -- 4. Return event details
    RETURN jsonb_build_object(
        'eventId', v_event.id,
        'eventName', v_event.name,
        'venueName', v_event.venue_name,
        'guestId', v_guest_id,
        'serviceConfig', v_event.service_config,
        'budgetConfig', v_event.budget_config
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 10. TRIGGER: Auto-generate Event Code
-- =====================================================
CREATE OR REPLACE FUNCTION auto_generate_event_code()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.event_code IS NULL OR NEW.event_code = '' THEN
        NEW.event_code := generate_event_code();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_auto_event_code
    BEFORE INSERT ON vending_events
    FOR EACH ROW
    EXECUTE FUNCTION auto_generate_event_code();

-- =====================================================
-- 11. RLS POLICIES
-- =====================================================

-- Events: Hosts can manage their events, anyone can view active events
ALTER TABLE vending_events ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Hosts can manage their events" ON vending_events
    FOR ALL USING (auth.uid() = host_user_id);

CREATE POLICY "Anyone can view active events" ON vending_events
    FOR SELECT USING (status = 'ACTIVE');

-- Event Guests: Users can view their own guest records
ALTER TABLE vending_event_guests ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their guest records" ON vending_event_guests
    FOR SELECT USING (auth.uid() = user_id);

-- Event Staff: Staff can view their own records
ALTER TABLE vending_event_staff ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Staff can view their records" ON vending_event_staff
    FOR SELECT USING (auth.uid() = user_id);

-- =====================================================
-- END OF EVENT MODE SCHEMA
-- =====================================================
