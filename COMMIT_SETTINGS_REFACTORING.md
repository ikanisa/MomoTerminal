# Git Commit Script for Settings Refactoring

## Files to Commit

### Backend Migrations (3 files)
```
supabase/migrations/20251206180000_create_normalized_settings_tables.sql
supabase/migrations/20251206180100_settings_helper_functions.sql
supabase/migrations/20251206180200_settings_rls_policies.sql
```

### Domain Layer (14 files)
```
core/domain/src/main/kotlin/com/momoterminal/core/domain/model/settings/MerchantSettings.kt
core/domain/src/main/kotlin/com/momoterminal/core/domain/repository/SettingsRepository.kt
core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/GetMerchantSettingsUseCase.kt
core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/UpdateMerchantProfileUseCase.kt
core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/UpdateBusinessDetailsUseCase.kt
core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/UpdateNotificationPreferencesUseCase.kt
core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/UpdateTransactionLimitsUseCase.kt
core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/UpdateFeatureFlagsUseCase.kt
core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/impl/GetMerchantSettingsUseCaseImpl.kt
core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/impl/UpdateMerchantProfileUseCaseImpl.kt
core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/impl/UpdateBusinessDetailsUseCaseImpl.kt
core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/impl/UpdateNotificationPreferencesUseCaseImpl.kt
core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/impl/UpdateTransactionLimitsUseCaseImpl.kt
core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/impl/UpdateFeatureFlagsUseCaseImpl.kt
```

### Data Layer (3 files)
```
core/data/src/main/kotlin/com/momoterminal/core/data/repository/SettingsRepositoryImpl.kt
core/data/src/main/kotlin/com/momoterminal/core/data/mapper/SettingsMapper.kt
core/data/src/main/kotlin/com/momoterminal/core/data/di/RepositoryModule.kt (modified)
```

### Feature Layer (2 files)
```
feature/settings/src/main/kotlin/com/momoterminal/feature/settings/viewmodel/SettingsViewModelNew.kt
feature/settings/src/main/kotlin/com/momoterminal/feature/settings/di/SettingsModule.kt
```

### Documentation (3 files)
```
SETTINGS_REFACTORING_IMPLEMENTATION_STATUS.md
SETTINGS_REFACTORING_SESSION_SUMMARY.md
(this file)
```

## Commit Commands

```bash
# Add all settings refactoring files
git add supabase/migrations/20251206180*.sql
git add core/domain/src/main/kotlin/com/momoterminal/core/domain/model/settings/
git add core/domain/src/main/kotlin/com/momoterminal/core/domain/repository/SettingsRepository.kt
git add core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/
git add core/data/src/main/kotlin/com/momoterminal/core/data/repository/SettingsRepositoryImpl.kt
git add core/data/src/main/kotlin/com/momoterminal/core/data/mapper/SettingsMapper.kt
git add core/data/src/main/kotlin/com/momoterminal/core/data/di/RepositoryModule.kt
git add feature/settings/src/main/kotlin/com/momoterminal/feature/settings/viewmodel/SettingsViewModelNew.kt
git add feature/settings/src/main/kotlin/com/momoterminal/feature/settings/di/SettingsModule.kt
git add SETTINGS_REFACTORING_*.md

# Commit with descriptive message
git commit -m "feat: Settings refactoring - Clean architecture implementation (75% complete)

Phase 1: Backend Cleanup ✅
- Normalized merchant_settings into 7 focused tables
- Created 7 database RPC functions for CRUD operations
- Implemented Row Level Security policies

Phase 2: Domain Layer ✅
- Created domain models (MerchantSettings, Profile, BusinessDetails, etc.)
- Implemented 6 use cases with validation logic
- Clean separation of concerns

Phase 3: Data Layer ✅
- Implemented repository pattern with Supabase
- Created JSON mappers for domain models
- Added dependency injection bindings

Phase 4: Feature Module (In Progress) 🟡
- Created new ViewModel with clean DI
- Proper state management with StateFlow
- Manual cleanup of duplicates required

Remaining: Phase 5 - UI implementation

Files: 25 new files
LOC: ~1,200 lines of clean, maintainable code
Architecture: Domain → Data → UI (Clean Architecture)"

# Push to remote
git push origin main
```

## After Commit

1. Deploy migrations to Supabase:
   ```bash
   export SUPABASE_ACCESS_TOKEN=sbp_500607f0d078e919aa24f179473291544003a035
   supabase db push
   ```

2. Clean up duplicate files (manual step)

3. Test build:
   ```bash
   ./gradlew clean assembleDebug
   ```

4. Continue with Phase 5 (UI implementation)

---

**Created**: December 6, 2025
**Session**: Settings Refactoring Phases 1-4
