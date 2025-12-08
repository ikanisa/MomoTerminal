-- =====================================================
-- Deprecate Redundant Settings Tables
-- =====================================================
-- Purpose: Mark merchant_settings and merchant_profiles as deprecated
--          These tables duplicate functionality of user_profiles
--          which is the canonical source of truth
-- 
-- Context: user_profiles (created 20251130000141) was extended with
--          MoMo fields (20251204000000) and already contains all
--          necessary user/merchant configuration fields
-- 
-- Action: Tables are marked as deprecated but NOT dropped
--         to preserve any existing data for historical reference
-- =====================================================

-- Mark merchant_settings as deprecated
COMMENT ON TABLE merchant_settings IS 
'DEPRECATED (2025-12-09): Use user_profiles table instead. 
This table contains redundant fields that overlap with user_profiles.
Canonical table: user_profiles (see migration 20251204000000_add_momo_fields_to_profiles.sql)
Kept for data preservation and historical reference only.';

-- Mark merchant_profiles and related normalized tables as deprecated
COMMENT ON TABLE merchant_profiles IS 
'DEPRECATED (2025-12-09): Use user_profiles table instead.
Over-engineered normalization that duplicates user_profiles purpose.
Canonical table: user_profiles (see migration 20251204000000_add_momo_fields_to_profiles.sql)
Kept for data preservation and historical reference only.';

COMMENT ON TABLE merchant_business_details IS 
'DEPRECATED (2025-12-09): Part of deprecated merchant_profiles system. Use user_profiles instead.';

COMMENT ON TABLE merchant_contact_info IS 
'DEPRECATED (2025-12-09): Part of deprecated merchant_profiles system. Use user_profiles instead.';

COMMENT ON TABLE merchant_notification_prefs IS 
'DEPRECATED (2025-12-09): Part of deprecated merchant_profiles system. Use user_profiles instead.';

COMMENT ON TABLE merchant_transaction_limits IS 
'DEPRECATED (2025-12-09): Part of deprecated merchant_profiles system. Use user_profiles instead.';

COMMENT ON TABLE merchant_feature_flags IS 
'DEPRECATED (2025-12-09): Part of deprecated merchant_profiles system. Use user_profiles instead.';

COMMENT ON TABLE merchant_payment_providers IS 
'DEPRECATED (2025-12-09): Part of deprecated merchant_profiles system. Use user_profiles instead.';

-- Add helpful comment to canonical table
COMMENT ON TABLE user_profiles IS 
'CANONICAL table for user/merchant profiles and settings.
Contains all user identity, preferences, and MoMo configuration.
See migration 20251204000000_add_momo_fields_to_profiles.sql for MoMo fields.
DO NOT create parallel settings tables - extend this table instead.';

-- Create view to document field mappings from deprecated tables
CREATE OR REPLACE VIEW deprecated_table_mapping AS
SELECT 
    'merchant_settings' as deprecated_table,
    'business_name' as deprecated_field,
    'user_profiles' as canonical_table,
    'merchant_name' as canonical_field
UNION ALL SELECT 'merchant_settings', 'nfc_enabled', 'user_profiles', 'nfc_terminal_enabled'
UNION ALL SELECT 'merchant_settings', 'biometric_auth_required', 'user_profiles', 'biometric_enabled'
UNION ALL SELECT 'merchant_profiles', 'business_name', 'user_profiles', 'merchant_name'
UNION ALL SELECT 'merchant_feature_flags', 'nfc_enabled', 'user_profiles', 'nfc_terminal_enabled'
UNION ALL SELECT 'merchant_feature_flags', 'biometric_required', 'user_profiles', 'biometric_enabled';

COMMENT ON VIEW deprecated_table_mapping IS 
'Documents field mappings from deprecated tables to canonical user_profiles table.
Use this view to understand which user_profiles fields replace deprecated table fields.';

-- Log deprecation event
DO $$
BEGIN
    RAISE NOTICE 'Migration 20251209000000: Deprecated redundant settings tables';
    RAISE NOTICE 'Canonical table: user_profiles';
    RAISE NOTICE 'Deprecated tables: merchant_settings, merchant_profiles, and 6 related tables';
    RAISE NOTICE 'Data preserved for historical reference - tables not dropped';
END $$;
