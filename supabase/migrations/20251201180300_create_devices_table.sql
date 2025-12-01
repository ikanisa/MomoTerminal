-- Migration: Create devices table
-- Description: Device registration and management
-- Created: 2025-12-01

CREATE TABLE IF NOT EXISTS public.devices (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Device identification
    device_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(100),
    device_model VARCHAR(100),
    manufacturer VARCHAR(100),
    
    -- System info
    os_version VARCHAR(50),
    app_version VARCHAR(20),
    sdk_version INT,
    
    -- Push notifications
    fcm_token TEXT,
    fcm_token_updated_at TIMESTAMPTZ,
    
    -- Security
    last_ip INET,
    last_location POINT,
    is_trusted BOOLEAN DEFAULT FALSE,
    is_blocked BOOLEAN DEFAULT FALSE,
    blocked_reason TEXT,
    blocked_at TIMESTAMPTZ,
    
    -- Activity tracking
    total_transactions INT DEFAULT 0,
    last_sync_at TIMESTAMPTZ,
    last_active_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Metadata
    registered_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Ensure one device per user per device_id
    CONSTRAINT unique_user_device UNIQUE(user_id, device_id)
);

-- Indexes
CREATE INDEX idx_devices_user_id ON public.devices(user_id);
CREATE INDEX idx_devices_device_id ON public.devices(device_id);
CREATE INDEX idx_devices_last_active ON public.devices(last_active_at DESC);
CREATE INDEX idx_devices_fcm_token ON public.devices(fcm_token) WHERE fcm_token IS NOT NULL;
CREATE INDEX idx_devices_blocked ON public.devices(is_blocked) WHERE is_blocked = TRUE;

-- Enable RLS
ALTER TABLE public.devices ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can view own devices" 
    ON public.devices
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can register devices" 
    ON public.devices
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own devices" 
    ON public.devices
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own devices" 
    ON public.devices
    FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Service role full access" 
    ON public.devices
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- Auto-update updated_at trigger
CREATE TRIGGER update_devices_updated_at
    BEFORE UPDATE ON public.devices
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- Comments
COMMENT ON TABLE public.devices IS 'Registered user devices for multi-device support';
COMMENT ON COLUMN public.devices.fcm_token IS 'Firebase Cloud Messaging token for push notifications';
COMMENT ON COLUMN public.devices.is_trusted IS 'User-marked trusted device (skip 2FA)';
COMMENT ON COLUMN public.devices.total_transactions IS 'Total transactions processed on this device';
