# Custom WhatsApp OTP Authentication Implementation

## 🎯 Overview

This implementation uses **custom Supabase Edge Functions** to send WhatsApp OTP directly via Meta's WhatsApp Business API, using your existing template `momo_terminal`.

## 📁 Files Created

### Supabase Edge Functions

1. **`supabase/functions/send-whatsapp-otp/index.ts`**
   - Sends WhatsApp OTP using Meta Business API
   - Stores OTP in database with expiry
   - Implements rate limiting (5 OTPs per hour)

2. **`supabase/functions/verify-whatsapp-otp/index.ts`**
   - Verifies OTP code
   - Creates user account if doesn't exist
   - Manages user sessions

### Android App Updates

1. **`EdgeFunctionsApi.kt`** - Retrofit interface for Edge Functions
2. **`SupabaseClient.kt`** - Added HTTP client for Edge Functions
3. **`SupabaseAuthService.kt`** - Updated to use custom Edge Functions
4. **`SupabaseModule.kt`** - Provides Edge Functions API

## 🚀 Deployment Steps

### Step 1: Deploy Edge Functions to Supabase

```bash
# Deploy send WhatsApp OTP function
supabase functions deploy send-whatsapp-otp --no-verify-jwt

# Deploy verify WhatsApp OTP function  
supabase functions deploy verify-whatsapp-otp --no-verify-jwt
```

### Step 2: Set Environment Secrets in Supabase

The functions use these secrets (already configured in your Supabase):

```bash
# WhatsApp credentials (already set)
WA_PHONE_ID=396791596844039
WHATSAPP_PHONE_NUMBER_ID=396791596844039
WA_TOKEN=EAAGHrMn6uugBO9xlSTNU1FsbnZB7AnBLCvTlgZCYQDZC8OZA7q3nrtxpxn3VgHiT8o9KbKQIyoPNrESHKZCq2c9B9lvNr2OsT8YDBewaDD1OzytQd74XlmSOgxZAVL6TEQpDT43zZCZBwQg9AZA5QPeksUVzmAqTaoNyIIaaqSvJniVmn6dW1rw88dbZAyR6VZBMTTpjQZDZD
WHATSAPP_ACCESS_TOKEN=EAAGHrMn6uugBO9xlSTNU1FsbnZB7AnBLCvTlgZCYQDZC8OZA7q3nrtxpxn3VgHiT8o9KbKQIyoPNrESHKZCq2c9B9lvNr2OsT8YDBewaDD1OzytQd74XlmSOgxZAVL6TEQpDT43zZCZBwQg9AZA5QPeksUVzmAqTaoNyIIaaqSvJniVmn6dW1rw88dbZAyR6VZBMTTpjQZDZD
META_WABA_BUSINESS_ID=297687286772462
WA_VERIFY_TOKEN=bd0e7b6f4a2c9d83f1e57a0c6b3d48e9
WA_APP_SECRET=e0b171d137e058e9055ae61bb94e0984

# Supabase secrets (auto-configured)
SUPABASE_URL=https://lhbowpbcpwoiparwnwgt.supabase.co
SUPABASE_SERVICE_ROLE_KEY=(from Supabase dashboard)
```

Verify secrets are set:
```bash
supabase secrets list
```

If any are missing, set them:
```bash
supabase secrets set WA_PHONE_ID=396791596844039
supabase secrets set WA_TOKEN=EAAGHrMn6uugBO9xlSTNU1FsbnZB7AnBLCvTlgZCYQDZC8OZA7q3nrtxpxn3VgHiT8o9KbKQIyoPNrESHKZCq2c9B9lvNr2OsT8YDBewaDD1OzytQd74XlmSOgxZAVL6TEQpDT43zZCZBwQg9AZA5QPeksUVzmAqTaoNyIIaaqSvJniVmn6dW1rw88dbZAyR6VZBMTTpjQZDZD
```

### Step 3: Deploy Database Migration

The OTP tables are already defined in `supabase/migrations/20251130000141_create_auth_tables.sql`.

Deploy via SQL Editor:
1. Go to: https://supabase.com/dashboard/project/lhbowpbcpwoiparwnwgt/sql/new
2. Copy contents of migration file
3. Run the SQL

Or use CLI:
```bash
supabase db push --linked
```

### Step 4: Test Edge Functions

Test sending OTP:
```bash
curl -i --location --request POST \
  'https://lhbowpbcpwoiparwnwgt.supabase.co/functions/v1/send-whatsapp-otp' \
  --header 'Authorization: Bearer YOUR_ANON_KEY' \
  --header 'Content-Type: application/json' \
  --data '{"phoneNumber":"+250788767816"}'
```

Test verifying OTP:
```bash
curl -i --location --request POST \
  'https://lhbowpbcpwoiparwnwgt.supabase.co/functions/v1/verify-whatsapp-otp' \
  --header 'Authorization: Bearer YOUR_ANON_KEY' \
  --header 'Content-Type: application/json' \
  --data '{"phoneNumber":"+250788767816","otpCode":"123456"}'
```

### Step 5: Fix Android Build Issues

There are compilation errors in the current Android code. To fix:

1. **Check imports** in `SupabaseAuthService.kt`
2. **Verify** all required dependencies are in `build.gradle.kts`
3. **Rebuild**: `./gradlew clean assembleDebug`

### Step 6: Install and Test

Once build succeeds:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 🔍 How It Works

### 1. Send OTP Flow

```
User enters phone → App calls send-whatsapp-otp Edge Function
→ Function generates 6-digit OTP → Saves to database
→ Calls Meta WhatsApp API with template "momo_terminal"
→ WhatsApp sends message to user
```

### 2. Verify OTP Flow

```
User enters OTP → App calls verify-whatsapp-otp Edge Function
→ Function checks OTP in database → Verifies not expired/used
→ Creates user account if new → Creates session
→ Returns success to app
```

## 📋 WhatsApp Template Format

Your template `momo_terminal` should have:

**Body**: 
```
{{1}} is your verification code. For your security, do not share this code.
Expires in 10 minutes.
```

**Button** (Copy Code):
```
Copy code: {{1}}
```

The `{{1}}` placeholder is replaced with the 6-digit OTP.

## 🐛 Troubleshooting

### "Function not found"
- Deploy edge functions: `supabase functions deploy`

### "WhatsApp API error"
- Check WA_TOKEN is valid and not expired
- Verify template "momo_terminal" is approved in Meta Business

### "OTP not received"
- Check Supabase logs: Dashboard → Logs → Edge Functions
- Check Meta Business logs for message delivery

### "Invalid OTP"
- OTPs expire after 5 minutes
- Maximum 5 attempts allowed
- Rate limit: 5 OTPs per hour per phone

## 📊 Monitoring

### Supabase Dashboard

1. **Edge Function Logs**:
   https://supabase.com/dashboard/project/lhbowpbcpwoiparwnwgt/logs/edge-functions

2. **Database OTP Codes**:
   ```sql
   SELECT * FROM otp_codes 
   ORDER BY created_at DESC 
   LIMIT 10;
   ```

3. **User Profiles**:
   ```sql
   SELECT * FROM user_profiles 
   ORDER BY created_at DESC;
   ```

### Meta Business Manager

Check message delivery:
https://business.facebook.com/wa/manage/message-templates/

## 🚨 Security Notes

1. ✅ OTP codes expire after 5 minutes
2. ✅ Rate limiting prevents abuse  
3. ✅ Maximum 5 verification attempts
4. ✅ Row Level Security on database tables
5. ✅ Service role key used server-side only

## 📚 References

- [Supabase Edge Functions](https://supabase.com/docs/guides/functions)
- [WhatsApp Business API](https://developers.facebook.com/docs/whatsapp/cloud-api)
- [WhatsApp Templates](https://developers.facebook.com/docs/whatsapp/cloud-api/guides/send-message-templates)

## ✅ Next Steps

1. Deploy edge functions to Supabase
2. Fix Android compilation errors
3. Build and install app
4. Test complete OTP flow
5. Monitor logs for any issues

---

**Note**: The Android app currently has compilation errors that need to be fixed before testing. The edge functions are ready to deploy.
