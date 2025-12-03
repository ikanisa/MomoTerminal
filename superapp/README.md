# Generic Super App - Android Architecture

A production-ready, domain-agnostic super app architecture built with modern Android development practices.

## 🏗️ Architecture Overview

This project implements a **multi-module, clean architecture** approach with:

- **Single Activity** architecture with Jetpack Compose
- **MVVM/MVI** pattern with unidirectional data flow
- **Offline-first** approach with Room database caching
- **Modular** structure for scalability and feature isolation
- **Dependency Injection** with Hilt
- **Reactive** programming with Kotlin Coroutines and Flow

## 📦 Module Structure

```
SuperApp/
├── app/                          # Main application module
│   ├── SuperAppApplication       # Application class with Hilt
│   ├── MainActivity              # Single activity host
│   └── navigation/               # App-level navigation
│
├── core/
│   ├── common/                   # Shared utilities, Result wrapper
│   ├── designsystem/             # Material 3 theme, components
│   ├── ui/                       # Base UI classes, BaseViewModel
│   ├── network/                  # Retrofit, API services, DTOs
│   ├── database/                 # Room database, DAOs, entities
│   ├── data/                     # Repository implementations
│   └── domain/                   # Use cases, domain models, repository interfaces
│
└── feature/
    ├── auth/                     # Authentication & user profile
    ├── featureA/                 # Generic feature module A
    ├── featureB/                 # Generic feature module B
    └── featureC/                 # Generic feature module C
```

## 🔄 Data Flow Architecture

```
UI Layer (Compose)
    ↓ Events
ViewModel (State Management)
    ↓ Invoke
Use Case (Business Logic)
    ↓ Request
Repository (Data Coordination)
    ↓ Fetch
Remote Data Source ←→ Local Data Source
    ↓                      ↓
  API                   Room DB
```

## 🎯 Key Design Patterns

### 1. Result Wrapper

All data operations return a sealed `Result` type:

```kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable, val message: String?) : Result<Nothing>
    data object Loading : Result<Nothing>
}
```

### 2. MVI Pattern (Model-View-Intent)

Each feature follows the MVI pattern:

```kotlin
// State - Represents UI state
data class FeatureAUiState(
    val entities: List<Entity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) : UiState

// Event - User interactions
sealed interface FeatureAUiEvent : UiEvent {
    data object LoadEntities : FeatureAUiEvent
    data class OnEntityClick(val id: String) : FeatureAUiEvent
}

// Effect - One-time side effects (navigation, toasts)
sealed interface FeatureAUiEffect : UiEffect {
    data class NavigateToDetail(val id: String) : FeatureAUiEffect
    data class ShowError(val message: String) : FeatureAUiEffect
}
```

### 3. Repository Pattern

Repositories coordinate between remote and local data sources:

```kotlin
class EntityRepositoryImpl(
    private val remoteDataSource: EntityRemoteDataSource,
    private val localDataSource: EntityLocalDataSource
) : EntityRepository {
    
    override fun getEntities(page: Int, pageSize: Int): Flow<Result<PaginatedResult<Entity>>> = flow {
        emit(Result.Loading)
        
        // Emit cached data first (offline-first)
        localDataSource.getEntities(pageSize, offset)
            .collect { cached -> emit(Result.Success(cached)) }
        
        // Fetch fresh data from API
        val remote = remoteDataSource.getEntities(page, pageSize)
        localDataSource.insertEntities(remote)
        emit(Result.Success(remote))
    }
}
```

### 4. Use Case Pattern

Use cases encapsulate business logic:

```kotlin
class GetEntitiesUseCase @Inject constructor(
    private val repository: EntityRepository
) : FlowUseCase<GetEntitiesParams, PaginatedResult<Entity>>() {
    
    override fun invoke(params: GetEntitiesParams): Flow<Result<PaginatedResult<Entity>>> {
        return repository.getEntities(params.page, params.pageSize)
    }
}
```

## 🛠️ Technology Stack

### Android
- **Language**: Kotlin 1.9+
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM/MVI + Clean Architecture
- **DI**: Hilt (Dagger)
- **Async**: Coroutines + Flow
- **Navigation**: Navigation Compose
- **Network**: Retrofit + OkHttp
- **Database**: Room
- **Preferences**: DataStore
- **Image Loading**: Coil

### Backend (Technology Agnostic)
- REST or GraphQL API
- JWT Authentication
- PostgreSQL/MongoDB
- Redis (Caching)
- S3-compatible storage

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK (API 24-35)

### Setup

1. **Clone the repository**
```bash
git clone <repository-url>
cd superapp
```

2. **Configure API endpoint**

Update the base URL in `core/network/src/main/kotlin/com/superapp/core/network/di/NetworkModule.kt`:

```kotlin
.baseUrl("https://your-api-endpoint.com/v1/")
```

3. **Build the project**
```bash
./gradlew build
```

4. **Run the app**
```bash
./gradlew installDebug
```

## 📱 Adding a New Feature Module

### 1. Create Module Structure

```bash
mkdir -p feature/featureX/src/main/kotlin/com/superapp/feature/featurex
```

### 2. Add to `settings.gradle.kts`

```kotlin
include(":feature:featureX")
```

### 3. Create Feature Files

```
feature/featureX/
├── FeatureXScreen.kt          # Compose UI
├── FeatureXViewModel.kt       # State management
├── FeatureXContract.kt        # UiState, UiEvent, UiEffect
└── navigation/
    └── FeatureXNavigation.kt  # Navigation setup
```

### 4. Define Contract

```kotlin
data class FeatureXUiState(
    val data: List<Item> = emptyList(),
    val isLoading: Boolean = false
) : UiState

sealed interface FeatureXUiEvent : UiEvent {
    data object Load : FeatureXUiEvent
}

sealed interface FeatureXUiEffect : UiEffect {
    data class ShowMessage(val text: String) : FeatureXUiEffect
}
```

### 5. Implement ViewModel

```kotlin
@HiltViewModel
class FeatureXViewModel @Inject constructor(
    private val useCase: YourUseCase
) : BaseViewModel<FeatureXUiState, FeatureXUiEvent, FeatureXUiEffect>(
    initialState = FeatureXUiState()
) {
    override fun onEvent(event: FeatureXUiEvent) {
        when (event) {
            is FeatureXUiEvent.Load -> loadData()
        }
    }
}
```

### 6. Create Compose Screen

```kotlin
@Composable
fun FeatureXScreen(
    viewModel: FeatureXViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    FeatureXContent(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}
```

### 7. Add to Navigation

Update `app/src/main/kotlin/com/superapp/navigation/AppNavHost.kt`:

```kotlin
composable("feature_x") {
    FeatureXScreen()
}
```

## 🧪 Testing Strategy

### Unit Tests
- ViewModels: Test state transitions and business logic
- Use Cases: Test business rules
- Repositories: Test data coordination logic

### Integration Tests
- Repository + Data Sources
- API + Database integration

### UI Tests
- Compose UI tests with `ComposeTestRule`
- Navigation flows

## 📊 Backend API Integration

The app expects a generic REST API. See [BACKEND_API_SPEC.md](BACKEND_API_SPEC.md) for complete API documentation.

### Key Endpoints

```
POST   /auth/login              # Authentication
GET    /users/me                # User profile
GET    /entities                # Paginated entity list
GET    /entities/{id}           # Single entity
POST   /entities                # Create entity
PUT    /entities/{id}           # Update entity
DELETE /entities/{id}           # Delete entity
```

### Example API Response

```json
{
  "data": [
    {
      "id": "ent_123",
      "type": "typeA",
      "title": "Sample Entity",
      "description": "Description",
      "metadata": {},
      "status": "active",
      "createdAt": "2025-12-03T18:47:27Z",
      "updatedAt": "2025-12-03T18:47:27Z"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalPages": 5,
    "totalItems": 100
  }
}
```

## 🔐 Security Best Practices

- Store tokens securely using EncryptedSharedPreferences or DataStore
- Use HTTPS for all API calls
- Implement certificate pinning for production
- Validate all user inputs
- Use ProGuard/R8 for code obfuscation in release builds

## 🌍 Internationalization (i18n)

Add string resources in `res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Super App</string>
    <string name="feature_a_title">Feature A</string>
</resources>
```

For additional languages, create `values-{language}/strings.xml`.

## 🎨 Theming

Material 3 theme is defined in `core/designsystem`. Customize colors, typography, and shapes:

```kotlin
@Composable
fun SuperAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

## 📈 Performance Optimization

- **Lazy Loading**: Use `LazyColumn` for lists
- **Image Caching**: Coil handles image caching automatically
- **Database Indexing**: Add indexes to frequently queried columns
- **Pagination**: Load data in chunks
- **Background Processing**: Use WorkManager for long-running tasks

## 🔄 Offline-First Strategy

1. **Read**: Always read from local database first
2. **Display**: Show cached data immediately
3. **Sync**: Fetch fresh data from API in background
4. **Update**: Update local cache with fresh data
5. **Notify**: Update UI with new data

## 📝 Code Style

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- Use meaningful variable names
- Keep functions small and focused
- Prefer immutability
- Use sealed classes for restricted hierarchies
- Document public APIs

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Write/update tests
4. Submit a pull request

## 📄 License

MIT License - See LICENSE file for details

## 🆘 Support

For issues and questions:
- Create an issue in the repository
- Check existing documentation
- Review the architecture guide

---

**Built with ❤️ using modern Android development practices**
