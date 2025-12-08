-- =====================================================
-- VENDING FEATURE - COMPLETE DATABASE SCHEMA
-- =====================================================
-- Run this in your Supabase SQL Editor

-- =====================================================
-- 1. VENDING PRODUCTS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    size_ml INTEGER NOT NULL DEFAULT 500,
    price BIGINT NOT NULL, -- In cents/minor units (e.g., 5000 = 50.00)
    description TEXT,
    image_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Sample products
INSERT INTO vending_products (name, size_ml, price, description) VALUES
    ('Mango Juice', 500, 50000, 'Fresh mango juice - 500ml'),
    ('Orange Juice', 500, 50000, 'Fresh orange juice - 500ml'),
    ('Pineapple Juice', 500, 50000, 'Fresh pineapple juice - 500ml'),
    ('Mixed Fruit Juice', 500, 55000, 'Mixed fruit juice - 500ml')
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
    status TEXT NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'OFFLINE', 'MAINTENANCE')),
    product_id UUID REFERENCES vending_products(id) NOT NULL,
    stock_level TEXT NOT NULL DEFAULT 'HIGH' CHECK (stock_level IN ('HIGH', 'MEDIUM', 'LOW', 'OUT_OF_STOCK')),
    image_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create spatial index for location queries
CREATE INDEX IF NOT EXISTS idx_vending_machines_location ON vending_machines USING GIST (
    ll_to_earth(latitude, longitude)
);

-- Sample machines (Yaoundé, Cameroon coordinates)
INSERT INTO vending_machines (name, location, latitude, longitude, status, product_id, stock_level) VALUES
    ('Campus Machine 1', 'University of Yaoundé I - Main Building', 3.8480, 11.5021, 'AVAILABLE', (SELECT id FROM vending_products WHERE name = 'Mango Juice' LIMIT 1), 'HIGH'),
    ('Campus Machine 2', 'University of Yaoundé I - Library', 3.8490, 11.5031, 'AVAILABLE', (SELECT id FROM vending_products WHERE name = 'Orange Juice' LIMIT 1), 'MEDIUM'),
    ('Downtown Machine 1', 'Central Market', 3.8667, 11.5167, 'AVAILABLE', (SELECT id FROM vending_products WHERE name = 'Pineapple Juice' LIMIT 1), 'HIGH')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 3. VENDING ORDERS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) NOT NULL,
    machine_id UUID REFERENCES vending_machines(id) NOT NULL,
    machine_name TEXT NOT NULL,
    machine_location TEXT NOT NULL,
    product_name TEXT NOT NULL,
    product_size_ml INTEGER NOT NULL,
    amount BIGINT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CODE_GENERATED', 'DISPENSED', 'EXPIRED', 'REFUNDED', 'FAILED')),
    created_at BIGINT NOT NULL, -- Unix timestamp in milliseconds
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vending_orders_user ON vending_orders(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vending_orders_machine ON vending_orders(machine_id);
CREATE INDEX IF NOT EXISTS idx_vending_orders_status ON vending_orders(status);

-- =====================================================
-- 4. VENDING CODES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID REFERENCES vending_orders(id) ON DELETE CASCADE NOT NULL,
    machine_id UUID REFERENCES vending_machines(id) NOT NULL,
    code_hash TEXT NOT NULL, -- SHA256 hash of the code
    code_plain TEXT, -- Temporary - only stored during initial generation
    expires_at BIGINT NOT NULL, -- Unix timestamp in milliseconds
    used_at BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(code_hash, machine_id)
);

CREATE INDEX IF NOT EXISTS idx_vending_codes_order ON vending_codes(order_id);
CREATE INDEX IF NOT EXISTS idx_vending_codes_expires ON vending_codes(expires_at);
CREATE INDEX IF NOT EXISTS idx_vending_codes_hash ON vending_codes(code_hash);

-- =====================================================
-- 5. VENDING TRANSACTIONS TABLE (Wallet Integration)
-- =====================================================
CREATE TABLE IF NOT EXISTS vending_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID REFERENCES vending_orders(id) NOT NULL,
    user_id UUID REFERENCES auth.users(id) NOT NULL,
    amount BIGINT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('DEBIT', 'REFUND')),
    wallet_balance_before BIGINT NOT NULL,
    wallet_balance_after BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vending_transactions_user ON vending_transactions(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vending_transactions_order ON vending_transactions(order_id);

-- =====================================================
-- 6. HELPER FUNCTION: Generate 4-digit code
-- =====================================================
CREATE OR REPLACE FUNCTION generate_vending_code()
RETURNS TEXT AS $$
DECLARE
    new_code TEXT;
    code_exists BOOLEAN;
BEGIN
    LOOP
        -- Generate random 4-digit code
        new_code := LPAD(FLOOR(RANDOM() * 10000)::TEXT, 4, '0');
        
        -- Check if code already exists and is not expired
        SELECT EXISTS (
            SELECT 1 FROM vending_codes 
            WHERE code_plain = new_code 
            AND expires_at > EXTRACT(EPOCH FROM NOW()) * 1000
        ) INTO code_exists;
        
        EXIT WHEN NOT code_exists;
    END LOOP;
    
    RETURN new_code;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 7. MAIN FUNCTION: Create Vending Order (Atomic Transaction)
-- =====================================================
CREATE OR REPLACE FUNCTION create_vending_order(
    p_user_id UUID,
    p_machine_id UUID,
    p_amount BIGINT
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
    v_new_order_id UUID;
    v_machine RECORD;
    v_product RECORD;
    v_code TEXT;
    v_code_hash TEXT;
    v_code_expires_at BIGINT;
    v_created_at BIGINT;
BEGIN
    -- Get current wallet balance
    SELECT total_tokens INTO v_wallet_balance
    FROM wallets
    WHERE user_id = p_user_id
    FOR UPDATE; -- Lock row for update
    
    -- Check if wallet exists
    IF v_wallet_balance IS NULL THEN
        RAISE EXCEPTION 'Wallet not found for user';
    END IF;
    
    -- Check sufficient balance
    IF v_wallet_balance < p_amount THEN
        RAISE EXCEPTION 'Insufficient balance. Have: %, Need: %', v_wallet_balance, p_amount;
    END IF;
    
    -- Get machine details
    SELECT * INTO v_machine
    FROM vending_machines
    WHERE id = p_machine_id;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Machine not found';
    END IF;
    
    IF v_machine.status != 'AVAILABLE' THEN
        RAISE EXCEPTION 'Machine is not available';
    END IF;
    
    -- Get product details
    SELECT * INTO v_product
    FROM vending_products
    WHERE id = v_machine.product_id;
    
    -- Debit wallet
    UPDATE wallets
    SET total_tokens = total_tokens - p_amount,
        updated_at = NOW()
    WHERE user_id = p_user_id;
    
    -- Generate code and expiry (5 minutes from now)
    v_code := generate_vending_code();
    v_code_hash := encode(digest(v_code, 'sha256'), 'hex');
    v_code_expires_at := (EXTRACT(EPOCH FROM NOW()) * 1000 + 300000)::BIGINT; -- 5 min
    v_created_at := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    
    -- Create order
    INSERT INTO vending_orders (
        user_id, machine_id, machine_name, machine_location,
        product_name, product_size_ml, amount, status, created_at
    ) VALUES (
        p_user_id, p_machine_id, v_machine.name, v_machine.location,
        v_product.name, v_product.size_ml, p_amount, 'CODE_GENERATED', v_created_at
    ) RETURNING id INTO v_new_order_id;
    
    -- Create code
    INSERT INTO vending_codes (order_id, machine_id, code_hash, code_plain, expires_at)
    VALUES (v_new_order_id, p_machine_id, v_code_hash, v_code, v_code_expires_at);
    
    -- Record transaction
    INSERT INTO vending_transactions (
        order_id, user_id, amount, type, wallet_balance_before, wallet_balance_after
    ) VALUES (
        v_new_order_id, p_user_id, p_amount, 'DEBIT', v_wallet_balance, v_wallet_balance - p_amount
    );
    
    -- Return result
    RETURN QUERY SELECT 
        v_new_order_id,
        'CODE_GENERATED'::TEXT,
        v_code,
        v_code_expires_at,
        v_wallet_balance - p_amount;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 8. FUNCTION: Validate & Use Code (For Machine API)
-- =====================================================
CREATE OR REPLACE FUNCTION use_vending_code(
    p_code TEXT,
    p_machine_id UUID
)
RETURNS TABLE(
    success BOOLEAN,
    message TEXT,
    order_id UUID
) AS $$
DECLARE
    v_code_hash TEXT;
    v_code_record RECORD;
    v_now_ms BIGINT;
BEGIN
    v_code_hash := encode(digest(p_code, 'sha256'), 'hex');
    v_now_ms := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    
    -- Find code
    SELECT * INTO v_code_record
    FROM vending_codes
    WHERE code_hash = v_code_hash
    AND machine_id = p_machine_id
    FOR UPDATE;
    
    IF NOT FOUND THEN
        RETURN QUERY SELECT false, 'Invalid code'::TEXT, NULL::UUID;
        RETURN;
    END IF;
    
    -- Check if already used
    IF v_code_record.used_at IS NOT NULL THEN
        RETURN QUERY SELECT false, 'Code already used'::TEXT, v_code_record.order_id;
        RETURN;
    END IF;
    
    -- Check if expired
    IF v_code_record.expires_at < v_now_ms THEN
        RETURN QUERY SELECT false, 'Code expired'::TEXT, v_code_record.order_id;
        RETURN;
    END IF;
    
    -- Mark as used
    UPDATE vending_codes
    SET used_at = v_now_ms,
        code_plain = NULL -- Clear plain code for security
    WHERE id = v_code_record.id;
    
    -- Update order status
    UPDATE vending_orders
    SET status = 'DISPENSED',
        updated_at = NOW()
    WHERE id = v_code_record.order_id;
    
    RETURN QUERY SELECT true, 'Code valid. Dispense drink.'::TEXT, v_code_record.order_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 9. FUNCTION: Process Expired Codes (Run via cron)
-- =====================================================
CREATE OR REPLACE FUNCTION process_expired_vending_codes()
RETURNS TABLE(
    processed_count INTEGER,
    refunded_amount BIGINT
) AS $$
DECLARE
    v_now_ms BIGINT;
    v_count INTEGER := 0;
    v_total_refunded BIGINT := 0;
    v_record RECORD;
BEGIN
    v_now_ms := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    
    -- Find expired unused codes
    FOR v_record IN
        SELECT vc.*, vo.user_id, vo.amount
        FROM vending_codes vc
        JOIN vending_orders vo ON vc.order_id = vo.id
        WHERE vc.expires_at < v_now_ms
        AND vc.used_at IS NULL
        AND vo.status IN ('CODE_GENERATED', 'PENDING')
    LOOP
        -- Refund wallet
        UPDATE wallets
        SET total_tokens = total_tokens + v_record.amount,
            updated_at = NOW()
        WHERE user_id = v_record.user_id;
        
        -- Update order status
        UPDATE vending_orders
        SET status = 'REFUNDED',
            updated_at = NOW()
        WHERE id = v_record.order_id;
        
        -- Record refund transaction
        INSERT INTO vending_transactions (order_id, user_id, amount, type, wallet_balance_before, wallet_balance_after)
        SELECT v_record.order_id, v_record.user_id, v_record.amount, 'REFUND',
               w.total_tokens - v_record.amount, w.total_tokens
        FROM wallets w WHERE w.user_id = v_record.user_id;
        
        v_count := v_count + 1;
        v_total_refunded := v_total_refunded + v_record.amount;
    END LOOP;
    
    RETURN QUERY SELECT v_count, v_total_refunded;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 10. ROW LEVEL SECURITY (RLS)
-- =====================================================
ALTER TABLE vending_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE vending_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE vending_transactions ENABLE ROW LEVEL SECURITY;

-- Users can only see their own orders
CREATE POLICY "Users can view their own orders"
    ON vending_orders FOR SELECT
    USING (auth.uid() = user_id);

-- Users can view their own transactions
CREATE POLICY "Users can view their own transactions"
    ON vending_transactions FOR SELECT
    USING (auth.uid() = user_id);

-- Codes are handled via functions only (no direct access)
CREATE POLICY "No direct code access"
    ON vending_codes FOR ALL
    USING (false);

-- =====================================================
-- SCHEMA COMPLETE ✅
-- =====================================================
