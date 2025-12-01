-- Migration: Create webhook_configs table
-- Description: Cloud-synced webhook configurations
-- Created: 2025-12-01

CREATE TABLE IF NOT EXISTS public.webhook_configs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Webhook details
    name VARCHAR(100) NOT NULL,
    url TEXT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    
    -- Security (encrypted at application level)
    api_key TEXT NOT NULL,
    hmac_secret TEXT NOT NULL,
    
    -- Status tracking
    is_active BOOLEAN DEFAULT TRUE,
    last_success_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    failure_count INT DEFAULT 0,
    
    -- Rate limiting
    max_requests_per_hour INT DEFAULT 1000,
    current_hour_requests INT DEFAULT 0,
    hour_reset_at TIMESTAMPTZ DEFAULT DATE_TRUNC('hour', NOW()) + INTERVAL '1 hour',
    
    -- Metadata
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Local database reference
    local_id BIGINT,
    
    -- Business rules
    CONSTRAINT unique_user_phone UNIQUE(user_id, phone_number)
);

-- Indexes
CREATE INDEX idx_webhook_configs_user_id ON public.webhook_configs(user_id);
CREATE INDEX idx_webhook_configs_active ON public.webhook_configs(is_active);
CREATE INDEX idx_webhook_configs_phone ON public.webhook_configs(phone_number);

-- Enable RLS
ALTER TABLE public.webhook_configs ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can manage own webhooks" 
    ON public.webhook_configs
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Service role full access" 
    ON public.webhook_configs
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- Comments
COMMENT ON TABLE public.webhook_configs IS 'Webhook endpoint configurations for SMS relay';
COMMENT ON COLUMN public.webhook_configs.api_key IS 'Bearer token for webhook authentication (encrypted)';
COMMENT ON COLUMN public.webhook_configs.hmac_secret IS 'Secret for HMAC-SHA256 signature (encrypted)';
COMMENT ON COLUMN public.webhook_configs.failure_count IS 'Consecutive failure count - webhook disabled after 10';
