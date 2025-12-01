-- Migration: Create merchant_settings table
-- Description: Merchant configuration and preferences
-- Created: 2025-12-01

CREATE TABLE IF NOT EXISTS public.merchant_settings (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Business information
    business_name VARCHAR(200),
    business_type VARCHAR(50) CHECK (business_type IN ('retail', 'restaurant', 'service', 'transport', 'agriculture', 'other')),
    merchant_code VARCHAR(50) UNIQUE,
    tax_id VARCHAR(50),
    business_registration_number VARCHAR(50),
    
    -- Contact info
    business_email VARCHAR(255),
    business_phone VARCHAR(20),
    business_address TEXT,
    business_location POINT,
    
    -- Payment provider preferences
    preferred_provider VARCHAR(50) DEFAULT 'MTN',
    enabled_providers JSONB DEFAULT '["MTN", "Vodafone", "AirtelTigo"]'::jsonb,
    provider_specific_settings JSONB DEFAULT '{}'::jsonb,
    
    -- Notification preferences
    email_notifications BOOLEAN DEFAULT TRUE,
    sms_notifications BOOLEAN DEFAULT FALSE,
    push_notifications BOOLEAN DEFAULT TRUE,
    whatsapp_notifications BOOLEAN DEFAULT FALSE,
    
    -- Notification events
    notify_on_transaction BOOLEAN DEFAULT TRUE,
    notify_on_failure BOOLEAN DEFAULT TRUE,
    notify_on_daily_summary BOOLEAN DEFAULT TRUE,
    
    -- Transaction limits and controls
    daily_transaction_limit DECIMAL(15, 2),
    single_transaction_limit DECIMAL(15, 2),
    monthly_transaction_limit DECIMAL(15, 2),
    minimum_transaction_amount DECIMAL(15, 2) DEFAULT 1.00,
    
    -- Feature flags
    nfc_enabled BOOLEAN DEFAULT TRUE,
    auto_sync_enabled BOOLEAN DEFAULT TRUE,
    offline_mode_enabled BOOLEAN DEFAULT TRUE,
    biometric_auth_required BOOLEAN DEFAULT FALSE,
    transaction_receipts_enabled BOOLEAN DEFAULT TRUE,
    
    -- Operating hours (JSON: {"monday": {"start": "08:00", "end": "18:00"}, ...})
    operating_hours JSONB DEFAULT '{}'::jsonb,
    timezone VARCHAR(50) DEFAULT 'Africa/Accra',
    
    -- Compliance
    terms_version_accepted VARCHAR(20),
    privacy_policy_accepted BOOLEAN DEFAULT FALSE,
    gdpr_consent BOOLEAN DEFAULT FALSE,
    data_retention_days INT DEFAULT 365,
    
    -- Metadata
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by UUID REFERENCES auth.users(id)
);

-- Indexes
CREATE INDEX idx_merchant_settings_user_id ON public.merchant_settings(user_id);
CREATE INDEX idx_merchant_settings_merchant_code ON public.merchant_settings(merchant_code);
CREATE INDEX idx_merchant_settings_business_type ON public.merchant_settings(business_type);

-- Enable RLS
ALTER TABLE public.merchant_settings ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can view own settings" 
    ON public.merchant_settings
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own settings" 
    ON public.merchant_settings
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own settings" 
    ON public.merchant_settings
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Service role full access" 
    ON public.merchant_settings
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- Auto-update trigger
CREATE TRIGGER update_merchant_settings_updated_at
    BEFORE UPDATE ON public.merchant_settings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- Comments
COMMENT ON TABLE public.merchant_settings IS 'Merchant configuration and business preferences';
COMMENT ON COLUMN public.merchant_settings.merchant_code IS 'Unique merchant identifier for payment routing';
COMMENT ON COLUMN public.merchant_settings.enabled_providers IS 'JSON array of enabled payment providers';
COMMENT ON COLUMN public.merchant_settings.operating_hours IS 'JSON object defining business hours per day';
