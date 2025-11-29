-- Migration: 001_create_auth_tables.sql
-- Description: Create tables for WhatsApp OTP authentication
-- Created: 2025-01-01
-- Author: MomoTerminal Team

-- ============================================
-- OTP Codes Table
-- Stores OTP codes sent to users via WhatsApp
-- ============================================
CREATE TABLE IF NOT EXISTS otp_codes (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    template_name VARCHAR(50) NOT NULL DEFAULT 'momo_terminal',
    channel VARCHAR(20) NOT NULL DEFAULT 'whatsapp',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    attempts INT DEFAULT 0,
    message_id VARCHAR(100),
    ip_address INET,
    user_agent TEXT,
    
    -- Constraint to limit verification attempts
    CONSTRAINT max_attempts CHECK (attempts <= 5)
);

-- Index for fast lookup by phone and code (only unverified, non-expired)
CREATE INDEX IF NOT EXISTS idx_otp_phone_code ON otp_codes(phone_number, code) 
    WHERE verified_at IS NULL AND expires_at > NOW();

-- Index for rate limiting queries
CREATE INDEX IF NOT EXISTS idx_otp_phone_created ON otp_codes(phone_number, created_at DESC);

-- Index for cleanup of expired codes
CREATE INDEX IF NOT EXISTS idx_otp_expires_at ON otp_codes(expires_at) 
    WHERE verified_at IS NULL;

-- ============================================
-- User Profiles Table (extends Supabase auth.users)
-- ============================================
CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    merchant_name VARCHAR(100),
    business_type VARCHAR(50),
    is_verified BOOLEAN DEFAULT FALSE,
    terms_accepted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for phone number lookups
CREATE INDEX IF NOT EXISTS idx_user_profiles_phone ON user_profiles(phone_number);

-- ============================================
-- Row Level Security Policies
-- ============================================

-- Enable RLS on otp_codes
ALTER TABLE otp_codes ENABLE ROW LEVEL SECURITY;

-- Only service role can insert/update/delete OTP codes
CREATE POLICY "Service role can manage OTP codes" ON otp_codes
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- Enable RLS on user_profiles
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;

-- Users can read their own profile
CREATE POLICY "Users can view own profile" ON user_profiles
    FOR SELECT
    TO authenticated
    USING (auth.uid() = id);

-- Users can update their own profile
CREATE POLICY "Users can update own profile" ON user_profiles
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- Service role can manage all profiles
CREATE POLICY "Service role can manage profiles" ON user_profiles
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- ============================================
-- Helper Functions
-- ============================================

-- Function to generate a random 6-digit OTP
CREATE OR REPLACE FUNCTION generate_otp()
RETURNS VARCHAR(6)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
END;
$$;

-- Function to check rate limiting (max 5 OTPs per hour per phone)
CREATE OR REPLACE FUNCTION check_otp_rate_limit(p_phone VARCHAR)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    otp_count INT;
BEGIN
    SELECT COUNT(*) INTO otp_count
    FROM otp_codes
    WHERE phone_number = p_phone
      AND created_at > NOW() - INTERVAL '1 hour';
    
    RETURN otp_count < 5;
END;
$$;

-- Function to clean up expired OTP codes (run periodically)
CREATE OR REPLACE FUNCTION cleanup_expired_otps()
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    deleted_count INT;
BEGIN
    DELETE FROM otp_codes
    WHERE expires_at < NOW() - INTERVAL '1 day'
       OR (verified_at IS NOT NULL AND verified_at < NOW() - INTERVAL '1 hour');
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$;

-- ============================================
-- Trigger for updating timestamps
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER user_profiles_updated_at
    BEFORE UPDATE ON user_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- ============================================
-- Comments for documentation
-- ============================================
COMMENT ON TABLE otp_codes IS 'Stores OTP codes for WhatsApp authentication';
COMMENT ON COLUMN otp_codes.phone_number IS 'E.164 formatted phone number';
COMMENT ON COLUMN otp_codes.code IS '6-digit OTP code';
COMMENT ON COLUMN otp_codes.template_name IS 'WhatsApp template name used (default: momo_terminal)';
COMMENT ON COLUMN otp_codes.attempts IS 'Number of verification attempts (max 5)';
COMMENT ON COLUMN otp_codes.verified_at IS 'Timestamp when OTP was successfully verified';

COMMENT ON TABLE user_profiles IS 'Extended user profile data for merchants';
COMMENT ON COLUMN user_profiles.phone_number IS 'E.164 formatted phone number';
COMMENT ON COLUMN user_profiles.merchant_name IS 'Business or merchant display name';
