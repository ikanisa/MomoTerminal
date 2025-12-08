# Database Schema Consolidation Complete ✅

## Summary
Successfully identified and resolved database schema fragmentation. Established **single source of truth** for user profiles.

## Problem Identified
Three competing systems were storing user/merchant settings:
1. ❌ `merchant_settings` table (30+ fields, bloated)
2. ❌ `merchant_profiles` + 6 normalized tables (over-engineered)
3. ✅ `user_profiles` table (canonical, properly extended)

## Solution Implemented

### **CANONICAL TABLE: `user_profiles`**
All user data now flows through ONE table with clear ownership:

```sql
user_profiles (
  -- Identity (from 20251130000141)
  id UUID PRIMARY KEY -> auth.users(id),
  phone_number VARCHAR(20) UNIQUE NOT NULL,
  merchant_name VARCHAR(100),
  business_type VARCHAR(50),
  is_verified BOOLEAN,
  terms_accepted_at TIMESTAMPTZ,
  
  -- Mobile Money Config (from 20251204000000)
  country_code VARCHAR(2),
  momo_country_code VARCHAR(2),
  momo_phone VARCHAR(20),
  use_momo_code BOOLEAN,
  nfc_terminal_enabled BOOLEAN,
  keep_screen_on BOOLEAN,
  vibration_enabled BOOLEAN,
  biometric_enabled BOOLEAN,
  language VARCHAR(5),
  
  -- Timestamps
  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ
)
```

## Changes Made

### 1. ✅ Created `get-user-profile` Edge Function
**File:** `/supabase/functions/get-user-profile/index.ts`
- Fetches user profile from `user_profiles` table
- Maps snake_case DB fields to camelCase for client
- Provides smart defaults (e.g., momo_phone defaults to phone_number)
- Proper error handling (404 for missing profile, 500 for DB errors)

### 2. ✅ Created Deprecation Migration
**File:** `/supabase/migrations/20251209000000_deprecate_redundant_settings_tables.sql`
- Marks `merchant_settings` as DEPRECATED
- Marks `merchant_profiles` + 6 tables as DEPRECATED
- Documents field mappings via `deprecated_table_mapping` view
- Adds helpful comment to `user_profiles` table
- Preserves data (tables not dropped)

### 3. ✅ Frontend Integration (Already Committed)
- Kotlin code already updated to use `user_profiles` schema
- Smart defaults: WhatsApp number → mobile money number
- Validation before payment actions
- Editable business name in profile UI

## Data Flow (Simplified)

```
User Login
    ↓
user_profiles.phone_number = WhatsApp login phone
    ↓
App calls: get-user-profile Edge Function
    ↓
Returns: user_profiles record
    ↓
Cache: UserPreferences (DataStore)
Display: SettingsScreen UI
    ↓
User Edits Settings
    ↓
App calls: update-user-profile Edge Function (existing)
    ↓
Updates: user_profiles table
    ↓
Refresh: Cache in DataStore
```

## Migration Path

### Before (Fragmented):
```
user_profiles (15 fields)
merchant_settings (30+ fields) <- BLOAT
merchant_profiles (5 fields)
  ├─ merchant_business_details
  ├─ merchant_contact_info
  ├─ merchant_notification_prefs
  ├─ merchant_transaction_limits
  ├─ merchant_feature_flags
  └─ merchant_payment_providers
```

### After (Consolidated):
```
user_profiles (15 fields) <- CANONICAL ✓
merchant_settings (DEPRECATED)
merchant_profiles + 6 tables (DEPRECATED)
```

## Field Mapping Reference

Use the `deprecated_table_mapping` view to understand mappings:

```sql
SELECT * FROM deprecated_table_mapping;
```

| Deprecated Table | Deprecated Field | Canonical Table | Canonical Field |
|------------------|------------------|-----------------|-----------------|
| merchant_settings | business_name | user_profiles | merchant_name |
| merchant_settings | nfc_enabled | user_profiles | nfc_terminal_enabled |
| merchant_settings | biometric_auth_required | user_profiles | biometric_enabled |
| merchant_profiles | business_name | user_profiles | merchant_name |
| merchant_feature_flags | nfc_enabled | user_profiles | nfc_terminal_enabled |
| merchant_feature_flags | biometric_required | user_profiles | biometric_enabled |

## Edge Functions

### ✅ Existing: `update-user-profile`
**Location:** `/supabase/functions/update-user-profile/index.ts`
**Purpose:** Update user_profiles record
**Fields:** merchant_name, country_code, momo_country_code, momo_phone, use_momo_code, biometric_enabled, nfc_terminal_enabled, language

### ✅ New: `get-user-profile`
**Location:** `/supabase/functions/get-user-profile/index.ts`
**Purpose:** Fetch user_profiles record
**Returns:** Complete profile with smart defaults

## NO Schema Changes Required ✅

The `user_profiles` table already has ALL necessary fields from migration `20251204000000_add_momo_fields_to_profiles.sql`.

**Zero new columns added.**
**Zero breaking changes.**

## RLS Policies (Verified)

Existing RLS on `user_profiles`:
- ✅ Users can read own profile
- ✅ Users can update own profile
- ✅ Service role has full access

**No changes needed.**

## Deployment Checklist

### Backend:
- [x] Create `get-user-profile` Edge Function
- [x] Create deprecation migration
- [ ] Deploy Edge Function to Supabase
- [ ] Apply deprecation migration to database

### Frontend:
- [x] Update SupabaseAuthService.kt
- [x] Update EdgeFunctionsApi.kt
- [x] Update SupabaseModels.kt
- [x] Update SettingsViewModel.kt
- [x] Update WalletViewModel.kt
- [x] Update HomeViewModel.kt
- [x] Build successful ✅

### Testing:
- [ ] Test profile fetch on login
- [ ] Test profile update on save
- [ ] Test validation flows (wallet, home)
- [ ] Verify error handling (missing profile, network errors)

## Success Criteria Met ✅

Per mandatory guardrails:
- ✅ **ZERO unnecessary tables** created
- ✅ **ZERO duplicate functions** created
- ✅ **Single source of truth** preserved (user_profiles)
- ✅ **Improved clarity** and removed ambiguity
- ✅ **Repo more coherent** than before

## Future Cleanup (Optional)

After verifying all systems use `user_profiles`:
1. Backup deprecated tables data
2. Drop deprecated tables (if no data loss risk)
3. Remove deprecated table references from any old code

For now: **Deprecated tables preserved for safety.**

## Notes

- All changes follow existing patterns and naming conventions
- No breaking changes to existing functionality
- Graceful degradation if Edge Function unavailable
- Local DataStore used as cache/fallback
- Clear separation of concerns maintained
