-- =====================================================
-- Phase 1: Settings Helper Functions
-- Database functions for efficient settings management
-- =====================================================

-- Function to get complete merchant settings
CREATE OR REPLACE FUNCTION get_merchant_settings(p_user_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    result JSONB;
BEGIN
    SELECT jsonb_build_object(
        'profile', (
            SELECT jsonb_build_object(
                'id', mp.id,
                'userId', mp.user_id,
                'businessName', mp.business_name,
                'merchantCode', mp.merchant_code,
                'status', mp.status,
                'createdAt', mp.created_at,
                'updatedAt', mp.updated_at
            )
            FROM merchant_profiles mp
            WHERE mp.user_id = p_user_id
        ),
        'businessDetails', (
            SELECT jsonb_build_object(
                'businessType', mbd.business_type,
                'taxId', mbd.tax_id,
                'registrationNumber', mbd.registration_number,
                'location', mbd.location,
                'businessCategory', mbd.business_category,
                'description', mbd.description,
                'website', mbd.website
            )
            FROM merchant_business_details mbd
            JOIN merchant_profiles mp ON mbd.profile_id = mp.id
            WHERE mp.user_id = p_user_id
        ),
        'contactInfo', (
            SELECT jsonb_build_object(
                'email', mci.email,
                'phone', mci.phone,
                'whatsapp', mci.whatsapp,
                'addressLine1', mci.address_line1,
                'addressLine2', mci.address_line2,
                'city', mci.city,
                'state', mci.state,
                'postalCode', mci.postal_code,
                'countryCode', mci.country_code
            )
            FROM merchant_contact_info mci
            JOIN merchant_profiles mp ON mci.profile_id = mp.id
            WHERE mp.user_id = p_user_id
        ),
        'notificationPrefs', (
            SELECT jsonb_build_object(
                'emailEnabled', mnp.email_enabled,
                'smsEnabled', mnp.sms_enabled,
                'pushEnabled', mnp.push_enabled,
                'whatsappEnabled', mnp.whatsapp_enabled,
                'eventsConfig', mnp.events_config,
                'quietHours', mnp.quiet_hours
            )
            FROM merchant_notification_prefs mnp
            JOIN merchant_profiles mp ON mnp.profile_id = mp.id
            WHERE mp.user_id = p_user_id
        ),
        'transactionLimits', (
            SELECT jsonb_build_object(
                'dailyLimit', mtl.daily_limit,
                'singleTransactionLimit', mtl.single_transaction_limit,
                'monthlyLimit', mtl.monthly_limit,
                'minimumAmount', mtl.minimum_amount,
                'maximumAmount', mtl.maximum_amount,
                'currency', mtl.currency,
                'requireApprovalAbove', mtl.require_approval_above
            )
            FROM merchant_transaction_limits mtl
            JOIN merchant_profiles mp ON mtl.profile_id = mp.id
            WHERE mp.user_id = p_user_id
        ),
        'featureFlags', (
            SELECT jsonb_build_object(
                'nfcEnabled', mff.nfc_enabled,
                'offlineMode', mff.offline_mode,
                'autoSync', mff.auto_sync,
                'biometricRequired', mff.biometric_required,
                'receiptsEnabled', mff.receipts_enabled,
                'multiCurrency', mff.multi_currency,
                'advancedAnalytics', mff.advanced_analytics,
                'apiAccess', mff.api_access
            )
            FROM merchant_feature_flags mff
            JOIN merchant_profiles mp ON mff.profile_id = mp.id
            WHERE mp.user_id = p_user_id
        ),
        'paymentProviders', (
            SELECT COALESCE(jsonb_agg(
                jsonb_build_object(
                    'id', mpp.id,
                    'providerName', mpp.provider_name,
                    'isPreferred', mpp.is_preferred,
                    'isEnabled', mpp.is_enabled,
                    'settings', mpp.settings_json
                )
            ), '[]'::jsonb)
            FROM merchant_payment_providers mpp
            JOIN merchant_profiles mp ON mpp.profile_id = mp.id
            WHERE mp.user_id = p_user_id
        )
    ) INTO result;
    
    RETURN result;
END;
$$;

-- Function to update merchant profile
CREATE OR REPLACE FUNCTION update_merchant_profile(
    p_user_id UUID,
    p_business_name VARCHAR DEFAULT NULL,
    p_status VARCHAR DEFAULT NULL
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    UPDATE merchant_profiles
    SET 
        business_name = COALESCE(p_business_name, business_name),
        status = COALESCE(p_status, status)
    WHERE user_id = p_user_id;
    
    RETURN FOUND;
END;
$$;

-- Function to update business details
CREATE OR REPLACE FUNCTION update_business_details(
    p_user_id UUID,
    p_business_type VARCHAR DEFAULT NULL,
    p_tax_id VARCHAR DEFAULT NULL,
    p_registration_number VARCHAR DEFAULT NULL,
    p_location JSONB DEFAULT NULL,
    p_business_category VARCHAR DEFAULT NULL,
    p_description TEXT DEFAULT NULL,
    p_website VARCHAR DEFAULT NULL
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_profile_id UUID;
BEGIN
    -- Get profile_id
    SELECT id INTO v_profile_id FROM merchant_profiles WHERE user_id = p_user_id;
    
    IF v_profile_id IS NULL THEN
        RETURN FALSE;
    END IF;
    
    -- Update or insert business details
    INSERT INTO merchant_business_details (
        profile_id, business_type, tax_id, registration_number,
        location, business_category, description, website
    ) VALUES (
        v_profile_id, p_business_type, p_tax_id, p_registration_number,
        p_location, p_business_category, p_description, p_website
    )
    ON CONFLICT (profile_id) DO UPDATE SET
        business_type = COALESCE(p_business_type, merchant_business_details.business_type),
        tax_id = COALESCE(p_tax_id, merchant_business_details.tax_id),
        registration_number = COALESCE(p_registration_number, merchant_business_details.registration_number),
        location = COALESCE(p_location, merchant_business_details.location),
        business_category = COALESCE(p_business_category, merchant_business_details.business_category),
        description = COALESCE(p_description, merchant_business_details.description),
        website = COALESCE(p_website, merchant_business_details.website);
    
    RETURN TRUE;
END;
$$;

-- Function to update notification preferences
CREATE OR REPLACE FUNCTION update_notification_preferences(
    p_user_id UUID,
    p_email_enabled BOOLEAN DEFAULT NULL,
    p_sms_enabled BOOLEAN DEFAULT NULL,
    p_push_enabled BOOLEAN DEFAULT NULL,
    p_whatsapp_enabled BOOLEAN DEFAULT NULL,
    p_events_config JSONB DEFAULT NULL,
    p_quiet_hours JSONB DEFAULT NULL
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_profile_id UUID;
BEGIN
    SELECT id INTO v_profile_id FROM merchant_profiles WHERE user_id = p_user_id;
    
    IF v_profile_id IS NULL THEN
        RETURN FALSE;
    END IF;
    
    INSERT INTO merchant_notification_prefs (
        profile_id, email_enabled, sms_enabled, push_enabled,
        whatsapp_enabled, events_config, quiet_hours
    ) VALUES (
        v_profile_id, p_email_enabled, p_sms_enabled, p_push_enabled,
        p_whatsapp_enabled, p_events_config, p_quiet_hours
    )
    ON CONFLICT (profile_id) DO UPDATE SET
        email_enabled = COALESCE(p_email_enabled, merchant_notification_prefs.email_enabled),
        sms_enabled = COALESCE(p_sms_enabled, merchant_notification_prefs.sms_enabled),
        push_enabled = COALESCE(p_push_enabled, merchant_notification_prefs.push_enabled),
        whatsapp_enabled = COALESCE(p_whatsapp_enabled, merchant_notification_prefs.whatsapp_enabled),
        events_config = COALESCE(p_events_config, merchant_notification_prefs.events_config),
        quiet_hours = COALESCE(p_quiet_hours, merchant_notification_prefs.quiet_hours);
    
    RETURN TRUE;
END;
$$;

-- Function to update transaction limits
CREATE OR REPLACE FUNCTION update_transaction_limits(
    p_user_id UUID,
    p_daily_limit DECIMAL DEFAULT NULL,
    p_single_transaction_limit DECIMAL DEFAULT NULL,
    p_monthly_limit DECIMAL DEFAULT NULL,
    p_minimum_amount DECIMAL DEFAULT NULL,
    p_maximum_amount DECIMAL DEFAULT NULL,
    p_currency VARCHAR DEFAULT NULL,
    p_require_approval_above DECIMAL DEFAULT NULL
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_profile_id UUID;
BEGIN
    SELECT id INTO v_profile_id FROM merchant_profiles WHERE user_id = p_user_id;
    
    IF v_profile_id IS NULL THEN
        RETURN FALSE;
    END IF;
    
    INSERT INTO merchant_transaction_limits (
        profile_id, daily_limit, single_transaction_limit, monthly_limit,
        minimum_amount, maximum_amount, currency, require_approval_above
    ) VALUES (
        v_profile_id, p_daily_limit, p_single_transaction_limit, p_monthly_limit,
        p_minimum_amount, p_maximum_amount, p_currency, p_require_approval_above
    )
    ON CONFLICT (profile_id) DO UPDATE SET
        daily_limit = COALESCE(p_daily_limit, merchant_transaction_limits.daily_limit),
        single_transaction_limit = COALESCE(p_single_transaction_limit, merchant_transaction_limits.single_transaction_limit),
        monthly_limit = COALESCE(p_monthly_limit, merchant_transaction_limits.monthly_limit),
        minimum_amount = COALESCE(p_minimum_amount, merchant_transaction_limits.minimum_amount),
        maximum_amount = COALESCE(p_maximum_amount, merchant_transaction_limits.maximum_amount),
        currency = COALESCE(p_currency, merchant_transaction_limits.currency),
        require_approval_above = COALESCE(p_require_approval_above, merchant_transaction_limits.require_approval_above);
    
    RETURN TRUE;
END;
$$;

-- Function to update feature flags
CREATE OR REPLACE FUNCTION update_feature_flags(
    p_user_id UUID,
    p_nfc_enabled BOOLEAN DEFAULT NULL,
    p_offline_mode BOOLEAN DEFAULT NULL,
    p_auto_sync BOOLEAN DEFAULT NULL,
    p_biometric_required BOOLEAN DEFAULT NULL,
    p_receipts_enabled BOOLEAN DEFAULT NULL,
    p_multi_currency BOOLEAN DEFAULT NULL,
    p_advanced_analytics BOOLEAN DEFAULT NULL,
    p_api_access BOOLEAN DEFAULT NULL
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_profile_id UUID;
BEGIN
    SELECT id INTO v_profile_id FROM merchant_profiles WHERE user_id = p_user_id;
    
    IF v_profile_id IS NULL THEN
        RETURN FALSE;
    END IF;
    
    INSERT INTO merchant_feature_flags (
        profile_id, nfc_enabled, offline_mode, auto_sync,
        biometric_required, receipts_enabled, multi_currency,
        advanced_analytics, api_access
    ) VALUES (
        v_profile_id, p_nfc_enabled, p_offline_mode, p_auto_sync,
        p_biometric_required, p_receipts_enabled, p_multi_currency,
        p_advanced_analytics, p_api_access
    )
    ON CONFLICT (profile_id) DO UPDATE SET
        nfc_enabled = COALESCE(p_nfc_enabled, merchant_feature_flags.nfc_enabled),
        offline_mode = COALESCE(p_offline_mode, merchant_feature_flags.offline_mode),
        auto_sync = COALESCE(p_auto_sync, merchant_feature_flags.auto_sync),
        biometric_required = COALESCE(p_biometric_required, merchant_feature_flags.biometric_required),
        receipts_enabled = COALESCE(p_receipts_enabled, merchant_feature_flags.receipts_enabled),
        multi_currency = COALESCE(p_multi_currency, merchant_feature_flags.multi_currency),
        advanced_analytics = COALESCE(p_advanced_analytics, merchant_feature_flags.advanced_analytics),
        api_access = COALESCE(p_api_access, merchant_feature_flags.api_access);
    
    RETURN TRUE;
END;
$$;

-- Function to initialize merchant settings for new users
CREATE OR REPLACE FUNCTION initialize_merchant_settings(
    p_user_id UUID,
    p_business_name VARCHAR,
    p_merchant_code VARCHAR
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_profile_id UUID;
BEGIN
    -- Create profile
    INSERT INTO merchant_profiles (user_id, business_name, merchant_code)
    VALUES (p_user_id, p_business_name, p_merchant_code)
    RETURNING id INTO v_profile_id;
    
    -- Initialize with defaults
    INSERT INTO merchant_business_details (profile_id) VALUES (v_profile_id);
    INSERT INTO merchant_contact_info (profile_id) VALUES (v_profile_id);
    INSERT INTO merchant_notification_prefs (profile_id) VALUES (v_profile_id);
    INSERT INTO merchant_transaction_limits (profile_id) VALUES (v_profile_id);
    INSERT INTO merchant_feature_flags (profile_id) VALUES (v_profile_id);
    
    RETURN v_profile_id;
END;
$$;

-- Grant execute permissions
GRANT EXECUTE ON FUNCTION get_merchant_settings(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION update_merchant_profile(UUID, VARCHAR, VARCHAR) TO authenticated;
GRANT EXECUTE ON FUNCTION update_business_details(UUID, VARCHAR, VARCHAR, VARCHAR, JSONB, VARCHAR, TEXT, VARCHAR) TO authenticated;
GRANT EXECUTE ON FUNCTION update_notification_preferences(UUID, BOOLEAN, BOOLEAN, BOOLEAN, BOOLEAN, JSONB, JSONB) TO authenticated;
GRANT EXECUTE ON FUNCTION update_transaction_limits(UUID, DECIMAL, DECIMAL, DECIMAL, DECIMAL, DECIMAL, VARCHAR, DECIMAL) TO authenticated;
GRANT EXECUTE ON FUNCTION update_feature_flags(UUID, BOOLEAN, BOOLEAN, BOOLEAN, BOOLEAN, BOOLEAN, BOOLEAN, BOOLEAN, BOOLEAN) TO authenticated;
GRANT EXECUTE ON FUNCTION initialize_merchant_settings(UUID, VARCHAR, VARCHAR) TO authenticated;
