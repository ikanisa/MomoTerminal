-- Migration: Create transactions table
-- Description: Cloud-synced transaction records from devices
-- Created: 2025-12-01

CREATE TABLE IF NOT EXISTS public.transactions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Transaction details
    sender VARCHAR(50) NOT NULL,
    body TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    
    -- Parsed data
    amount DECIMAL(15, 2),
    currency VARCHAR(10) DEFAULT 'GHS',
    transaction_id VARCHAR(100),
    merchant_code VARCHAR(50),
    
    -- Provider information
    provider VARCHAR(50),
    provider_type VARCHAR(20),
    
    -- Metadata
    device_id UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    synced_at TIMESTAMPTZ,
    
    -- Local database reference (for conflict resolution)
    local_id BIGINT,
    
    -- Ensure no duplicate syncs from same device
    CONSTRAINT unique_user_local_transaction UNIQUE(user_id, local_id, device_id)
);

-- Add user_id column if table exists but column doesn't
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'transactions' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE public.transactions ADD COLUMN user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON public.transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON public.transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON public.transactions(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_provider ON public.transactions(provider);
CREATE INDEX IF NOT EXISTS idx_transactions_device ON public.transactions(device_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_timestamp ON public.transactions(user_id, timestamp DESC);

-- Enable Row Level Security
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;

-- RLS Policies
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'transactions' 
        AND policyname = 'Users can view own transactions'
    ) THEN
        CREATE POLICY "Users can view own transactions" 
            ON public.transactions
            FOR SELECT
            TO authenticated
            USING (auth.uid() = user_id);
    END IF;
END $$;

DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'transactions' 
        AND policyname = 'Users can insert own transactions'
    ) THEN
        CREATE POLICY "Users can insert own transactions" 
            ON public.transactions
            FOR INSERT
            TO authenticated
            WITH CHECK (auth.uid() = user_id);
    END IF;
END $$;

DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'transactions' 
        AND policyname = 'Users can update own transactions'
    ) THEN
        CREATE POLICY "Users can update own transactions" 
            ON public.transactions
            FOR UPDATE
            TO authenticated
            USING (auth.uid() = user_id)
            WITH CHECK (auth.uid() = user_id);
    END IF;
END $$;

DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'transactions' 
        AND policyname = 'Service role full access'
    ) THEN
        CREATE POLICY "Service role full access" 
            ON public.transactions
            FOR ALL
            TO service_role
            USING (true)
            WITH CHECK (true);
    END IF;
END $$;

-- Comments
COMMENT ON TABLE public.transactions IS 'Cloud-synced transaction records from mobile devices';
COMMENT ON COLUMN public.transactions.local_id IS 'Reference to local Room database ID for sync tracking';
COMMENT ON COLUMN public.transactions.synced_at IS 'Timestamp when transaction was synced to cloud';
