-- Analytics and Monitoring Functions for OTP System

-- 1. Get OTP delivery success rate (last 24 hours)
CREATE OR REPLACE FUNCTION public.get_otp_delivery_stats(hours int DEFAULT 24)
RETURNS TABLE (
  total_sent bigint,
  total_verified bigint,
  success_rate numeric,
  avg_verification_time interval
) 
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  RETURN QUERY
  WITH stats AS (
    SELECT 
      COUNT(*) as sent_count,
      COUNT(CASE WHEN verified_at IS NOT NULL THEN 1 END) as verified_count,
      AVG(CASE 
        WHEN verified_at IS NOT NULL 
        THEN verified_at - created_at 
      END) as avg_time
    FROM otp_codes
    WHERE created_at > NOW() - (hours || ' hours')::interval
  )
  SELECT 
    sent_count::bigint,
    verified_count::bigint,
    ROUND((verified_count::numeric / NULLIF(sent_count, 0) * 100), 2) as success_rate,
    avg_time
  FROM stats;
END;
$$;

-- 2. Get rate limit violations (security monitoring)
CREATE OR REPLACE FUNCTION public.get_rate_limit_violations(hours int DEFAULT 1)
RETURNS TABLE (
  ip_address text,
  phone_number text,
  request_count bigint,
  first_request timestamptz,
  last_request timestamptz
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  RETURN QUERY
  SELECT 
    orl.ip_address,
    orl.phone_number,
    COUNT(*) as request_count,
    MIN(orl.created_at) as first_request,
    MAX(orl.created_at) as last_request
  FROM otp_request_logs orl
  WHERE orl.created_at > NOW() - (hours || ' hours')::interval
  GROUP BY orl.ip_address, orl.phone_number
  HAVING COUNT(*) > 10  -- Threshold for suspicious activity
  ORDER BY request_count DESC;
END;
$$;

-- 3. Get failed verification attempts (security monitoring)
CREATE OR REPLACE FUNCTION public.get_failed_verifications(hours int DEFAULT 24)
RETURNS TABLE (
  phone_number text,
  ip_address text,
  attempts bigint,
  last_attempt timestamptz
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  RETURN QUERY
  SELECT 
    oc.phone_number,
    orl.ip_address,
    oc.attempts::bigint,
    MAX(orl.created_at) as last_attempt
  FROM otp_codes oc
  LEFT JOIN otp_request_logs orl ON oc.phone_number = orl.phone_number
  WHERE oc.created_at > NOW() - (hours || ' hours')::interval
    AND oc.verified_at IS NULL
    AND oc.attempts >= 3
  GROUP BY oc.phone_number, orl.ip_address, oc.attempts
  ORDER BY oc.attempts DESC, last_attempt DESC;
END;
$$;

-- 4. Get OTP request volume by hour (analytics dashboard)
CREATE OR REPLACE FUNCTION public.get_otp_hourly_volume(days int DEFAULT 7)
RETURNS TABLE (
  hour timestamptz,
  send_count bigint,
  verify_count bigint,
  success_count bigint
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  RETURN QUERY
  SELECT 
    date_trunc('hour', orl.created_at) as hour,
    COUNT(CASE WHEN orl.request_type = 'send_otp' THEN 1 END) as send_count,
    COUNT(CASE WHEN orl.request_type = 'verify_otp' THEN 1 END) as verify_count,
    COUNT(CASE 
      WHEN orl.request_type = 'verify_otp' 
      AND EXISTS (
        SELECT 1 FROM otp_codes oc 
        WHERE oc.phone_number = orl.phone_number 
        AND oc.verified_at IS NOT NULL
        AND oc.verified_at > orl.created_at - interval '1 minute'
      )
      THEN 1 
    END) as success_count
  FROM otp_request_logs orl
  WHERE orl.created_at > NOW() - (days || ' days')::interval
  GROUP BY date_trunc('hour', orl.created_at)
  ORDER BY hour DESC;
END;
$$;

-- 5. Get top users by OTP requests (abuse detection)
CREATE OR REPLACE FUNCTION public.get_top_otp_users(hours int DEFAULT 24, limit_count int DEFAULT 20)
RETURNS TABLE (
  phone_number text,
  request_count bigint,
  unique_ips bigint,
  first_request timestamptz,
  last_request timestamptz
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  RETURN QUERY
  SELECT 
    orl.phone_number,
    COUNT(*) as request_count,
    COUNT(DISTINCT orl.ip_address) as unique_ips,
    MIN(orl.created_at) as first_request,
    MAX(orl.created_at) as last_request
  FROM otp_request_logs orl
  WHERE orl.created_at > NOW() - (hours || ' hours')::interval
  GROUP BY orl.phone_number
  ORDER BY request_count DESC
  LIMIT limit_count;
END;
$$;

-- Grant execute permissions
GRANT EXECUTE ON FUNCTION public.get_otp_delivery_stats(int) TO service_role, authenticated;
GRANT EXECUTE ON FUNCTION public.get_rate_limit_violations(int) TO service_role;
GRANT EXECUTE ON FUNCTION public.get_failed_verifications(int) TO service_role;
GRANT EXECUTE ON FUNCTION public.get_otp_hourly_volume(int) TO service_role, authenticated;
GRANT EXECUTE ON FUNCTION public.get_top_otp_users(int, int) TO service_role;

-- Add helpful comments
COMMENT ON FUNCTION public.get_otp_delivery_stats IS 'Returns OTP delivery statistics including success rate';
COMMENT ON FUNCTION public.get_rate_limit_violations IS 'Identifies potential abuse/attack patterns';
COMMENT ON FUNCTION public.get_failed_verifications IS 'Lists phone numbers with multiple failed attempts';
COMMENT ON FUNCTION public.get_otp_hourly_volume IS 'Returns hourly OTP request volumes for dashboards';
COMMENT ON FUNCTION public.get_top_otp_users IS 'Returns users with highest OTP request volumes';
