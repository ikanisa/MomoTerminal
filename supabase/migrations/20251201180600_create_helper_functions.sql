-- Migration: Create helper functions
-- Description: Database helper functions for common operations
-- Created: 2025-12-01

-- ============================================
-- Transaction Statistics Function
-- ============================================
CREATE OR REPLACE FUNCTION get_transaction_stats(p_user_id UUID)
RETURNS TABLE (
    total_count BIGINT,
    total_amount NUMERIC,
    pending_count BIGINT,
    sent_count BIGINT,
    failed_count BIGINT,
    today_count BIGINT,
    today_amount NUMERIC,
    this_week_count BIGINT,
    this_month_count BIGINT,
    average_amount NUMERIC,
    highest_amount NUMERIC,
    providers_used BIGINT
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(*)::BIGINT AS total_count,
        COALESCE(SUM(amount), 0)::NUMERIC AS total_amount,
        COUNT(*) FILTER (WHERE status = 'PENDING')::BIGINT AS pending_count,
        COUNT(*) FILTER (WHERE status = 'SENT')::BIGINT AS sent_count,
        COUNT(*) FILTER (WHERE status = 'FAILED')::BIGINT AS failed_count,
        COUNT(*) FILTER (WHERE DATE(timestamp) = CURRENT_DATE)::BIGINT AS today_count,
        COALESCE(SUM(amount) FILTER (WHERE DATE(timestamp) = CURRENT_DATE), 0)::NUMERIC AS today_amount,
        COUNT(*) FILTER (WHERE timestamp >= DATE_TRUNC('week', NOW()))::BIGINT AS this_week_count,
        COUNT(*) FILTER (WHERE timestamp >= DATE_TRUNC('month', NOW()))::BIGINT AS this_month_count,
        COALESCE(AVG(amount), 0)::NUMERIC AS average_amount,
        COALESCE(MAX(amount), 0)::NUMERIC AS highest_amount,
        COUNT(DISTINCT provider)::BIGINT AS providers_used
    FROM public.transactions
    WHERE user_id = p_user_id;
END;
$$;

COMMENT ON FUNCTION get_transaction_stats IS 'Get comprehensive transaction statistics for a user';

-- ============================================
-- Device Activity Update
-- ============================================
CREATE OR REPLACE FUNCTION update_device_activity(
    p_device_id VARCHAR,
    p_user_id UUID,
    p_transaction_count INT DEFAULT 0
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    UPDATE public.devices
    SET 
        last_active_at = NOW(),
        last_sync_at = NOW(),
        total_transactions = total_transactions + p_transaction_count
    WHERE device_id = p_device_id AND user_id = p_user_id;
END;
$$;

COMMENT ON FUNCTION update_device_activity IS 'Update device last active timestamp and transaction count';

-- ============================================
-- Webhook Health Check
-- ============================================
CREATE OR REPLACE FUNCTION check_webhook_health(p_webhook_id UUID)
RETURNS TABLE (
    is_healthy BOOLEAN,
    success_rate NUMERIC,
    total_deliveries BIGINT,
    failed_deliveries BIGINT,
    last_success TIMESTAMPTZ,
    last_failure TIMESTAMPTZ
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    SELECT
        (COUNT(*) FILTER (WHERE status = 'sent')::NUMERIC / NULLIF(COUNT(*), 0) > 0.9) AS is_healthy,
        (COUNT(*) FILTER (WHERE status = 'sent')::NUMERIC / NULLIF(COUNT(*), 0) * 100) AS success_rate,
        COUNT(*)::BIGINT AS total_deliveries,
        COUNT(*) FILTER (WHERE status = 'failed')::BIGINT AS failed_deliveries,
        MAX(sent_at) FILTER (WHERE status = 'sent') AS last_success,
        MAX(created_at) FILTER (WHERE status = 'failed') AS last_failure
    FROM public.sms_delivery_logs
    WHERE webhook_id = p_webhook_id
      AND created_at > NOW() - INTERVAL '24 hours';
END;
$$;

COMMENT ON FUNCTION check_webhook_health IS 'Check webhook health based on recent delivery success rate';

-- ============================================
-- Auto-disable Unhealthy Webhooks
-- ============================================
CREATE OR REPLACE FUNCTION auto_disable_unhealthy_webhooks()
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    disabled_count INT;
BEGIN
    WITH unhealthy_webhooks AS (
        SELECT DISTINCT webhook_id
        FROM public.sms_delivery_logs
        WHERE created_at > NOW() - INTERVAL '1 hour'
        GROUP BY webhook_id
        HAVING COUNT(*) FILTER (WHERE status = 'failed') >= 10
           AND COUNT(*) FILTER (WHERE status = 'sent') = 0
    )
    UPDATE public.webhook_configs
    SET 
        is_active = FALSE,
        updated_at = NOW()
    WHERE id IN (SELECT webhook_id FROM unhealthy_webhooks)
      AND is_active = TRUE
    RETURNING 1 INTO disabled_count;
    
    RETURN COALESCE(disabled_count, 0);
END;
$$;

COMMENT ON FUNCTION auto_disable_unhealthy_webhooks IS 'Automatically disable webhooks with consistent failures';

-- ============================================
-- Cleanup Old Data
-- ============================================
CREATE OR REPLACE FUNCTION cleanup_old_data()
RETURNS TABLE (
    expired_otps_deleted INT,
    old_analytics_deleted INT,
    old_errors_deleted INT,
    old_logs_deleted INT
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    otp_count INT;
    analytics_count INT;
    error_count INT;
    log_count INT;
BEGIN
    -- Clean up expired OTPs (older than 1 day)
    DELETE FROM otp_codes
    WHERE expires_at < NOW() - INTERVAL '1 day'
       OR (verified_at IS NOT NULL AND verified_at < NOW() - INTERVAL '1 hour');
    GET DIAGNOSTICS otp_count = ROW_COUNT;
    
    -- Clean up old analytics events (older than 90 days)
    DELETE FROM public.analytics_events
    WHERE created_at < NOW() - INTERVAL '90 days';
    GET DIAGNOSTICS analytics_count = ROW_COUNT;
    
    -- Clean up resolved error logs (older than 30 days)
    DELETE FROM public.error_logs
    WHERE is_resolved = TRUE 
      AND resolved_at < NOW() - INTERVAL '30 days';
    GET DIAGNOSTICS error_count = ROW_COUNT;
    
    -- Clean up old SMS delivery logs (older than 60 days, only successful ones)
    DELETE FROM public.sms_delivery_logs
    WHERE status = 'delivered'
      AND created_at < NOW() - INTERVAL '60 days';
    GET DIAGNOSTICS log_count = ROW_COUNT;
    
    RETURN QUERY SELECT otp_count, analytics_count, error_count, log_count;
END;
$$;

COMMENT ON FUNCTION cleanup_old_data IS 'Clean up expired and old data across tables';

-- ============================================
-- Refresh Analytics Materialized View
-- ============================================
CREATE OR REPLACE FUNCTION refresh_analytics_views()
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY public.daily_transaction_summary;
END;
$$;

COMMENT ON FUNCTION refresh_analytics_views IS 'Refresh all analytics materialized views';

-- ============================================
-- Get Recent Transactions with Pagination
-- ============================================
CREATE OR REPLACE FUNCTION get_recent_transactions(
    p_user_id UUID,
    p_limit INT DEFAULT 20,
    p_offset INT DEFAULT 0,
    p_provider VARCHAR DEFAULT NULL,
    p_status VARCHAR DEFAULT NULL
)
RETURNS TABLE (
    id UUID,
    sender VARCHAR,
    amount NUMERIC,
    currency VARCHAR,
    status VARCHAR,
    provider VARCHAR,
    timestamp TIMESTAMPTZ,
    transaction_id VARCHAR
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    SELECT
        t.id,
        t.sender,
        t.amount,
        t.currency,
        t.status,
        t.provider,
        t.timestamp,
        t.transaction_id
    FROM public.transactions t
    WHERE t.user_id = p_user_id
      AND (p_provider IS NULL OR t.provider = p_provider)
      AND (p_status IS NULL OR t.status = p_status)
    ORDER BY t.timestamp DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$;

COMMENT ON FUNCTION get_recent_transactions IS 'Get paginated recent transactions with optional filters';

-- ============================================
-- Upsert Merchant Settings
-- ============================================
CREATE OR REPLACE FUNCTION upsert_merchant_settings(
    p_user_id UUID,
    p_settings JSONB
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_settings_id UUID;
BEGIN
    INSERT INTO public.merchant_settings (user_id)
    VALUES (p_user_id)
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_settings_id;
    
    IF v_settings_id IS NULL THEN
        SELECT id INTO v_settings_id
        FROM public.merchant_settings
        WHERE user_id = p_user_id;
    END IF;
    
    UPDATE public.merchant_settings
    SET
        business_name = COALESCE((p_settings->>'business_name')::VARCHAR, business_name),
        business_type = COALESCE((p_settings->>'business_type')::VARCHAR, business_type),
        preferred_provider = COALESCE((p_settings->>'preferred_provider')::VARCHAR, preferred_provider),
        email_notifications = COALESCE((p_settings->>'email_notifications')::BOOLEAN, email_notifications),
        push_notifications = COALESCE((p_settings->>'push_notifications')::BOOLEAN, push_notifications),
        updated_at = NOW()
    WHERE id = v_settings_id;
    
    RETURN v_settings_id;
END;
$$;

COMMENT ON FUNCTION upsert_merchant_settings IS 'Insert or update merchant settings from JSON';

-- ============================================
-- Schedule Automated Jobs (PostgreSQL cron extension)
-- ============================================

-- Schedule cleanup job (every hour at :00)
SELECT cron.schedule(
    'cleanup-old-data',
    '0 * * * *',
    $$SELECT cleanup_old_data()$$
);

-- Schedule analytics refresh (every 5 minutes)
SELECT cron.schedule(
    'refresh-analytics',
    '*/5 * * * *',
    $$SELECT refresh_analytics_views()$$
);

-- Schedule webhook health check (every 30 minutes)
SELECT cron.schedule(
    'check-webhook-health',
    '*/30 * * * *',
    $$SELECT auto_disable_unhealthy_webhooks()$$
);

-- Schedule OTP cleanup (every 15 minutes)
SELECT cron.schedule(
    'cleanup-expired-otps',
    '*/15 * * * *',
    $$SELECT cleanup_expired_otps()$$
);

-- ============================================
-- Grant Execute Permissions
-- ============================================

GRANT EXECUTE ON FUNCTION get_transaction_stats TO authenticated;
GRANT EXECUTE ON FUNCTION update_device_activity TO authenticated;
GRANT EXECUTE ON FUNCTION check_webhook_health TO authenticated;
GRANT EXECUTE ON FUNCTION get_recent_transactions TO authenticated;
GRANT EXECUTE ON FUNCTION upsert_merchant_settings TO authenticated;
