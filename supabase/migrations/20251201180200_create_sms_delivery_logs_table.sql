-- Migration: Create sms_delivery_logs table
-- Description: SMS delivery tracking and analytics
-- Created: 2025-12-01

CREATE TABLE IF NOT EXISTS public.sms_delivery_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    webhook_id UUID REFERENCES webhook_configs(id) ON DELETE SET NULL,
    
    -- SMS details
    phone_number VARCHAR(20) NOT NULL,
    sender VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    
    -- Delivery tracking
    status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'sent', 'failed', 'delivered', 'retrying')),
    response_code INT,
    response_body TEXT,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    
    -- Error handling
    error_message TEXT,
    error_type VARCHAR(50),
    
    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW(),
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ,
    
    -- Local reference
    local_id BIGINT,
    
    -- Performance tracking
    processing_time_ms INT
);

-- Indexes for queries and analytics
CREATE INDEX idx_sms_logs_user_id ON public.sms_delivery_logs(user_id);
CREATE INDEX idx_sms_logs_webhook_id ON public.sms_delivery_logs(webhook_id);
CREATE INDEX idx_sms_logs_status ON public.sms_delivery_logs(status);
CREATE INDEX idx_sms_logs_created ON public.sms_delivery_logs(created_at DESC);
CREATE INDEX idx_sms_logs_phone ON public.sms_delivery_logs(phone_number);
CREATE INDEX idx_sms_logs_retry ON public.sms_delivery_logs(status, next_retry_at) 
    WHERE status = 'retrying';

-- Enable RLS
ALTER TABLE public.sms_delivery_logs ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can view own logs" 
    ON public.sms_delivery_logs
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own logs" 
    ON public.sms_delivery_logs
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Service role full access" 
    ON public.sms_delivery_logs
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- Comments
COMMENT ON TABLE public.sms_delivery_logs IS 'SMS delivery tracking with retry logic';
COMMENT ON COLUMN public.sms_delivery_logs.processing_time_ms IS 'Time taken to deliver SMS in milliseconds';
COMMENT ON COLUMN public.sms_delivery_logs.next_retry_at IS 'Scheduled time for next retry attempt';
