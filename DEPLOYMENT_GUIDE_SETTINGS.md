# Settings Refactoring - Deployment & Cleanup Guide

**Date**: December 6, 2025, 19:00 UTC  
**Status**: Ready for Deployment

---

## Step 1: Deploy Database Migrations to Supabase ⚡

### Option A: Using Supabase Dashboard (Recommended)

1. **Login to Supabase**: https://supabase.com/dashboard
2. **Select your project**: MomoTerminal
3. **Navigate to**: SQL Editor
4. **Execute migrations in order**:

#### Migration 1: Create Tables
Copy and paste content from:
`supabase/migrations/20251206180000_create_normalized_settings_tables.sql`

Click "Run" and verify:
```sql
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name LIKE 'merchant_%';
```

Expected result: 7 tables
- merchant_profiles
- merchant_business_details
- merchant_contact_info
- merchant_notification_prefs
- merchant_transaction_limits
- merchant_feature_flags
- merchant_payment_providers

#### Migration 2: Create Functions
Copy and paste content from:
`supabase/migrations/20251206180100_settings_helper_functions.sql`

Click "Run" and verify:
```sql
SELECT routine_name FROM information_schema.routines 
WHERE routine_schema = 'public' 
AND routine_name LIKE '%merchant%';
```

Expected result: 7 functions
- get_merchant_settings
- update_merchant_profile
- update_business_details
- update_notification_preferences
- update_transaction_limits
- update_feature_flags
- initialize_merchant_settings

#### Migration 3: Add RLS Policies
Copy and paste content from:
`supabase/migrations/20251206180200_settings_rls_policies.sql`

Click "Run" and verify:
```sql
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE schemaname = 'public' 
AND tablename LIKE 'merchant_%';
```

Expected result: All 7 tables have `rowsecurity = true`

### Option B: Using Supabase CLI

If you have Supabase CLI installed:

```bash
# Set environment variables
export SUPABASE_ACCESS_TOKEN=sbp_500607f0d078e919aa24f179473291544003a035
export SUPABASE_DB_URL="postgresql://postgres:Pq0jyevTlfoa376P@db.lhbowpbcpwoiparwnwgt.supabase.co:5432/postgres"

# Push migrations
supabase db push

# Verify
supabase db diff
```

---

## Step 2: Test Database Functions

Run these SQL queries in Supabase SQL Editor:

```sql
-- 1. Initialize a test merchant
SELECT initialize_merchant_settings(
    'test-user-uuid-12345'::uuid,
    'Test Merchant Ltd',
    'MERCH001'
);
-- Expected: Returns profile_id (UUID)

-- 2. Fetch settings
SELECT get_merchant_settings('test-user-uuid-12345'::uuid);
-- Expected: Returns JSON object with all settings

-- 3. Update profile
SELECT update_merchant_profile(
    'test-user-uuid-12345'::uuid,
    'Updated Merchant Name',
    'active'
);
-- Expected: Returns true

-- 4. Verify update
SELECT get_merchant_settings('test-user-uuid-12345'::uuid);
-- Expected: businessName should be 'Updated Merchant Name'

-- 5. Update feature flags
SELECT update_feature_flags(
    'test-user-uuid-12345'::uuid,
    true,  -- nfc_enabled
    true,  -- offline_mode
    true,  -- auto_sync
    false, -- biometric_required
    true,  -- receipts_enabled
    false, -- multi_currency
    false, -- advanced_analytics
    false  -- api_access
);
-- Expected: Returns true

-- 6. Clean up test data
DELETE FROM merchant_profiles WHERE user_id = 'test-user-uuid-12345'::uuid;
-- Expected: Cascading deletes remove all related records
```

---

## Step 3: Delete Duplicate Files (Manual)

### Files to Delete:

**From app module** (2 files):
```
app/src/main/java/com/momoterminal/presentation/screens/settings/SettingsScreen.kt
app/src/main/java/com/momoterminal/presentation/screens/settings/SettingsViewModel.kt
```

**From feature module** (1 file):
```
feature/settings/src/main/kotlin/com/momoterminal/feature/settings/SettingsViewModel.kt
```

### Using File Explorer:
1. Navigate to each file location
2. Delete the files
3. If the `settings` folder in app module is now empty, delete the folder too

### Using Terminal (if available):
```bash
# Delete app module duplicates
rm app/src/main/java/com/momoterminal/presentation/screens/settings/SettingsScreen.kt
rm app/src/main/java/com/momoterminal/presentation/screens/settings/SettingsViewModel.kt
rmdir app/src/main/java/com/momoterminal/presentation/screens/settings/

# Delete old feature module ViewModel
rm feature/settings/src/main/kotlin/com/momoterminal/feature/settings/SettingsViewModel.kt

# Rename new ViewModel
mv feature/settings/src/main/kotlin/com/momoterminal/feature/settings/viewmodel/SettingsViewModelNew.kt \
   feature/settings/src/main/kotlin/com/momoterminal/feature/settings/viewmodel/SettingsViewModel.kt
```

---

## Step 4: Verify Project Structure

After cleanup, your structure should be:

```
feature/settings/
├── src/main/kotlin/com/momoterminal/feature/settings/
│   ├── di/
│   │   └── SettingsModule.kt ✅
│   ├── ui/
│   │   └── SettingsScreen.kt (existing)
│   └── viewmodel/
│       └── SettingsViewModel.kt ✅ (renamed from SettingsViewModelNew.kt)

core/domain/
├── model/settings/
│   └── MerchantSettings.kt ✅
├── repository/
│   └── SettingsRepository.kt ✅
└── usecase/settings/
    ├── GetMerchantSettingsUseCase.kt ✅
    ├── Update*.kt ✅ (5 more files)
    └── impl/
        └── *Impl.kt ✅ (6 implementation files)

core/data/
├── mapper/
│   └── SettingsMapper.kt ✅
├── repository/
│   └── SettingsRepositoryImpl.kt ✅
└── di/
    └── RepositoryModule.kt ✅ (updated)

app/src/main/java/.../presentation/screens/
└── settings/ ❌ DELETED
```

---

## Step 5: Build & Test

### Clean Build
```bash
./gradlew clean
./gradlew assembleDebug
```

### If Build Fails:

**Common Issues:**

1. **Missing imports**:
   - Add `import javax.inject.Inject` to use case implementations
   - Add `import kotlinx.coroutines.flow.Flow` where needed

2. **DI conflicts**:
   - Ensure `SettingsModule.kt` is in correct package
   - Check `@InstallIn` annotation is ViewModelComponent

3. **Compilation errors**:
   - Check all Kotlin files for syntax errors
   - Verify package names match file locations

---

## Step 6: Commit Changes

```bash
# Add all new/modified files
git add supabase/migrations/20251206180*.sql
git add core/domain/src/main/kotlin/com/momoterminal/core/domain/model/settings/
git add core/domain/src/main/kotlin/com/momoterminal/core/domain/repository/SettingsRepository.kt
git add core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/
git add core/data/src/main/kotlin/com/momoterminal/core/data/repository/SettingsRepositoryImpl.kt
git add core/data/src/main/kotlin/com/momoterminal/core/data/mapper/SettingsMapper.kt
git add core/data/src/main/kotlin/com/momoterminal/core/data/di/RepositoryModule.kt
git add feature/settings/src/main/kotlin/com/momoterminal/feature/settings/viewmodel/SettingsViewModel.kt
git add feature/settings/src/main/kotlin/com/momoterminal/feature/settings/di/SettingsModule.kt
git add *.md

# Commit
git commit -m "feat: Settings refactoring - Clean architecture (Phases 1-4 complete)

- Normalized database schema (7 tables)
- Created domain models and use cases with validation
- Implemented repository pattern with Supabase
- Set up dependency injection
- Cleaned up duplicate code

Remaining: UI implementation (Phase 5)"

# Push
git push origin main
```

---

## Step 7: Update Supabase Migration History

After successful deployment, update the migration history in Supabase:

```bash
export SUPABASE_ACCESS_TOKEN=sbp_500607f0d078e919aa24f179473291544003a035
supabase migration repair --status applied \
  20251206180000 \
  20251206180100 \
  20251206180200
```

---

## ✅ Checklist

Before proceeding to Phase 5 (UI):

- [ ] Migrations deployed to Supabase
- [ ] Database functions tested and working
- [ ] RLS policies enabled on all tables
- [ ] Duplicate files deleted from app module
- [ ] Old ViewModel deleted from feature module
- [ ] New ViewModel renamed to SettingsViewModel.kt
- [ ] Project builds successfully (`./gradlew assembleDebug`)
- [ ] Changes committed to git
- [ ] Changes pushed to remote

---

## 🚨 Troubleshooting

### Migration Errors

**Error**: "relation already exists"
- Some tables may already exist from old schema
- Drop old tables or merge schemas carefully

**Error**: "function already exists"  
- Drop old functions first: `DROP FUNCTION IF EXISTS function_name;`

**Error**: "permission denied"
- Check Supabase credentials
- Verify user has SUPERUSER or CREATE permissions

### Build Errors

**Error**: "Unresolved reference"
- Check imports in use case implementations
- Verify all packages are correctly named

**Error**: "Cannot find symbol class"
- Run `./gradlew clean` first
- Check if generated code is stale

**Error**: "Dagger component missing"
- Rebuild project: `./gradlew build --refresh-dependencies`
- Check `@InstallIn` annotations

---

**Created**: December 6, 2025, 19:00 UTC  
**Next**: Phase 5 - UI Implementation
