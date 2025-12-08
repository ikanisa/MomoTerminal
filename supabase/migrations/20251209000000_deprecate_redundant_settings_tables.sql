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

-- Mark merchant_settings as deprecated (if it exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'merchant_settings') THEN
        COMMENT ON TABLE merchant_settings IS 
        'DEPRECATED (2025-12-09): Use user_profiles table instead. 
        This table contains redundant fields that overlap with user_profiles.
        Canonical table: user_profiles (see migration 20251204000000_add_momo_fields_to_profiles.sql)
        Kept for data preservation and historical reference only.';
        RAISE NOTICE 'merchant_settings table marked as deprecated';
    ELSE
        RAISE NOTICE 'merchant_settings table does not exist (good - no fragmentation created)';
    END IF;
END $$;

-- Mark merchant_profiles and related normalized tables as deprecated (if they exist)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'merchant_profiles') THEN
        COMMENT ON TABLE merchant_profiles IS 
        'DEPRECATED (2025-12-09): Use user_profiles table instead.
        Over-engineered normalization that duplicates user_profiles purpose.
        Canonical table: user_profiles (see migration 20251204000000_add_momo_fields_to_profiles.sql)
        Kept for data preservation and historical reference only.';
        RAISE NOTICE 'merchant_profiles table marked as deprecated';
    ELSE
        RAISE NOTICE 'merchant_profiles table does not exist (good - no fragmentation created)';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'merchant_business_details') THEN
        COMMENT ON TABLE merchant_business_details IS 
        'DEPRECATED (2025-12-09): Part of deprecated merchant_profiles system. Use user_profiles instead.';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'merchant_contact_info') THEN
        COMMENT ON TABLE merchant_contact_info IS 
        'DEPRECATED (2025-12-09): Part of deprecated merchant_profiles system. Use user_profiles instead.';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'merchant_notification_prefs') THEN
        COMMENT ON TABLE merchant_notification_prefs IS 
        'DEPRECATED (2025-12-09): Part of deprecated merchant_profiles system. Use user_profiles instead.';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'merchant_transaction_limits') THEN
        COMMENT ON TABLE merchant_transaction_limits IS 
        'DEPRECATED (2025-12-09): Part of deprecated merchant_profiles system. Use user_profiles instead.';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'merchant_feature_flags') THEN
        COMMENT ON TABLE merchant_feature_flags IS 
        'DEPRECATED (2025-12-09): Part of deprecated merchant_profiles system. Use user_profiles instead.';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'merchant_payment_providers') THEN
        COMMENT ON TABLE merchant_payment_providers IS 
        'DEPRECATED (2025-12-09): Part of deprecated merchant_profiles system. Use user_profiles instead.';
    END IF;
END $$;

-- Add helpful comment to canonical table
COMMENT ON TABLE user_profiles IS 
'CANONICAL table for user/merchant profiles and settings.
Contains all user identity, preferences, and MoMo configuration.
See migration 20251204000000_add_momo_fields_to_profiles.sql for MoMo fields.
DO NOT create parallel settings tables - extend this table instead.';

-- Create view to document field mappings from deprecated tables (if they exist)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'merchant_settings') 
    OR EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'merchant_profiles') THEN
        
        -- Drop view if it exists
        DROP VIEW IF EXISTS deprecated_table_mapping;
        
        -- Create mapping view
        CREATE VIEW deprecated_table_mapping AS
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
        
        RAISE NOTICE 'Created deprecated_table_mapping view';
    ELSE
        RAISE NOTICE 'No deprecated tables found - mapping view not needed';
    END IF;
END $$;

-- Log deprecation event
DO $$
DECLARE
    deprecated_count INTEGER := 0;
BEGIN
    -- Count how many deprecated tables exist
    SELECT COUNT(*) INTO deprecated_count
    FROM pg_tables 
    WHERE schemaname = 'public' 
    AND tablename IN ('merchant_settings', 'merchant_profiles', 'merchant_business_details', 
                      'merchant_contact_info', 'merchant_notification_prefs', 
                      'merchant_transaction_limits', 'merchant_feature_flags', 
                      'merchant_payment_providers');
    
    RAISE NOTICE '==================================================';
    RAISE NOTICE 'Migration 20251209000000: Database Schema Cleanup';
    RAISE NOTICE '==================================================';
    RAISE NOTICE 'Canonical table: user_profiles (SINGLE SOURCE OF TRUTH)';
    RAISE NOTICE 'Deprecated tables found: %', deprecated_count;
    
    IF deprecated_count > 0 THEN
        RAISE NOTICE 'Tables marked as deprecated and preserved for reference';
    ELSE
        RAISE NOTICE 'EXCELLENT: No fragmentation found - production is clean!';
        RAISE NOTICE 'The redundant tables only existed locally during development';
    END IF;
    
    RAISE NOTICE 'All user/merchant data flows through: user_profiles';
    RAISE NOTICE 'Edge Functions: get-user-profile, update-user-profile';
    RAISE NOTICE '==================================================';
END $$;
