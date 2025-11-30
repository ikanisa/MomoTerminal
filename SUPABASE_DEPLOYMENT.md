# Supabase Deployment Guide

## 📦 Migration Status

✅ **Migration file created**: `supabase/migrations/20251130000141_create_auth_tables.sql`  
✅ **Committed to Git**: Pushed to `main` branch  
🔗 **Linked Project**: easyMO (lhbowpbcpwoiparwnwgt)

## 🚀 Deployment Options

### Option 1: Deploy via Supabase Dashboard (Recommended)

1. Open the SQL Editor in your Supabase dashboard:
   ```
   https://supabase.com/dashboard/project/lhbowpbcpwoiparwnwgt/sql/new
   ```

2. Copy the entire contents of `supabase/migrations/20251130000141_create_auth_tables.sql`

3. Paste into the SQL Editor and click **Run**

4. Verify the tables were created:
   ```sql
   SELECT tablename FROM pg_tables 
   WHERE schemaname = 'public' 
   AND tablename IN ('otp_codes', 'user_profiles');
   ```

### Option 2: Deploy via CLI (Advanced)

If your local migration history is in sync:

```bash
supabase db push --linked
```

**Note**: Currently, the remote database has many migrations not tracked locally. You may need to sync first by marking remote migrations as applied.

## 📋 What This Migration Creates

### Tables

1. **`otp_codes`** - Stores OTP codes for WhatsApp authentication
   - 6-digit OTP codes
   - Expiry tracking
   - Verification attempts (max 5)
   - Rate limiting support

2. **`user_profiles`** - Extended user profile data
   - Linked to Supabase auth.users
   - Merchant information
   - Terms acceptance tracking

### Security

- **Row Level Security (RLS)** enabled on both tables
- Service role can manage OTP codes
- Users can only access their own profiles

### Helper Functions

- `generate_otp()` - Generate random 6-digit OTP
- `check_otp_rate_limit(phone)` - Rate limiting (5 OTPs/hour)
- `cleanup_expired_otps()` - Clean up expired codes

### Triggers

- Auto-update `updated_at` timestamp on `user_profiles`

## 🔍 Verification

After deployment, verify with:

```sql
-- Check tables exist
\dt otp_codes
\dt user_profiles

-- Check functions
\df generate_otp
\df check_otp_rate_limit
\df cleanup_expired_otps

-- Check RLS policies
SELECT tablename, policyname 
FROM pg_policies 
WHERE tablename IN ('otp_codes', 'user_profiles');
```

## 🔧 Troubleshooting

### If tables already exist

The migration uses `CREATE TABLE IF NOT EXISTS`, so it won't fail if tables already exist. However, you may want to check if the schema matches:

```sql
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'otp_codes';
```

### If you need to rollback

```sql
DROP TABLE IF EXISTS otp_codes CASCADE;
DROP TABLE IF EXISTS user_profiles CASCADE;
DROP FUNCTION IF EXISTS generate_otp();
DROP FUNCTION IF EXISTS check_otp_rate_limit(VARCHAR);
DROP FUNCTION IF EXISTS cleanup_expired_otps();
```

## 📱 Next Steps

After deployment:

1. ✅ Test OTP generation via Supabase Edge Functions
2. ✅ Test WhatsApp OTP sending
3. ✅ Test OTP verification flow
4. ✅ Set up periodic cleanup job for expired OTPs
5. ✅ Monitor rate limiting

## 📞 Support

For issues or questions:
- Check Supabase logs: https://supabase.com/dashboard/project/lhbowpbcpwoiparwnwgt/logs
- Review migration file: `supabase/migrations/20251130000141_create_auth_tables.sql`
- Check documentation: `docs/supabase/`
