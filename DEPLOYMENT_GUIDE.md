# Security Fixes Deployment Guide

## 🚨 IMPORTANT: Manual Database Migration Required

The Phase 1 security fixes require two database functions to be created manually in Supabase.

### Step 1: Access Supabase SQL Editor

1. Go to: https://supabase.com/dashboard/project/lhbowpbcpwoiparwnwgt/sql
2. Click "New Query"

### Step 2: Run This SQL

```sql
-- Create a secure function to get user ID by phone number
-- Replaces the inefficient listUsers() approach

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
```

### Step 3: Verify Functions Created

Run this query to verify:

```sql
SELECT 
  routine_name, 
  routine_type 
FROM information_schema.routines 
WHERE routine_schema = 'public' 
  AND routine_name IN ('get_user_id_by_phone', 'increment_otp_attempts');
```

Expected output: 2 rows showing both functions.

### Step 4: Test the Functions

```sql
-- Test get_user_id_by_phone (replace with actual phone)
SELECT get_user_id_by_phone('+250788767816');

-- Test increment_otp_attempts (replace with actual phone)
SELECT increment_otp_attempts('+250788767816');
```

---

## 🧪 Post-Deployment Testing

### 1. Test OTP Request

```bash
curl -X POST https://lhbowpbcpwoiparwnwgt.supabase.co/functions/v1/send-whatsapp-otp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ANON_KEY" \
  -d '{"phoneNumber": "+250788767816"}'
```

Expected:
- 200 status
- OTP sent to WhatsApp
- Check database: `SELECT code FROM otp_codes ORDER BY created_at DESC LIMIT 1;`
- Code should be a 64-character hex hash, NOT 6 digits

### 2. Test OTP Verification

Use the actual 6-digit code from WhatsApp (NOT the hash from DB):

```bash
curl -X POST https://lhbowpbcpwoiparwnwgt.supabase.co/functions/v1/verify-whatsapp-otp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ANON_KEY" \
  -d '{
    "phoneNumber": "+250788767816",
    "otpCode": "123456"
  }'
```

Expected:
- 200 status with tokens if correct OTP
- 400 status if wrong OTP (and attempts incremented)

### 3. Test Input Validation

**Invalid Phone:**
```bash
curl -X POST .../send-whatsapp-otp \
  -d '{"phoneNumber": "0788767816"}'  # Missing +
```
Expected: 400 "Invalid phone number format"

**Invalid OTP Format:**
```bash
curl -X POST .../verify-whatsapp-otp \
  -d '{"phoneNumber": "+250788767816", "otpCode": "12345"}'  # Only 5 digits
```
Expected: 400 "OTP must be exactly 6 digits"

### 4. Test Attempt Limit

Send 6 verification requests with wrong OTP:
- First 5 should return 400 "Invalid or expired OTP"
- 6th attempt should be blocked

### 5. Verify Android App

1. Build and install latest APK
2. Request OTP
3. Enter code from WhatsApp
4. Verify authentication works

---

## 🔧 Rollback Plan (If Issues Occur)

### Revert Edge Functions

```bash
# Get previous deployment
cd supabase
npx supabase functions list

# Restore from git
git revert HEAD
git push origin main

# Redeploy old versions
npx supabase functions deploy send-whatsapp-otp
npx supabase functions deploy verify-whatsapp-otp
```

### Drop Database Functions

```sql
DROP FUNCTION IF EXISTS public.get_user_id_by_phone(text);
DROP FUNCTION IF EXISTS public.increment_otp_attempts(text);
```

---

## 📊 Monitoring

Check logs after deployment:

```bash
# Function logs
npx supabase functions logs send-whatsapp-otp --tail
npx supabase functions logs verify-whatsapp-otp --tail
```

Look for:
- "Looking for OTP hash for phone"
- "Found existing auth user" (should appear, not "listing users")
- No error stack traces

---

## ✅ Deployment Checklist

- [ ] Backup current database
- [ ] Run SQL to create database functions
- [ ] Verify functions created successfully
- [ ] Edge functions already deployed (auto-deployed via CLI)
- [ ] Test OTP request → verify hash in database
- [ ] Test OTP verification → verify works with actual code
- [ ] Test input validation errors
- [ ] Test attempt limit (5 max)
- [ ] Test on Android app
- [ ] Monitor logs for 1 hour
- [ ] Mark as complete in SECURITY_FIXES_PHASE1.md

---

## 🆘 Troubleshooting

### "Function get_user_id_by_phone does not exist"

→ SQL functions not created. Re-run Step 2 above.

### "Invalid or expired OTP" with correct code

→ Check if OTP is hashed in database:
```sql
SELECT code, phone_number FROM otp_codes ORDER BY created_at DESC LIMIT 3;
```
If seeing 6-digit codes → functions not deployed correctly.

### Session creation fails

→ Check Supabase logs for actual error. May need to verify service role key.

---

**Deployed by:** CLI (automated)  
**Date:** 2025-11-30  
**Status:** ⚠️ **Requires manual DB migration** (Step 2 above)
