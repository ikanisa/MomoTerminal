-- Create table for OTP request logging (rate limiting & analytics)
CREATE TABLE IF NOT EXISTS public.otp_request_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  phone_number text NOT NULL,
  ip_address text NOT NULL,
  user_agent text,
  request_type text NOT NULL CHECK (request_type IN ('send_otp', 'verify_otp')),
  created_at timestamptz NOT NULL DEFAULT NOW()
);

-- Indexes for efficient rate limiting queries
CREATE INDEX IF NOT EXISTS idx_otp_logs_phone_created ON otp_request_logs(phone_number, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_otp_logs_ip_created ON otp_request_logs(ip_address, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_otp_logs_type_created ON otp_request_logs(request_type, created_at DESC);

-- Enable RLS
ALTER TABLE public.otp_request_logs ENABLE ROW LEVEL SECURITY;

-- Policy: Service role can do anything
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'otp_request_logs' 
        AND policyname = 'Service role full access'
    ) THEN
        CREATE POLICY "Service role full access" ON public.otp_request_logs
          FOR ALL USING (auth.role() = 'service_role');
    END IF;
END $$;

-- Add partitioning hint comment for future optimization
COMMENT ON TABLE public.otp_request_logs IS 'Logs all OTP requests for rate limiting and analytics. Consider partitioning by created_at when volume grows.';

-- Grant permissions
GRANT ALL ON public.otp_request_logs TO service_role;
GRANT SELECT ON public.otp_request_logs TO authenticated;

-- Create function to clean old logs (retention: 7 days)
CREATE OR REPLACE FUNCTION public.cleanup_old_otp_logs()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  DELETE FROM public.otp_request_logs
  WHERE created_at < NOW() - INTERVAL '7 days';
END;
$$;

-- Schedule cleanup (run daily at 3 AM UTC)
-- Note: Requires pg_cron extension
-- SELECT cron.schedule('cleanup-otp-logs', '0 3 * * *', 'SELECT cleanup_old_otp_logs()');

GRANT EXECUTE ON FUNCTION public.cleanup_old_otp_logs() TO service_role;
