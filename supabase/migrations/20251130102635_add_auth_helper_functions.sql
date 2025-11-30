-- Create a secure function to get user ID by phone number
-- This replaces the inefficient listUsers() approach

CREATE OR REPLACE FUNCTION public.get_user_id_by_phone(phone text)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  user_uuid uuid;
BEGIN
  -- Look up user ID from auth.users by phone
  SELECT id INTO user_uuid
  FROM auth.users
  WHERE phone = get_user_id_by_phone.phone
  LIMIT 1;
  
  RETURN user_uuid;
END;
$$;

-- Create function to atomically increment OTP attempts
CREATE OR REPLACE FUNCTION public.increment_otp_attempts(p_phone_number text)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  -- Increment attempts for all unverified OTPs for this phone
  UPDATE otp_codes
  SET attempts = attempts + 1
  WHERE phone_number = p_phone_number
    AND verified_at IS NULL
    AND expires_at > NOW();
END;
$$;

-- Grant execute permissions to service role
GRANT EXECUTE ON FUNCTION public.get_user_id_by_phone(text) TO service_role;
GRANT EXECUTE ON FUNCTION public.increment_otp_attempts(text) TO service_role;
