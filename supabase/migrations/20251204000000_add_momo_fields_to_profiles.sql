-- Add Mobile Money configuration fields to user_profiles
-- This allows users to store their MoMo number and country separately from their WhatsApp profile

ALTER TABLE user_profiles 
ADD COLUMN IF NOT EXISTS country_code VARCHAR(2) DEFAULT 'RW',
ADD COLUMN IF NOT EXISTS momo_country_code VARCHAR(2),
ADD COLUMN IF NOT EXISTS momo_phone VARCHAR(20),
ADD COLUMN IF NOT EXISTS use_momo_code BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS nfc_terminal_enabled BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS keep_screen_on BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS vibration_enabled BOOLEAN DEFAULT true,
ADD COLUMN IF NOT EXISTS biometric_enabled BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS language VARCHAR(5) DEFAULT 'en';

-- Add index for momo_phone lookups
CREATE INDEX IF NOT EXISTS idx_user_profiles_momo_phone ON user_profiles(momo_phone) WHERE momo_phone IS NOT NULL;

-- Add index for country lookups
CREATE INDEX IF NOT EXISTS idx_user_profiles_country ON user_profiles(country_code);
CREATE INDEX IF NOT EXISTS idx_user_profiles_momo_country ON user_profiles(momo_country_code) WHERE momo_country_code IS NOT NULL;

-- Add comment
COMMENT ON COLUMN user_profiles.country_code IS 'User profile country (from WhatsApp registration)';
COMMENT ON COLUMN user_profiles.momo_country_code IS 'Mobile Money country (can differ from profile country)';
COMMENT ON COLUMN user_profiles.momo_phone IS 'Mobile Money phone number or code';
COMMENT ON COLUMN user_profiles.use_momo_code IS 'Whether using MoMo code instead of phone number';
COMMENT ON COLUMN user_profiles.nfc_terminal_enabled IS 'NFC terminal mode enabled';
COMMENT ON COLUMN user_profiles.language IS 'Preferred app language';
