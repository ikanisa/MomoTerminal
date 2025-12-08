-- Migration: Create wallet tables
-- Description: User wallet system for vending and payments
-- Created: 2025-12-08

-- =====================================================
-- 1. WALLETS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS wallets (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'RWF',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- One wallet per user
    CONSTRAINT unique_user_wallet UNIQUE(user_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_wallets_user_id ON wallets(user_id);
CREATE INDEX IF NOT EXISTS idx_wallets_status ON wallets(status);

-- =====================================================
-- 2. WALLET LEDGER TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS wallet_ledger (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
    amount DECIMAL(15, 2) NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN (
        'CREDIT', 'DEBIT', 
        'VENDING_PURCHASE', 'VENDING_REFUND',
        'TOP_UP', 'WITHDRAWAL',
        'TRANSFER_IN', 'TRANSFER_OUT'
    )),
    description TEXT,
    reference_id UUID,
    balance_before DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::JSONB
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_wallet_ledger_wallet_id ON wallet_ledger(wallet_id);
CREATE INDEX IF NOT EXISTS idx_wallet_ledger_type ON wallet_ledger(type);
CREATE INDEX IF NOT EXISTS idx_wallet_ledger_created ON wallet_ledger(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_ledger_reference ON wallet_ledger(reference_id);

-- =====================================================
-- 3. ROW LEVEL SECURITY
-- =====================================================
ALTER TABLE wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE wallet_ledger ENABLE ROW LEVEL SECURITY;

-- Users can view their own wallet
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'wallets' 
        AND policyname = 'Users can view own wallet'
    ) THEN
        CREATE POLICY "Users can view own wallet"
            ON wallets FOR SELECT
            TO authenticated
            USING (auth.uid() = user_id);
    END IF;
END $$;

-- Users can view their own ledger entries
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'wallet_ledger' 
        AND policyname = 'Users can view own ledger'
    ) THEN
        CREATE POLICY "Users can view own ledger"
            ON wallet_ledger FOR SELECT
            TO authenticated
            USING (wallet_id IN (SELECT id FROM wallets WHERE user_id = auth.uid()));
    END IF;
END $$;

-- Service role full access
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'wallets' 
        AND policyname = 'Service role full access to wallets'
    ) THEN
        CREATE POLICY "Service role full access to wallets"
            ON wallets FOR ALL
            TO service_role
            USING (true)
            WITH CHECK (true);
    END IF;
END $$;

DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'wallet_ledger' 
        AND policyname = 'Service role full access to ledger'
    ) THEN
        CREATE POLICY "Service role full access to ledger"
            ON wallet_ledger FOR ALL
            TO service_role
            USING (true)
            WITH CHECK (true);
    END IF;
END $$;

-- =====================================================
-- 4. TRIGGERS
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_wallet_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for wallets
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger 
        WHERE tgname = 'update_wallets_updated_at'
    ) THEN
        CREATE TRIGGER update_wallets_updated_at
            BEFORE UPDATE ON wallets
            FOR EACH ROW
            EXECUTE FUNCTION update_wallet_updated_at();
    END IF;
END $$;

-- Function to automatically set balance_before and balance_after in ledger
CREATE OR REPLACE FUNCTION set_wallet_ledger_balances()
RETURNS TRIGGER AS $$
DECLARE
    v_current_balance DECIMAL(15, 2);
BEGIN
    -- Get current wallet balance
    SELECT balance INTO v_current_balance FROM wallets WHERE id = NEW.wallet_id;
    
    -- Set balance_before
    NEW.balance_before = v_current_balance;
    
    -- Calculate balance_after
    NEW.balance_after = v_current_balance + NEW.amount;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for wallet_ledger
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger 
        WHERE tgname = 'set_wallet_ledger_balances_trigger'
    ) THEN
        CREATE TRIGGER set_wallet_ledger_balances_trigger
            BEFORE INSERT ON wallet_ledger
            FOR EACH ROW
            EXECUTE FUNCTION set_wallet_ledger_balances();
    END IF;
END $$;

-- =====================================================
-- 5. HELPER FUNCTIONS
-- =====================================================

-- Function to create wallet for new user
CREATE OR REPLACE FUNCTION create_user_wallet()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO wallets (user_id, balance, currency)
    VALUES (NEW.id, 0.00, 'RWF')
    ON CONFLICT (user_id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to auto-create wallet when user signs up
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger 
        WHERE tgname = 'on_auth_user_created_create_wallet'
    ) THEN
        CREATE TRIGGER on_auth_user_created_create_wallet
            AFTER INSERT ON auth.users
            FOR EACH ROW
            EXECUTE FUNCTION create_user_wallet();
    END IF;
END $$;

-- Function to get wallet balance
CREATE OR REPLACE FUNCTION get_wallet_balance(p_user_id UUID)
RETURNS DECIMAL(15, 2)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_balance DECIMAL(15, 2);
BEGIN
    SELECT balance INTO v_balance
    FROM wallets
    WHERE user_id = p_user_id;
    
    RETURN COALESCE(v_balance, 0.00);
END;
$$;

-- =====================================================
-- 6. COMMENTS
-- =====================================================
COMMENT ON TABLE wallets IS 'User wallet balances for vending and payments';
COMMENT ON TABLE wallet_ledger IS 'Complete transaction history for wallet operations';
COMMENT ON COLUMN wallet_ledger.amount IS 'Positive for credits, negative for debits';
COMMENT ON COLUMN wallet_ledger.balance_before IS 'Wallet balance before this transaction';
COMMENT ON COLUMN wallet_ledger.balance_after IS 'Wallet balance after this transaction';
