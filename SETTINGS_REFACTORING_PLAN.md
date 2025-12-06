# Settings Fullstack Refactoring Plan

**Date**: December 6, 2025  
**Priority**: P1 - High (Architecture Cleanup)  
**Estimated Time**: 6-8 hours  
**Status**: 🔴 **NEEDS IMMEDIATE REFACTORING**

---

## 🔍 Current State Analysis

### Critical Issues Identified

#### 1. **Duplicate Settings ViewModels and Screens** 🔴
```
DUPLICATES FOUND:
├── app/src/main/java/com/momoterminal/presentation/screens/settings/
│   ├── SettingsScreen.kt (838 lines)
│   └── SettingsViewModel.kt (345 lines)
├── feature/settings/src/main/kotlin/com/momoterminal/feature/settings/
│   ├── SettingsViewModel.kt
│   ├── ui/SettingsScreen.kt
│   └── viewmodel/SettingsViewModel.kt
```

**Problem**: Multiple versions of the same functionality scattered across modules

#### 2. **Missing Proper Architecture Layers**
```
CURRENT (WRONG):
app/
  presentation/screens/settings/  ← UI in app module (should be in feature)
  
feature/settings/
  ??? ← Unclear what's actually here vs app
  
MISSING:
- Proper domain layer for settings
- Repository pattern
- Use cases / Interactors
- Clear data sources (local vs remote)
```

#### 3. **Backend Schema Issues**
```sql
-- merchant_settings table has 50+ columns! 🔴
-- Should be normalized into multiple tables:
- merchant_business_info
- merchant_notification_preferences  
- merchant_transaction_limits
- merchant_feature_flags
```

#### 4. **No Clear Data Flow**
```
UI → ??? → Database
(Missing: ViewModel → UseCase → Repository → DataSource layers)
```

---

## 🎯 Refactoring Strategy

### Phase 1: Backend Cleanup (2 hours)

#### 1.1 Normalize Database Schema
```sql
-- BEFORE: Single bloated table
merchant_settings (50+ columns)

-- AFTER: Normalized tables
merchant_profiles (id, user_id, business_name, merchant_code, created_at)
merchant_business_details (profile_id, business_type, tax_id, registration_number, location)
merchant_contact_info (profile_id, email, phone, address, whatsapp)
merchant_notification_prefs (profile_id, email_enabled, sms_enabled, push_enabled, events_config)
merchant_transaction_limits (profile_id, daily_limit, single_limit, monthly_limit, min_amount)
merchant_feature_flags (profile_id, nfc_enabled, offline_enabled, auto_sync, biometric_required)
merchant_payment_providers (profile_id, provider_name, is_preferred, settings_json, is_enabled)
```

#### 1.2 Create Migration Scripts
```
supabase/migrations/
  20251206_refactor_settings_schema/
    01_create_normalized_tables.sql
    02_migrate_existing_data.sql
    03_add_indexes.sql
    04_add_rls_policies.sql
    05_drop_old_table.sql
```

#### 1.3 Create Database Functions
```sql
-- Helper functions for settings management
get_merchant_settings(user_id UUID)
update_merchant_profile(user_id UUID, settings JSONB)
update_notification_preferences(user_id UUID, prefs JSONB)
update_transaction_limits(user_id UUID, limits JSONB)
```

---

### Phase 2: Domain Layer (1.5 hours)

#### 2.1 Define Domain Models
```kotlin
// core/domain/src/main/kotlin/com/momoterminal/core/domain/model/settings/

data class MerchantProfile(
    val id: String,
    val userId: String,
    val businessName: String,
    val merchantCode: String,
    val createdAt: Instant
)

data class BusinessDetails(
    val businessType: BusinessType,
    val taxId: String?,
    val registrationNumber: String?,
    val location: Location?
)

data class NotificationPreferences(
    val emailEnabled: Boolean,
    val smsEnabled: Boolean,
    val pushEnabled: Boolean,
    val whatsappEnabled: Boolean,
    val events: NotificationEvents
)

data class TransactionLimits(
    val dailyLimit: BigDecimal?,
    val singleTransactionLimit: BigDecimal?,
    val monthlyLimit: BigDecimal?,
    val minimumAmount: BigDecimal
)

data class FeatureFlags(
    val nfcEnabled: Boolean,
    val offlineMode: Boolean,
    val autoSync: Boolean,
    val biometricRequired: Boolean,
    val receiptsEnabled: Boolean
)

// Aggregate all settings
data class MerchantSettings(
    val profile: MerchantProfile,
    val businessDetails: BusinessDetails,
    val contactInfo: ContactInfo,
    val notificationPrefs: NotificationPreferences,
    val transactionLimits: TransactionLimits,
    val featureFlags: FeatureFlags,
    val paymentProviders: List<PaymentProvider>
)
```

#### 2.2 Define Use Cases
```kotlin
// core/domain/src/main/kotlin/com/momoterminal/core/domain/usecase/settings/

interface GetMerchantSettingsUseCase {
    suspend operator fun invoke(userId: String): Result<MerchantSettings>
}

interface UpdateMerchantProfileUseCase {
    suspend operator fun invoke(profile: MerchantProfile): Result<Unit>
}

interface UpdateNotificationPreferencesUseCase {
    suspend operator fun invoke(prefs: NotificationPreferences): Result<Unit>
}

interface UpdateTransactionLimitsUseCase {
    suspend operator fun invoke(limits: TransactionLimits): Result<Unit>
}

interface UpdateFeatureFlagsUseCase {
    suspend operator fun invoke(flags: FeatureFlags): Result<Unit>
}
```

---

### Phase 3: Data Layer (2 hours)

#### 3.1 Create Repository Interface
```kotlin
// core/domain/src/main/kotlin/com/momoterminal/core/domain/repository/

interface SettingsRepository {
    suspend fun getMerchantSettings(userId: String): Result<MerchantSettings>
    suspend fun updateProfile(profile: MerchantProfile): Result<Unit>
    suspend fun updateBusinessDetails(details: BusinessDetails): Result<Unit>
    suspend fun updateNotificationPrefs(prefs: NotificationPreferences): Result<Unit>
    suspend fun updateTransactionLimits(limits: TransactionLimits): Result<Unit>
    suspend fun updateFeatureFlags(flags: FeatureFlags): Result<Unit>
    suspend fun updatePaymentProviders(providers: List<PaymentProvider>): Result<Unit>
    
    // Observables for real-time updates
    fun observeMerchantSettings(userId: String): Flow<MerchantSettings>
}
```

#### 3.2 Implement Repository
```kotlin
// core/data/src/main/kotlin/com/momoterminal/data/repository/

class SettingsRepositoryImpl @Inject constructor(
    private val remoteDataSource: SettingsRemoteDataSource,
    private val localDataSource: SettingsLocalDataSource,
    private val settingsCache: SettingsCache
) : SettingsRepository {
    
    override suspend fun getMerchantSettings(userId: String): Result<MerchantSettings> {
        // Try cache first
        settingsCache.get(userId)?.let { return Result.success(it) }
        
        // Try local database
        localDataSource.getSettings(userId)?.let { 
            settingsCache.set(userId, it)
            return Result.success(it) 
        }
        
        // Fetch from remote
        return remoteDataSource.getSettings(userId)
            .onSuccess { settings ->
                localDataSource.saveSettings(settings)
                settingsCache.set(userId, settings)
            }
    }
    
    // Implement other methods with offline-first pattern
}
```

#### 3.3 Create Data Sources
```kotlin
// core/data/src/main/kotlin/com/momoterminal/data/source/settings/

interface SettingsRemoteDataSource {
    suspend fun getSettings(userId: String): Result<MerchantSettings>
    suspend fun updateSettings(userId: String, settings: MerchantSettings): Result<Unit>
}

class SettingsSupabaseDataSource @Inject constructor(
    private val supabase: SupabaseClient
) : SettingsRemoteDataSource {
    override suspend fun getSettings(userId: String): Result<MerchantSettings> {
        return try {
            // Call Supabase RPC function
            val response = supabase.rpc("get_merchant_settings")
                .params(mapOf("user_id" to userId))
                .execute()
                .decodeAs<MerchantSettingsDto>()
            
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

interface SettingsLocalDataSource {
    suspend fun getSettings(userId: String): MerchantSettings?
    suspend fun saveSettings(settings: MerchantSettings)
    suspend fun clearSettings(userId: String)
}

class SettingsRoomDataSource @Inject constructor(
    private val dao: SettingsDao
) : SettingsLocalDataSource {
    // Room DAO implementation
}
```

---

### Phase 4: Feature Module Cleanup (1.5 hours)

#### 4.1 Consolidate to Single Feature Module
```
feature/settings/
├── src/main/kotlin/com/momoterminal/feature/settings/
│   ├── ui/
│   │   ├── SettingsScreen.kt
│   │   ├── ProfileSettingsScreen.kt
│   │   ├── NotificationSettingsScreen.kt
│   │   ├── LimitsSettingsScreen.kt
│   │   ├── ProviderSettingsScreen.kt
│   │   └── components/
│   │       ├── SettingsToggleItem.kt
│   │       ├── SettingsSlider.kt
│   │       └── ProviderCard.kt
│   ├── viewmodel/
│   │   ├── SettingsViewModel.kt (consolidated, clean)
│   │   ├── ProfileSettingsViewModel.kt
│   │   └── NotificationSettingsViewModel.kt
│   ├── navigation/
│   │   └── SettingsNavigation.kt
│   └── di/
│       └── SettingsModule.kt
```

#### 4.2 Delete Duplicates
```bash
# Remove from app module
rm -rf app/src/main/java/com/momoterminal/presentation/screens/settings/

# Consolidate feature module
# Keep only: feature/settings/src/main/kotlin/...
# Delete: feature/settings/src/main/kotlin/com/momoterminal/feature/settings/SettingsViewModel.kt (top-level)
```

#### 4.3 Create Clean ViewModel
```kotlin
// feature/settings/src/main/kotlin/.../viewmodel/SettingsViewModel.kt

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getMerchantSettings: GetMerchantSettingsUseCase,
    private val updateProfile: UpdateMerchantProfileUseCase,
    private val updateNotificationPrefs: UpdateNotificationPreferencesUseCase,
    private val updateTransactionLimits: UpdateTransactionLimitsUseCase,
    private val updateFeatureFlags: UpdateFeatureFlagsUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            
            sessionManager.currentUserId()?.let { userId ->
                getMerchantSettings(userId)
                    .onSuccess { settings ->
                        _uiState.value = SettingsUiState.Success(settings)
                    }
                    .onFailure { error ->
                        _uiState.value = SettingsUiState.Error(error.message ?: "Unknown error")
                    }
            } ?: run {
                _uiState.value = SettingsUiState.Error("User not logged in")
            }
        }
    }
    
    fun updateBusinessName(name: String) {
        viewModelScope.launch {
            val currentSettings = (_uiState.value as? SettingsUiState.Success)?.settings ?: return@launch
            val updatedProfile = currentSettings.profile.copy(businessName = name)
            
            updateProfile(updatedProfile)
                .onSuccess { loadSettings() }
                .onFailure { /* handle error */ }
        }
    }
    
    // Clean, focused methods for each update action
}

sealed interface SettingsUiState {
    object Loading : SettingsUiState
    data class Success(val settings: MerchantSettings) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}
```

---

### Phase 5: UI Layer (1.5 hours)

#### 5.1 Create Tab-Based Settings UI
```kotlin
// feature/settings/src/main/kotlin/.../ui/SettingsScreen.kt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is SettingsUiState.Loading -> LoadingIndicator()
            is SettingsUiState.Error -> ErrorView(state.message)
            is SettingsUiState.Success -> {
                SettingsContent(
                    settings = state.settings,
                    onUpdateProfile = viewModel::updateBusinessName,
                    onUpdateNotifications = viewModel::updateNotifications,
                    onUpdateLimits = viewModel::updateLimits,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    settings: MerchantSettings,
    onUpdateProfile: (String) -> Unit,
    onUpdateNotifications: (NotificationPreferences) -> Unit,
    onUpdateLimits: (TransactionLimits) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Profile", "Notifications", "Limits", "Providers", "Features")
    
    Column(modifier = modifier) {
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        when (selectedTab) {
            0 -> ProfileSettingsTab(settings.profile, settings.businessDetails, onUpdateProfile)
            1 -> NotificationSettingsTab(settings.notificationPrefs, onUpdateNotifications)
            2 -> TransactionLimitsTab(settings.transactionLimits, onUpdateLimits)
            3 -> PaymentProvidersTab(settings.paymentProviders)
            4 -> FeatureFlagsTab(settings.featureFlags)
        }
    }
}
```

---

## 📋 Implementation Checklist

### Backend Tasks
- [ ] Create normalized database schema migration
- [ ] Write data migration script for existing records
- [ ] Create database indexes for performance
- [ ] Implement Row Level Security policies
- [ ] Create helper functions (`get_merchant_settings`, etc.)
- [ ] Test migration on local Supabase
- [ ] Deploy to production Supabase

### Domain Layer Tasks
- [ ] Create domain models in `core/domain/model/settings/`
- [ ] Define use case interfaces
- [ ] Implement use cases with business logic
- [ ] Add validation rules
- [ ] Write unit tests for domain logic

### Data Layer Tasks
- [ ] Create repository interface in `core/domain`
- [ ] Implement repository in `core/data`
- [ ] Create Supabase remote data source
- [ ] Create Room local data source
- [ ] Add settings cache layer
- [ ] Implement offline-first sync
- [ ] Write repository tests

### Feature Module Tasks
- [ ] Delete duplicate files from `app/presentation/screens/settings/`
- [ ] Consolidate feature module structure
- [ ] Create clean ViewModel with proper DI
- [ ] Build tab-based UI with Compose
- [ ] Create reusable settings components
- [ ] Add navigation
- [ ] Write UI tests

### Integration Tasks
- [ ] Update app navigation to use new settings screen
- [ ] Remove old settings references
- [ ] Test end-to-end flow
- [ ] Verify offline mode works
- [ ] Test with real Supabase backend
- [ ] Performance testing
- [ ] Update documentation

---

## 🎯 Success Criteria

### Architecture
- ✅ Single source of truth for settings
- ✅ Clear separation of concerns (Domain → Data → UI)
- ✅ No duplicate ViewModels or Screens
- ✅ Proper dependency injection
- ✅ Testable at every layer

### Backend
- ✅ Normalized database schema (< 10 columns per table)
- ✅ Fast queries with proper indexes
- ✅ RLS policies for security
- ✅ Database functions for complex operations

### Frontend
- ✅ Intuitive tab-based UI
- ✅ Real-time updates via Flow
- ✅ Offline-first with sync
- ✅ Validation and error handling
- ✅ Loading states and optimistic updates

### Performance
- ✅ Settings load < 500ms
- ✅ Updates < 200ms (optimistic)
- ✅ Smooth scrolling (60fps)
- ✅ Low memory footprint

---

## 📊 Effort Estimation

| Phase | Tasks | Time | Complexity |
|-------|-------|------|------------|
| Backend Cleanup | Schema, migrations, functions | 2h | Medium |
| Domain Layer | Models, use cases | 1.5h | Low |
| Data Layer | Repository, data sources | 2h | Medium |
| Feature Cleanup | Consolidate, delete dupes | 1.5h | Low |
| UI Layer | New screens, components | 1.5h | Medium |
| **TOTAL** | | **8.5h** | **Medium** |

---

## 🚨 Risks & Mitigation

### Risk 1: Data Migration Failure
**Mitigation**: 
- Test migration on local Supabase first
- Create rollback script
- Backup production data before migration

### Risk 2: Breaking Existing Functionality
**Mitigation**:
- Feature flag for new settings UI
- Keep old code until new code is verified
- Comprehensive testing

### Risk 3: Offline Sync Issues
**Mitigation**:
- Implement conflict resolution strategy
- Use last-write-wins with timestamps
- Show sync status to user

---

## 📈 Benefits

### Code Quality
- **Before**: 5 duplicate files, 1,183 lines, scattered logic
- **After**: Clean architecture, ~800 lines total, single source of truth

### Maintainability
- **Before**: Changes needed in 5 places
- **After**: Changes in 1 place, propagate automatically

### Performance
- **Before**: Loading all 50+ columns every time
- **After**: Load only needed data, lazy load details

### Testing
- **Before**: Hard to test, tightly coupled
- **After**: Each layer testable independently

---

## 🏁 Next Steps

1. **Review this plan** with team (15 min)
2. **Approve architecture** and database changes (30 min)
3. **Start Phase 1**: Backend cleanup (2h)
4. **Continue sequentially** through phases
5. **Test thoroughly** after each phase
6. **Deploy incrementally** with feature flags

---

**Estimated Completion**: 1-2 days of focused work  
**Priority**: High (blocks production-ready status)  
**Owner**: Backend + Frontend developer  
**Review**: Architecture team approval needed

---

**Created**: December 6, 2025  
**Status**: 🔴 **AWAITING APPROVAL TO START**
