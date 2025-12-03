# Refactoring MomoTerminal to Generic Super App Architecture

## Current State Analysis

MomoTerminal is a mobile money POS app with:
- NFC HCE payment terminal
- SMS relay for transaction notifications
- Supabase backend integration
- Multi-language support
- Offline-first architecture

## Refactoring Strategy

Transform the existing monolithic app into a **multi-module super app** that can support multiple feature domains beyond mobile money.

## Phase 1: Module Extraction (Week 1)

### 1.1 Create Core Modules

Extract existing code into core modules:

```
:core:common
├── Result.kt (NEW - wrap existing error handling)
├── Extensions.kt (MOVE from util/)
└── Constants.kt (CONSOLIDATE from config/)

:core:designsystem
├── theme/ (MOVE from designsystem/)
├── components/ (MOVE from designsystem/components/)
└── tokens/ (NEW - design tokens)

:core:ui
├── base/BaseViewModel.kt (REFACTOR existing ViewModels)
├── base/UiState.kt (NEW - standardize state)
└── components/ (MOVE shared UI components)

:core:network
├── api/ (MOVE from api/)
├── model/ (MOVE DTOs)
└── di/NetworkModule.kt (MOVE from di/)

:core:database
├── dao/ (MOVE from data/local/)
├── entity/ (MOVE database entities)
└── di/DatabaseModule.kt (NEW)

:core:data
├── repository/ (MOVE from data/repository/)
├── source/ (REFACTOR data sources)
└── mapper/ (NEW - DTO ↔ Domain mapping)

:core:domain
├── model/ (MOVE domain models)
├── repository/ (MOVE repository interfaces)
└── usecase/ (NEW - extract business logic)
```

### 1.2 Create Feature Modules

Transform existing features into isolated modules:

```
:feature:payment (Mobile Money POS)
├── nfc/ (MOVE from nfc/)
├── ussd/ (MOVE from ussd/)
├── PaymentScreen.kt
├── PaymentViewModel.kt
└── PaymentContract.kt

:feature:transactions (Transaction History)
├── list/
├── detail/
└── sync/

:feature:auth (Authentication)
├── login/ (MOVE from auth/)
├── register/
└── profile/

:feature:settings (App Settings)
├── merchant/
├── webhooks/
└── preferences/
```

## Phase 2: Architecture Refactoring (Week 2)

### 2.1 Implement Clean Architecture Layers

**Current Structure:**
```
app/
└── com/momoterminal/
    ├── ui/ (mixed presentation logic)
    ├── data/ (mixed data sources)
    ├── api/ (network layer)
    └── util/ (utilities)
```

**Target Structure:**
```
Presentation → Domain → Data
    ↓          ↓        ↓
  UI Layer  Use Cases  Repository
```

### 2.2 Standardize State Management

**Before:**
```kotlin
// Mixed state handling
class PaymentViewModel : ViewModel() {
    val amount = MutableLiveData<String>()
    val error = MutableLiveData<String?>()
    val loading = MutableLiveData<Boolean>()
}
```

**After:**
```kotlin
data class PaymentUiState(
    val amount: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) : UiState

sealed interface PaymentUiEvent : UiEvent {
    data class AmountChanged(val amount: String) : PaymentUiEvent
    data object ProcessPayment : PaymentUiEvent
}

class PaymentViewModel : BaseViewModel<PaymentUiState, PaymentUiEvent, PaymentUiEffect>()
```

### 2.3 Implement Result Wrapper

**Before:**
```kotlin
// Direct exception handling
try {
    val result = api.processPayment(request)
    _success.value = result
} catch (e: Exception) {
    _error.value = e.message
}
```

**After:**
```kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Nothing>
    data object Loading : Result<Nothing>
}

repository.processPayment(request)
    .collect { result ->
        when (result) {
            is Result.Loading -> updateState { copy(isLoading = true) }
            is Result.Success -> updateState { copy(data = result.data, isLoading = false) }
            is Result.Error -> updateState { copy(error = result.exception.message, isLoading = false) }
        }
    }
```

## Phase 3: Dependency Injection Refactoring (Week 2)

### 3.1 Modularize Hilt Modules

**Current:** All DI in `app/di/`

**Target:** Module-specific DI

```
:core:network/di/NetworkModule.kt
:core:database/di/DatabaseModule.kt
:core:data/di/DataModule.kt
:feature:payment/di/PaymentModule.kt
```

### 3.2 Create Base DI Structure

```kotlin
// core/common/di/CoreModule.kt
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun provideCoroutineDispatchers(): CoroutineDispatchers = CoroutineDispatchers()
}

// core/network/di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = ...
}
```

## Phase 4: Feature Module Implementation (Week 3)

### 4.1 Payment Feature Module

```
:feature:payment/
├── src/main/kotlin/com/superapp/feature/payment/
│   ├── PaymentScreen.kt
│   ├── PaymentViewModel.kt
│   ├── PaymentContract.kt
│   ├── nfc/
│   │   ├── NfcPaymentHandler.kt
│   │   └── NfcHceService.kt
│   ├── ussd/
│   │   └── UssdGenerator.kt
│   └── di/
│       └── PaymentModule.kt
└── build.gradle.kts
```

**Dependencies:**
```kotlin
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
}
```

### 4.2 Transaction Feature Module

```
:feature:transactions/
├── list/
│   ├── TransactionListScreen.kt
│   └── TransactionListViewModel.kt
├── detail/
│   ├── TransactionDetailScreen.kt
│   └── TransactionDetailViewModel.kt
└── sync/
    └── TransactionSyncWorker.kt
```

## Phase 5: Navigation Refactoring (Week 3)

### 5.1 Centralize Navigation

**Before:** Navigation scattered across activities

**After:** Single-activity with Navigation Compose

```kotlin
// app/navigation/AppNavHost.kt
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Feature: Payment
        composable(Screen.Payment.route) {
            PaymentScreen(
                onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) }
            )
        }
        
        // Feature: Transactions
        composable(Screen.Transactions.route) {
            TransactionListScreen(
                onNavigateToDetail = { id -> navController.navigate(Screen.TransactionDetail.createRoute(id)) }
            )
        }
    }
}
```

## Phase 6: Backend Integration Refactoring (Week 4)

### 6.1 Generic API Structure

**Before:** Supabase-specific implementation

**After:** Generic repository pattern with Supabase as implementation

```kotlin
// core/domain/repository/EntityRepository.kt
interface EntityRepository {
    fun getEntities(page: Int): Flow<Result<PaginatedResult<Entity>>>
}

// core/data/repository/SupabaseEntityRepository.kt
class SupabaseEntityRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : EntityRepository {
    override fun getEntities(page: Int): Flow<Result<PaginatedResult<Entity>>> = flow {
        // Implementation
    }
}
```

### 6.2 Abstract Data Sources

```kotlin
// core/data/source/RemoteDataSource.kt
interface RemoteDataSource<T> {
    suspend fun fetch(query: Query): List<T>
    suspend fun create(item: T): T
    suspend fun update(id: String, item: T): T
    suspend fun delete(id: String)
}

// core/data/source/SupabaseRemoteDataSource.kt
class SupabaseRemoteDataSource<T> : RemoteDataSource<T> {
    // Supabase-specific implementation
}
```

## Phase 7: Testing Infrastructure (Week 4)

### 7.1 Add Test Modules

```
:core:testing/
├── FakeRepository.kt
├── TestDispatchers.kt
└── TestData.kt
```

### 7.2 Unit Tests for Each Module

```kotlin
// feature/payment/src/test/
class PaymentViewModelTest {
    @Test
    fun `when amount entered, state updates correctly`() = runTest {
        // Given
        val viewModel = PaymentViewModel(fakeRepository)
        
        // When
        viewModel.onEvent(PaymentUiEvent.AmountChanged("100"))
        
        // Then
        assertEquals("100", viewModel.uiState.value.amount)
    }
}
```

## Migration Checklist

### Week 1: Core Modules
- [ ] Create `:core:common` module
- [ ] Create `:core:designsystem` module
- [ ] Create `:core:ui` module
- [ ] Create `:core:network` module
- [ ] Create `:core:database` module
- [ ] Create `:core:data` module
- [ ] Create `:core:domain` module
- [ ] Move existing code to core modules
- [ ] Update dependencies

### Week 2: Architecture
- [ ] Implement `Result` wrapper
- [ ] Create `BaseViewModel`
- [ ] Standardize `UiState/UiEvent/UiEffect`
- [ ] Refactor existing ViewModels
- [ ] Implement use cases
- [ ] Refactor repositories
- [ ] Update DI modules

### Week 3: Feature Modules
- [ ] Create `:feature:payment` module
- [ ] Create `:feature:transactions` module
- [ ] Create `:feature:auth` module
- [ ] Create `:feature:settings` module
- [ ] Migrate payment logic
- [ ] Migrate transaction logic
- [ ] Implement navigation
- [ ] Test feature isolation

### Week 4: Backend & Testing
- [ ] Abstract Supabase implementation
- [ ] Create generic repository interfaces
- [ ] Implement data source abstraction
- [ ] Add unit tests
- [ ] Add integration tests
- [ ] Update documentation
- [ ] Performance testing

## Benefits of Refactoring

1. **Modularity**: Easy to add/remove features
2. **Testability**: Isolated modules are easier to test
3. **Scalability**: Can grow to support multiple business domains
4. **Maintainability**: Clear separation of concerns
5. **Reusability**: Core modules can be reused across products
6. **Team Collaboration**: Teams can work on separate modules
7. **Build Performance**: Parallel module compilation

## Backward Compatibility

During migration:
- Keep existing functionality working
- Migrate incrementally (feature by feature)
- Run both old and new code paths in parallel
- Use feature flags for gradual rollout

## Post-Refactoring Structure

```
SuperApp/
├── app/ (Main container)
├── core/
│   ├── common/
│   ├── designsystem/
│   ├── ui/
│   ├── network/
│   ├── database/
│   ├── data/
│   └── domain/
├── feature/
│   ├── payment/ (Mobile Money POS)
│   ├── transactions/
│   ├── auth/
│   └── settings/
└── buildSrc/ (Dependency management)
```

## Next Steps

1. Review this plan with the team
2. Set up feature branches for each phase
3. Start with Phase 1: Core module extraction
4. Migrate one feature at a time
5. Maintain test coverage throughout
6. Update documentation as you go

---

**Estimated Timeline**: 4 weeks
**Risk Level**: Medium (incremental migration reduces risk)
**Team Size**: 2-3 developers recommended
