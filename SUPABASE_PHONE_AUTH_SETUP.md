# Supabase Phone Authentication Setup

## Error: "Unsupported phone provider"

This error occurs because WhatsApp/Phone authentication is not enabled in your Supabase project.

## 🔧 Enable Phone Authentication in Supabase

### Step 1: Access Supabase Dashboard

1. Go to: https://supabase.com/dashboard/project/lhbowpbcpwoiparwnwgt/auth/providers
2. Login to your Supabase account

### Step 2: Enable Phone Auth Provider

1. Scroll to **Phone Auth** section
2. Click **Enable Phone provider**
3. Choose your SMS provider:
   - **Twilio** (Recommended for production)
   - **MessageBird**
   - **Textlocal**
   - **Vonage**

### Step 3: Configure SMS Provider (Twilio Example)

If using Twilio:

1. **Get Twilio Credentials**:
   - Sign up at https://www.twilio.com
   - Get Account SID
   - Get Auth Token
   - Get a Twilio phone number

2. **Configure in Supabase**:
   ```
   Account SID: Your_Twilio_Account_SID
   Auth Token: Your_Twilio_Auth_Token
   Sender Phone Number: +1234567890 (your Twilio number)
   ```

3. **SMS Template** (optional):
   ```
   Your OTP code is: {{ .Token }}
   Expires in 5 minutes.
   ```

### Step 4: Enable OTP Settings

1. Go to **Auth** → **Settings**
2. Enable:
   - ✅ Enable phone sign-ups
   - ✅ Enable phone confirmations
3. Set OTP expiry: `300` seconds (5 minutes)
4. Set OTP length: `6` digits

### Step 5: Save Configuration

Click **Save** at the bottom of the page.

## 🔄 Alternative: Use Email OTP (Temporary Solution)

If you want to test without setting up SMS:

1. Enable **Email Auth** instead:
   - Go to: https://supabase.com/dashboard/project/lhbowpbcpwoiparwnwgt/auth/providers
   - Enable **Email provider**
   - Configure SMTP or use Supabase's built-in email

2. The app will need to be modified to support email authentication

## 📱 Testing Phone Auth

After enabling phone authentication:

### Test with Supabase Dashboard

1. Go to SQL Editor
2. Run this to check phone auth is enabled:
   ```sql
   SELECT * FROM auth.config;
   ```

### Test OTP Generation

1. Use the app to send OTP
2. Check Supabase Auth logs:
   - Dashboard → Logs → Auth Logs
3. Verify OTP was sent

### Check Twilio Logs

1. Go to Twilio Console
2. Check Message Logs
3. Verify SMS was delivered

## 🔑 Environment Variables

After setup, update these if needed:

```bash
# In local.properties (already set)
SUPABASE_URL=https://lhbowpbcpwoiparwnwgt.supabase.co
SUPABASE_ANON_KEY=your_anon_key
```

## 🐛 Troubleshooting

### "Unsupported phone provider"
- **Cause**: Phone auth not enabled in Supabase
- **Fix**: Follow Step 2 above

### "Invalid phone number"
- **Cause**: Phone number not in E.164 format
- **Fix**: Ensure format is +250788123456

### "SMS delivery failed"
- **Cause**: Twilio credentials incorrect or insufficient balance
- **Fix**: Check Twilio console and verify credentials

### "Too many requests"
- **Cause**: Rate limiting
- **Fix**: Wait 60 seconds between OTP requests

## 📞 Cost Considerations

### Twilio Pricing (Example)
- SMS to Rwanda: ~$0.05 per message
- Free trial: $15 credit
- Sufficient for ~300 test messages

### Alternatives
- **MessageBird**: Often cheaper for African countries
- **Africa's Talking**: Specialized for African markets

## 🚀 Production Checklist

Before going live:

- [ ] Phone auth provider configured
- [ ] SMS provider credentials added
- [ ] Test OTP sending to multiple numbers
- [ ] Test OTP verification flow
- [ ] Set up rate limiting
- [ ] Configure custom SMS templates
- [ ] Add billing alerts on SMS provider
- [ ] Test in different countries/networks

## 📚 Documentation

- Supabase Phone Auth: https://supabase.com/docs/guides/auth/phone-login
- Twilio SMS: https://www.twilio.com/docs/sms
- Phone Auth Providers: https://supabase.com/docs/guides/auth/phone-login/twilio

