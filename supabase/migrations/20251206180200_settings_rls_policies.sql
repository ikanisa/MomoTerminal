-- =====================================================
-- Phase 1: Row Level Security Policies
-- Secure access to settings tables
-- =====================================================

-- Enable RLS on all tables
ALTER TABLE merchant_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchant_business_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchant_contact_info ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchant_notification_prefs ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchant_transaction_limits ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchant_feature_flags ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchant_payment_providers ENABLE ROW LEVEL SECURITY;

-- Merchant Profiles Policies
CREATE POLICY "Users can view their own profile"
    ON merchant_profiles FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can update their own profile"
    ON merchant_profiles FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own profile"
    ON merchant_profiles FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Business Details Policies
CREATE POLICY "Users can view their own business details"
    ON merchant_business_details FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_business_details.profile_id
            AND mp.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can update their own business details"
    ON merchant_business_details FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_business_details.profile_id
            AND mp.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert their own business details"
    ON merchant_business_details FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_business_details.profile_id
            AND mp.user_id = auth.uid()
        )
    );

-- Contact Info Policies
CREATE POLICY "Users can view their own contact info"
    ON merchant_contact_info FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_contact_info.profile_id
            AND mp.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can update their own contact info"
    ON merchant_contact_info FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_contact_info.profile_id
            AND mp.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert their own contact info"
    ON merchant_contact_info FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_contact_info.profile_id
            AND mp.user_id = auth.uid()
        )
    );

-- Notification Preferences Policies
CREATE POLICY "Users can view their own notification prefs"
    ON merchant_notification_prefs FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_notification_prefs.profile_id
            AND mp.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can update their own notification prefs"
    ON merchant_notification_prefs FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_notification_prefs.profile_id
            AND mp.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert their own notification prefs"
    ON merchant_notification_prefs FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_notification_prefs.profile_id
            AND mp.user_id = auth.uid()
        )
    );

-- Transaction Limits Policies
CREATE POLICY "Users can view their own transaction limits"
    ON merchant_transaction_limits FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_transaction_limits.profile_id
            AND mp.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can update their own transaction limits"
    ON merchant_transaction_limits FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_transaction_limits.profile_id
            AND mp.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert their own transaction limits"
    ON merchant_transaction_limits FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_transaction_limits.profile_id
            AND mp.user_id = auth.uid()
        )
    );

-- Feature Flags Policies
CREATE POLICY "Users can view their own feature flags"
    ON merchant_feature_flags FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_feature_flags.profile_id
            AND mp.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can update their own feature flags"
    ON merchant_feature_flags FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_feature_flags.profile_id
            AND mp.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert their own feature flags"
    ON merchant_feature_flags FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_feature_flags.profile_id
            AND mp.user_id = auth.uid()
        )
    );

-- Payment Providers Policies
CREATE POLICY "Users can view their own payment providers"
    ON merchant_payment_providers FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_payment_providers.profile_id
            AND mp.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can update their own payment providers"
    ON merchant_payment_providers FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_payment_providers.profile_id
            AND mp.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert their own payment providers"
    ON merchant_payment_providers FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_payment_providers.profile_id
            AND mp.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can delete their own payment providers"
    ON merchant_payment_providers FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM merchant_profiles mp
            WHERE mp.id = merchant_payment_providers.profile_id
            AND mp.user_id = auth.uid()
        )
    );
