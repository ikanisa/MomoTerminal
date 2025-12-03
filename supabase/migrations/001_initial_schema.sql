-- MomoTerminal Database Schema
-- Run this in Supabase SQL Editor

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Merchants table
CREATE TABLE IF NOT EXISTS merchants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255),
    country_code VARCHAR(2) NOT NULL DEFAULT 'RW',
    currency VARCHAR(3) NOT NULL DEFAULT 'RWF',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Devices table (for multi-device support)
CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID REFERENCES merchants(id) ON DELETE CASCADE,
    device_uuid VARCHAR(255) NOT NULL UNIQUE,
    device_name VARCHAR(255),
    fcm_token TEXT,
    is_active BOOLEAN DEFAULT true,
    last_seen_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID REFERENCES merchants(id) ON DELETE CASCADE,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    
    -- Transaction details
    amount BIGINT NOT NULL, -- Amount in minor units (e.g., cents)
    currency VARCHAR(3) NOT NULL DEFAULT 'RWF',
    type VARCHAR(20) NOT NULL, -- 'received', 'sent', 'payment', 'withdrawal'
    status VARCHAR(20) NOT NULL DEFAULT 'pending', -- 'pending', 'completed', 'failed'
    
    -- Provider info
    provider VARCHAR(50), -- 'MTN', 'AIRTEL', etc.
    provider_ref VARCHAR(100), -- Transaction ID from provider
    
    -- Parties
    sender_phone VARCHAR(20),
    sender_name VARCHAR(255),
    recipient_phone VARCHAR(20),
    recipient_name VARCHAR(255),
    
    -- SMS source
    sms_sender VARCHAR(50),
    sms_body TEXT,
    sms_timestamp TIMESTAMPTZ,
    
    -- Metadata
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    synced_at TIMESTAMPTZ DEFAULT NOW()
);

-- Webhooks table
CREATE TABLE IF NOT EXISTS webhooks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID REFERENCES merchants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    api_key TEXT,
    hmac_secret TEXT,
    phone_filter VARCHAR(20), -- Optional: only trigger for specific phone
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Webhook delivery logs
CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    webhook_id UUID REFERENCES webhooks(id) ON DELETE CASCADE,
    transaction_id UUID REFERENCES transactions(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL, -- 'pending', 'success', 'failed'
    response_code INT,
    response_body TEXT,
    attempts INT DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_transactions_merchant ON transactions(merchant_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created ON transactions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_devices_merchant ON devices(merchant_id);
CREATE INDEX IF NOT EXISTS idx_webhooks_merchant ON webhooks(merchant_id);

-- Row Level Security (RLS)
ALTER TABLE merchants ENABLE ROW LEVEL SECURITY;
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE webhooks ENABLE ROW LEVEL SECURITY;
ALTER TABLE webhook_deliveries ENABLE ROW LEVEL SECURITY;

-- RLS Policies (users can only access their own data)
CREATE POLICY "Users can view own merchant" ON merchants
    FOR ALL USING (auth.uid()::text = id::text);

CREATE POLICY "Users can view own devices" ON devices
    FOR ALL USING (merchant_id IN (SELECT id FROM merchants WHERE auth.uid()::text = id::text));

CREATE POLICY "Users can view own transactions" ON transactions
    FOR ALL USING (merchant_id IN (SELECT id FROM merchants WHERE auth.uid()::text = id::text));

CREATE POLICY "Users can view own webhooks" ON webhooks
    FOR ALL USING (merchant_id IN (SELECT id FROM merchants WHERE auth.uid()::text = id::text));

CREATE POLICY "Users can view own webhook deliveries" ON webhook_deliveries
    FOR ALL USING (webhook_id IN (SELECT id FROM webhooks WHERE merchant_id IN (SELECT id FROM merchants WHERE auth.uid()::text = id::text)));

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for merchants updated_at
CREATE TRIGGER merchants_updated_at
    BEFORE UPDATE ON merchants
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
