-- =====================================================
-- Phase 1: Normalize Settings Schema
-- Create normalized tables to replace bloated merchant_settings
-- =====================================================

-- 1. Merchant Profiles (Core Identity)
CREATE TABLE IF NOT EXISTS merchant_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    business_name VARCHAR(255) NOT NULL,
    merchant_code VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'inactive', 'suspended')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id)
);

-- 2. Business Details
CREATE TABLE IF NOT EXISTS merchant_business_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    business_type VARCHAR(50),
    tax_id VARCHAR(100),
    registration_number VARCHAR(100),
    location JSONB, -- {lat, lng, address, city, country}
    business_category VARCHAR(100),
    description TEXT,
    website VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(profile_id)
);

-- 3. Contact Information
CREATE TABLE IF NOT EXISTS merchant_contact_info (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    email VARCHAR(255),
    phone VARCHAR(20),
    whatsapp VARCHAR(20),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country_code VARCHAR(2),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(profile_id)
);

-- 4. Notification Preferences
CREATE TABLE IF NOT EXISTS merchant_notification_prefs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    email_enabled BOOLEAN DEFAULT true,
    sms_enabled BOOLEAN DEFAULT true,
    push_enabled BOOLEAN DEFAULT true,
    whatsapp_enabled BOOLEAN DEFAULT false,
    events_config JSONB DEFAULT '{
        "transaction_success": true,
        "transaction_failed": true,
        "daily_summary": true,
        "weekly_report": false,
        "security_alerts": true,
        "system_updates": false
    }'::jsonb,
    quiet_hours JSONB, -- {start: "22:00", end: "08:00", enabled: true}
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(profile_id)
);

-- 5. Transaction Limits
CREATE TABLE IF NOT EXISTS merchant_transaction_limits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    daily_limit DECIMAL(15,2),
    single_transaction_limit DECIMAL(15,2),
    monthly_limit DECIMAL(15,2),
    minimum_amount DECIMAL(15,2) DEFAULT 100.00,
    maximum_amount DECIMAL(15,2),
    currency VARCHAR(3) DEFAULT 'XAF',
    require_approval_above DECIMAL(15,2),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(profile_id)
);

-- 6. Feature Flags
CREATE TABLE IF NOT EXISTS merchant_feature_flags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    nfc_enabled BOOLEAN DEFAULT false,
    offline_mode BOOLEAN DEFAULT true,
    auto_sync BOOLEAN DEFAULT true,
    biometric_required BOOLEAN DEFAULT false,
    receipts_enabled BOOLEAN DEFAULT true,
    multi_currency BOOLEAN DEFAULT false,
    advanced_analytics BOOLEAN DEFAULT false,
    api_access BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(profile_id)
);

-- 7. Payment Providers (Many-to-Many)
CREATE TABLE IF NOT EXISTS merchant_payment_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    provider_name VARCHAR(50) NOT NULL,
    is_preferred BOOLEAN DEFAULT false,
    is_enabled BOOLEAN DEFAULT true,
    settings_json JSONB DEFAULT '{}'::jsonb,
    api_credentials_encrypted TEXT, -- Encrypted credentials
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(profile_id, provider_name)
);

-- Create indexes for performance
CREATE INDEX idx_merchant_profiles_user_id ON merchant_profiles(user_id);
CREATE INDEX idx_merchant_profiles_merchant_code ON merchant_profiles(merchant_code);
CREATE INDEX idx_merchant_profiles_status ON merchant_profiles(status);

CREATE INDEX idx_business_details_profile_id ON merchant_business_details(profile_id);
CREATE INDEX idx_contact_info_profile_id ON merchant_contact_info(profile_id);
CREATE INDEX idx_notification_prefs_profile_id ON merchant_notification_prefs(profile_id);
CREATE INDEX idx_transaction_limits_profile_id ON merchant_transaction_limits(profile_id);
CREATE INDEX idx_feature_flags_profile_id ON merchant_feature_flags(profile_id);
CREATE INDEX idx_payment_providers_profile_id ON merchant_payment_providers(profile_id);
CREATE INDEX idx_payment_providers_enabled ON merchant_payment_providers(profile_id, is_enabled);

-- Create updated_at triggers
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_merchant_profiles_updated_at BEFORE UPDATE ON merchant_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_merchant_business_details_updated_at BEFORE UPDATE ON merchant_business_details
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_merchant_contact_info_updated_at BEFORE UPDATE ON merchant_contact_info
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_merchant_notification_prefs_updated_at BEFORE UPDATE ON merchant_notification_prefs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_merchant_transaction_limits_updated_at BEFORE UPDATE ON merchant_transaction_limits
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_merchant_feature_flags_updated_at BEFORE UPDATE ON merchant_feature_flags
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_merchant_payment_providers_updated_at BEFORE UPDATE ON merchant_payment_providers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
