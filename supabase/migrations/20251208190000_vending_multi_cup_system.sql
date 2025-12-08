-- =====================================================
-- VENDING FEATURE - MULTI-CUP SERVING SYSTEM
-- =====================================================
-- Supports: Juice, Coffee, Cocktail, Alcohol, Beer
-- Features: Multi-cup orders, age verification, session-based serving
-- =====================================================

-- =====================================================
-- 1. VENDING PRODUCTS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('JUICE', 'HOT_COFFEE', 'COCKTAIL', 'ALCOHOL', 'BEER')),
    serving_size_ml INTEGER NOT NULL DEFAULT 500,
    price_per_serving BIGINT NOT NULL, -- In cents (e.g., 5000 = 50.00)
    description TEXT,
    image_url TEXT,
    is_age_restricted BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

COMMENT ON COLUMN vending_products.category IS 'Product category: JUICE, HOT_COFFEE, COCKTAIL, ALCOHOL, BEER';
COMMENT ON COLUMN vending_products.serving_size_ml IS 'Default: 500ml (50cl) per cup';
COMMENT ON COLUMN vending_products.is_age_restricted IS 'True for ALCOHOL and BEER categories';

CREATE INDEX IF NOT EXISTS idx_vending_products_category ON vending_products(category);
CREATE INDEX IF NOT EXISTS idx_vending_products_active ON vending_products(is_active);

-- Sample products across all categories
INSERT INTO vending_products (name, category, serving_size_ml, price_per_serving, description, is_age_restricted) VALUES
    ('Mango Juice', 'JUICE', 500, 30000, 'Fresh mango juice - 500ml per cup', false),
    ('Orange Juice', 'JUICE', 500, 30000, 'Fresh orange juice - 500ml per cup', false),
    ('Pineapple Juice', 'JUICE', 500, 30000, 'Fresh pineapple juice - 500ml per cup', false),
    ('Espresso', 'HOT_COFFEE', 500, 50000, 'Hot espresso coffee - 500ml', false),
    ('Cappuccino', 'HOT_COFFEE', 500, 60000, 'Hot cappuccino - 500ml', false),
    ('Mojito Mix', 'COCKTAIL', 500, 100000, 'Pre-mixed mojito cocktail - 500ml', false),
    ('Piña Colada Mix', 'COCKTAIL', 500, 120000, 'Pre-mixed piña colada - 500ml', false),
    ('Red Wine', 'ALCOHOL', 500, 200000, 'Red wine - 500ml per serving', true),
    ('White Wine', 'ALCOHOL', 500, 200000, 'White wine - 500ml per serving', true),
    ('Local Beer', 'BEER', 500, 100000, 'Local beer - 500ml', true),
    ('Premium Beer', 'BEER', 500, 150000, 'Premium imported beer - 500ml', true)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 2. VENDING MACHINES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_machines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    location TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    status TEXT NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'OFFLINE', 'MAINTENANCE', 'LOW_STOCK')),
    product_id UUID REFERENCES vending_products(id) NOT NULL,
    stock_level TEXT NOT NULL DEFAULT 'HIGH' CHECK (stock_level IN ('HIGH', 'MEDIUM', 'LOW', 'OUT_OF_STOCK')),
    image_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

COMMENT ON TABLE vending_machines IS 'Each machine sells ONE product only';
COMMENT ON COLUMN vending_machines.status IS 'Machine operational status';

CREATE INDEX IF NOT EXISTS idx_vending_machines_status ON vending_machines(status);
CREATE INDEX IF NOT EXISTS idx_vending_machines_product ON vending_machines(product_id);

-- Sample machines (Yaoundé, Cameroon coordinates)
INSERT INTO vending_machines (name, location, latitude, longitude, status, product_id, stock_level) VALUES
    ('Juice Station 1', 'University of Yaoundé I - Main Campus', 3.8480, 11.5021, 'AVAILABLE', (SELECT id FROM vending_products WHERE name = 'Mango Juice' LIMIT 1), 'HIGH'),
    ('Coffee Bar 1', 'Downtown Business District', 3.8667, 11.5167, 'AVAILABLE', (SELECT id FROM vending_products WHERE name = 'Cappuccino' LIMIT 1), 'HIGH'),
    ('Cocktail Station', 'Bastos Nightlife Area', 3.8760, 11.5180, 'AVAILABLE', (SELECT id FROM vending_products WHERE name = 'Mojito Mix' LIMIT 1), 'MEDIUM'),
    ('Beer Tap 1', 'Central Market Square', 3.8650, 11.5200, 'AVAILABLE', (SELECT id FROM vending_products WHERE name = 'Local Beer' LIMIT 1), 'HIGH')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 3. USER AGE VERIFICATION TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS user_age_verification (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) NOT NULL UNIQUE,
    is_verified BOOLEAN NOT NULL DEFAULT false,
    date_of_birth DATE,
    verification_method TEXT CHECK (verification_method IN ('ID_CARD', 'PASSPORT', 'MANUAL')),
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by UUID REFERENCES auth.users(id),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

COMMENT ON TABLE user_age_verification IS 'Age verification for alcohol purchases';
COMMENT ON COLUMN user_age_verification.is_verified IS 'True if user is verified 18+ years old';

CREATE INDEX IF NOT EXISTS idx_age_verification_user ON user_age_verification(user_id);
CREATE INDEX IF NOT EXISTS idx_age_verification_status ON user_age_verification(is_verified);

-- =====================================================
-- 4. VENDING ORDERS TABLE (Multi-Cup Support)
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) NOT NULL,
    machine_id UUID REFERENCES vending_machines(id) NOT NULL,
    machine_name TEXT NOT NULL,
    machine_location TEXT NOT NULL,
    product_id UUID REFERENCES vending_products(id) NOT NULL,
    product_name TEXT NOT NULL,
    product_category TEXT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 1 AND quantity <= 10),
    serving_size_ml INTEGER NOT NULL DEFAULT 500,
    price_per_serving BIGINT NOT NULL,
    total_amount BIGINT NOT NULL,
    status TEXT NOT NULL DEFAULT 'CREATED' CHECK (status IN ('CREATED', 'PAID', 'CODE_ISSUED', 'IN_PROGRESS', 'COMPLETED', 'EXPIRED', 'REFUNDED', 'FAILED')),
    created_at BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

COMMENT ON COLUMN vending_orders.quantity IS 'Number of cups (1-10)';
COMMENT ON COLUMN vending_orders.total_amount IS 'quantity × price_per_serving';
COMMENT ON COLUMN vending_orders.status IS 'Order lifecycle status';

CREATE INDEX IF NOT EXISTS idx_vending_orders_user ON vending_orders(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vending_orders_machine ON vending_orders(machine_id);
CREATE INDEX IF NOT EXISTS idx_vending_orders_status ON vending_orders(status);

-- =====================================================
-- 5. VENDING SESSIONS TABLE (Multi-Serve Codes)
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID REFERENCES vending_orders(id) ON DELETE CASCADE NOT NULL UNIQUE,
    machine_id UUID REFERENCES vending_machines(id) NOT NULL,
    code_hash TEXT NOT NULL,
    code_plain TEXT,
    total_serves INTEGER NOT NULL CHECK (total_serves >= 1 AND total_serves <= 10),
    remaining_serves INTEGER NOT NULL CHECK (remaining_serves >= 0),
    expires_at BIGINT NOT NULL,
    used_at BIGINT,
    closed_at BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(code_hash, machine_id)
);

COMMENT ON TABLE vending_sessions IS 'Multi-cup session codes with serve tracking';
COMMENT ON COLUMN vending_sessions.total_serves IS 'Total cups purchased';
COMMENT ON COLUMN vending_sessions.remaining_serves IS 'Cups remaining to dispense';
COMMENT ON COLUMN vending_sessions.used_at IS 'When first cup was dispensed';
COMMENT ON COLUMN vending_sessions.closed_at IS 'When session completed or expired';

CREATE INDEX IF NOT EXISTS idx_vending_sessions_order ON vending_sessions(order_id);
CREATE INDEX IF NOT EXISTS idx_vending_sessions_expires ON vending_sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_vending_sessions_hash ON vending_sessions(code_hash);
CREATE INDEX IF NOT EXISTS idx_vending_sessions_machine ON vending_sessions(machine_id);

-- =====================================================
-- 6. VENDING TRANSACTIONS TABLE (Wallet Integration)
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID REFERENCES vending_orders(id) NOT NULL,
    user_id UUID REFERENCES auth.users(id) NOT NULL,
    amount BIGINT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('DEBIT', 'REFUND', 'PARTIAL_REFUND')),
    wallet_balance_before BIGINT NOT NULL,
    wallet_balance_after BIGINT NOT NULL,
    reference TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

COMMENT ON COLUMN vending_transactions.type IS 'DEBIT: payment, REFUND: full refund, PARTIAL_REFUND: unused cups';

CREATE INDEX IF NOT EXISTS idx_vending_transactions_user ON vending_transactions(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vending_transactions_order ON vending_transactions(order_id);

-- =====================================================
-- 7. HELPER FUNCTION: Generate 4-digit code
-- =====================================================
CREATE OR REPLACE FUNCTION generate_vending_code()
RETURNS TEXT AS $$
DECLARE
    new_code TEXT;
    code_exists BOOLEAN;
    max_attempts INTEGER := 100;
    attempt INTEGER := 0;
BEGIN
    LOOP
        new_code := LPAD(FLOOR(RANDOM() * 10000)::TEXT, 4, '0');
        
        SELECT EXISTS (
            SELECT 1 FROM vending_sessions 
            WHERE code_plain = new_code 
            AND expires_at > EXTRACT(EPOCH FROM NOW()) * 1000
        ) INTO code_exists;
        
        EXIT WHEN NOT code_exists;
        
        attempt := attempt + 1;
        IF attempt > max_attempts THEN
            RAISE EXCEPTION 'Could not generate unique code after % attempts', max_attempts;
        END IF;
    END LOOP;
    
    RETURN new_code;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 8. MAIN FUNCTION: Create Vending Order (Multi-Cup)
-- =====================================================
CREATE OR REPLACE FUNCTION create_vending_order(
    p_user_id UUID,
    p_machine_id UUID,
    p_quantity INTEGER
)
RETURNS TABLE(
    order_id UUID,
    order_status TEXT,
    code TEXT,
    code_expires_at BIGINT,
    total_serves INTEGER,
    remaining_serves INTEGER,
    wallet_balance BIGINT
) AS $$
DECLARE
    v_wallet_balance BIGINT;
    v_new_order_id UUID;
    v_machine RECORD;
    v_product RECORD;
    v_code TEXT;
    v_code_hash TEXT;
    v_code_expires_at BIGINT;
    v_created_at BIGINT;
    v_total_amount BIGINT;
    v_expiry_minutes INTEGER;
BEGIN
    -- Validate quantity
    IF p_quantity < 1 OR p_quantity > 10 THEN
        RAISE EXCEPTION 'Quantity must be between 1 and 10 cups';
    END IF;
    
    -- Get current wallet balance
    SELECT total_tokens INTO v_wallet_balance
    FROM wallets
    WHERE user_id = p_user_id
    FOR UPDATE;
    
    IF v_wallet_balance IS NULL THEN
        RAISE EXCEPTION 'Wallet not found for user';
    END IF;
    
    -- Get machine details
    SELECT * INTO v_machine
    FROM vending_machines
    WHERE id = p_machine_id;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Machine not found';
    END IF;
    
    IF v_machine.status NOT IN ('AVAILABLE', 'LOW_STOCK') THEN
        RAISE EXCEPTION 'Machine is not available (status: %)', v_machine.status;
    END IF;
    
    IF v_machine.stock_level = 'OUT_OF_STOCK' THEN
        RAISE EXCEPTION 'Machine is out of stock';
    END IF;
    
    -- Get product details
    SELECT * INTO v_product
    FROM vending_products
    WHERE id = v_machine.product_id AND is_active = true;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Product not available';
    END IF;
    
    -- Age verification check for restricted products
    IF v_product.is_age_restricted THEN
        IF NOT EXISTS (
            SELECT 1 FROM user_age_verification 
            WHERE user_id = p_user_id AND is_verified = true
        ) THEN
            RAISE EXCEPTION 'Age verification required for this product';
        END IF;
    END IF;
    
    -- Calculate total amount
    v_total_amount := v_product.price_per_serving * p_quantity;
    
    -- Check sufficient balance
    IF v_wallet_balance < v_total_amount THEN
        RAISE EXCEPTION 'Insufficient balance. Have: %, Need: %', v_wallet_balance, v_total_amount;
    END IF;
    
    -- Debit wallet
    UPDATE wallets
    SET total_tokens = total_tokens - v_total_amount,
        updated_at = NOW()
    WHERE user_id = p_user_id;
    
    -- Generate code and calculate expiry
    v_code := generate_vending_code();
    v_code_hash := encode(digest(v_code, 'sha256'), 'hex');
    
    -- Dynamic expiry: 1 cup = 3 min, 2-5 cups = 7 min, 6-10 cups = 12 min
    v_expiry_minutes := CASE
        WHEN p_quantity = 1 THEN 3
        WHEN p_quantity <= 5 THEN 7
        ELSE 12
    END;
    
    v_code_expires_at := (EXTRACT(EPOCH FROM NOW()) * 1000 + (v_expiry_minutes * 60 * 1000))::BIGINT;
    v_created_at := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    
    -- Create order
    INSERT INTO vending_orders (
        user_id, machine_id, machine_name, machine_location,
        product_id, product_name, product_category,
        quantity, serving_size_ml, price_per_serving, total_amount,
        status, created_at
    ) VALUES (
        p_user_id, p_machine_id, v_machine.name, v_machine.location,
        v_product.id, v_product.name, v_product.category,
        p_quantity, v_product.serving_size_ml, v_product.price_per_serving, v_total_amount,
        'CODE_ISSUED', v_created_at
    ) RETURNING id INTO v_new_order_id;
    
    -- Create session
    INSERT INTO vending_sessions (
        order_id, machine_id, code_hash, code_plain,
        total_serves, remaining_serves, expires_at
    ) VALUES (
        v_new_order_id, p_machine_id, v_code_hash, v_code,
        p_quantity, p_quantity, v_code_expires_at
    );
    
    -- Record transaction
    INSERT INTO vending_transactions (
        order_id, user_id, amount, type, 
        wallet_balance_before, wallet_balance_after, reference
    ) VALUES (
        v_new_order_id, p_user_id, v_total_amount, 'DEBIT',
        v_wallet_balance, v_wallet_balance - v_total_amount,
        'Vending order: ' || p_quantity || ' cups'
    );
    
    -- Return result
    RETURN QUERY SELECT 
        v_new_order_id,
        'CODE_ISSUED'::TEXT,
        v_code,
        v_code_expires_at,
        p_quantity,
        p_quantity,
        v_wallet_balance - v_total_amount;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 9. FUNCTION: Validate Session Code (Machine API)
-- =====================================================
CREATE OR REPLACE FUNCTION validate_vending_session(
    p_code TEXT,
    p_machine_id UUID
)
RETURNS TABLE(
    valid BOOLEAN,
    message TEXT,
    order_id UUID,
    remaining_serves INTEGER,
    expires_at BIGINT
) AS $$
DECLARE
    v_code_hash TEXT;
    v_session RECORD;
    v_now_ms BIGINT;
BEGIN
    v_code_hash := encode(digest(p_code, 'sha256'), 'hex');
    v_now_ms := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    
    -- Find session
    SELECT * INTO v_session
    FROM vending_sessions
    WHERE code_hash = v_code_hash
    AND machine_id = p_machine_id;
    
    IF NOT FOUND THEN
        RETURN QUERY SELECT false, 'Invalid code'::TEXT, NULL::UUID, 0, 0::BIGINT;
        RETURN;
    END IF;
    
    -- Check if expired
    IF v_session.expires_at < v_now_ms THEN
        RETURN QUERY SELECT false, 'Code expired'::TEXT, v_session.order_id, v_session.remaining_serves, v_session.expires_at;
        RETURN;
    END IF;
    
    -- Check if completed
    IF v_session.remaining_serves <= 0 THEN
        RETURN QUERY SELECT false, 'All cups dispensed'::TEXT, v_session.order_id, 0, v_session.expires_at;
        RETURN;
    END IF;
    
    -- Valid
    RETURN QUERY SELECT true, 'Valid session'::TEXT, v_session.order_id, v_session.remaining_serves, v_session.expires_at;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 10. FUNCTION: Consume Serving (Machine API)
-- =====================================================
CREATE OR REPLACE FUNCTION consume_vending_serve(
    p_code TEXT,
    p_machine_id UUID,
    p_servings INTEGER DEFAULT 1
)
RETURNS TABLE(
    success BOOLEAN,
    message TEXT,
    order_id UUID,
    remaining_serves INTEGER,
    session_status TEXT
) AS $$
DECLARE
    v_code_hash TEXT;
    v_session RECORD;
    v_now_ms BIGINT;
    v_new_remaining INTEGER;
BEGIN
    v_code_hash := encode(digest(p_code, 'sha256'), 'hex');
    v_now_ms := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    
    -- Find and lock session
    SELECT * INTO v_session
    FROM vending_sessions
    WHERE code_hash = v_code_hash
    AND machine_id = p_machine_id
    FOR UPDATE;
    
    IF NOT FOUND THEN
        RETURN QUERY SELECT false, 'Invalid code'::TEXT, NULL::UUID, 0, 'INVALID'::TEXT;
        RETURN;
    END IF;
    
    -- Check expired
    IF v_session.expires_at < v_now_ms THEN
        RETURN QUERY SELECT false, 'Code expired'::TEXT, v_session.order_id, v_session.remaining_serves, 'EXPIRED'::TEXT;
        RETURN;
    END IF;
    
    -- Check remaining
    IF v_session.remaining_serves < p_servings THEN
        RETURN QUERY SELECT false, 'Not enough serves remaining'::TEXT, v_session.order_id, v_session.remaining_serves, 'INSUFFICIENT'::TEXT;
        RETURN;
    END IF;
    
    -- Consume servings
    v_new_remaining := v_session.remaining_serves - p_servings;
    
    UPDATE vending_sessions
    SET remaining_serves = v_new_remaining,
        used_at = COALESCE(used_at, v_now_ms),
        closed_at = CASE WHEN v_new_remaining = 0 THEN v_now_ms ELSE closed_at END,
        code_plain = NULL,
        updated_at = NOW()
    WHERE id = v_session.id;
    
    -- Update order status
    UPDATE vending_orders
    SET status = CASE
            WHEN v_new_remaining = 0 THEN 'COMPLETED'
            WHEN v_session.used_at IS NULL THEN 'IN_PROGRESS'
            ELSE status
        END,
        updated_at = NOW()
    WHERE id = v_session.order_id;
    
    -- Return result
    RETURN QUERY SELECT 
        true,
        format('Dispensed %s cup(s). %s remaining.', p_servings, v_new_remaining),
        v_session.order_id,
        v_new_remaining,
        CASE WHEN v_new_remaining = 0 THEN 'COMPLETED' ELSE 'ACTIVE' END;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 11. FUNCTION: Process Expired Sessions (Cron Job)
-- =====================================================
CREATE OR REPLACE FUNCTION process_expired_vending_sessions()
RETURNS TABLE(
    processed_count INTEGER,
    fully_refunded INTEGER,
    partially_refunded INTEGER,
    total_refunded_amount BIGINT
) AS $$
DECLARE
    v_now_ms BIGINT;
    v_count INTEGER := 0;
    v_full_refund_count INTEGER := 0;
    v_partial_refund_count INTEGER := 0;
    v_total_refunded BIGINT := 0;
    v_record RECORD;
    v_refund_amount BIGINT;
BEGIN
    v_now_ms := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    
    -- Find expired sessions with remaining serves
    FOR v_record IN
        SELECT vs.*, vo.user_id, vo.price_per_serving
        FROM vending_sessions vs
        JOIN vending_orders vo ON vs.order_id = vo.id
        WHERE vs.expires_at < v_now_ms
        AND vs.remaining_serves > 0
        AND vs.closed_at IS NULL
        AND vo.status IN ('CODE_ISSUED', 'IN_PROGRESS')
        FOR UPDATE OF vs
    LOOP
        -- Calculate refund amount for unused cups
        v_refund_amount := v_record.price_per_serving * v_record.remaining_serves;
        
        -- Refund wallet
        UPDATE wallets
        SET total_tokens = total_tokens + v_refund_amount,
            updated_at = NOW()
        WHERE user_id = v_record.user_id;
        
        -- Close session
        UPDATE vending_sessions
        SET closed_at = v_now_ms,
            code_plain = NULL,
            updated_at = NOW()
        WHERE id = v_record.id;
        
        -- Update order status
        UPDATE vending_orders
        SET status = CASE 
                WHEN v_record.remaining_serves = v_record.total_serves THEN 'REFUNDED'
                ELSE 'COMPLETED'
            END,
            updated_at = NOW()
        WHERE id = v_record.order_id;
        
        -- Record refund transaction
        INSERT INTO vending_transactions (order_id, user_id, amount, type, wallet_balance_before, wallet_balance_after, reference)
        SELECT v_record.order_id, v_record.user_id, v_refund_amount,
               CASE WHEN v_record.remaining_serves = v_record.total_serves THEN 'REFUND' ELSE 'PARTIAL_REFUND' END,
               w.total_tokens - v_refund_amount, w.total_tokens,
               format('Expired: %s of %s cups unused', v_record.remaining_serves, v_record.total_serves)
        FROM wallets w WHERE w.user_id = v_record.user_id;
        
        -- Count refunds
        IF v_record.remaining_serves = v_record.total_serves THEN
            v_full_refund_count := v_full_refund_count + 1;
        ELSE
            v_partial_refund_count := v_partial_refund_count + 1;
        END IF;
        
        v_count := v_count + 1;
        v_total_refunded := v_total_refunded + v_refund_amount;
    END LOOP;
    
    RETURN QUERY SELECT v_count, v_full_refund_count, v_partial_refund_count, v_total_refunded;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 12. ROW LEVEL SECURITY (RLS)
-- =====================================================
ALTER TABLE vending_products ENABLE ROW LEVEL SECURITY;
ALTER TABLE vending_machines ENABLE ROW LEVEL SECURITY;
ALTER TABLE vending_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE vending_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vending_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_age_verification ENABLE ROW LEVEL SECURITY;

-- Everyone can view products and machines
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'vending_products' 
        AND policyname = 'Anyone can view active products'
    ) THEN
        CREATE POLICY "Anyone can view active products"
            ON vending_products FOR SELECT
            USING (is_active = true);
    END IF;
END $$;

DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'vending_machines' 
        AND policyname = 'Anyone can view available machines'
    ) THEN
        CREATE POLICY "Anyone can view available machines"
            ON vending_machines FOR SELECT
            USING (true);
    END IF;
END $$;

-- Users can only see their own orders
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'vending_orders' 
        AND policyname = 'Users can view their own orders'
    ) THEN
        CREATE POLICY "Users can view their own orders"
            ON vending_orders FOR SELECT
            USING (auth.uid() = user_id);
    END IF;
END $$;

-- Users can view their own transactions
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'vending_transactions' 
        AND policyname = 'Users can view their own transactions'
    ) THEN
        CREATE POLICY "Users can view their own transactions"
            ON vending_transactions FOR SELECT
            USING (auth.uid() = user_id);
    END IF;
END $$;

-- Users can view their own age verification
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'user_age_verification' 
        AND policyname = 'Users can view their own age verification'
    ) THEN
        CREATE POLICY "Users can view their own age verification"
            ON user_age_verification FOR SELECT
            USING (auth.uid() = user_id);
    END IF;
END $$;

-- Sessions are handled via functions only
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'vending_sessions' 
        AND policyname = 'No direct session access'
    ) THEN
        CREATE POLICY "No direct session access"
            ON vending_sessions FOR ALL
            USING (false);
    END IF;
END $$;

-- =====================================================
-- 13. TRIGGERS FOR UPDATED_AT
-- =====================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_vending_products_updated_at BEFORE UPDATE ON vending_products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_vending_machines_updated_at BEFORE UPDATE ON vending_machines
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_age_verification_updated_at BEFORE UPDATE ON user_age_verification
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- SCHEMA COMPLETE ✅
-- =====================================================
-- Features:
-- ✅ Multi-cup orders (1-10 cups per order)
-- ✅ 5 product categories with age restrictions
-- ✅ Session-based serving with remaining count
-- ✅ Dynamic expiry based on quantity
-- ✅ Partial refunds for unused cups
-- ✅ Age verification for alcohol/beer
-- ✅ Atomic wallet transactions
-- ✅ Machine status and stock tracking
-- =====================================================
