# Developer Quick Start Guide

## Project Structure

```
MomoTerminal/
├── app/                          # Main application module
├── core/                         # Core functionality (10 modules)
│   ├── common/                   # Shared utilities (Result, AppError, MVI)
│   ├── domain/                   # Business models and repository interfaces
│   ├── data/                     # Repository implementations
│   ├── database/                 # Room database and DataStore
│   ├── network/                  # Retrofit API client
│   ├── ui/                       # BaseViewModel and UI components
│   ├── designsystem/             # Material 3 theme
│   ├── os-integration/           # Notifications, deep links, widgets
│   ├── performance/              # Startup, offline, monitoring
│   └── i18n/                     # Localization and formatting
└── feature/                      # Feature modules (4 modules)
    ├── auth/                     # Authentication
    ├── payment/                  # Payment processing
    ├── transactions/             # Transaction history
    └── settings/                 # App settings
```

## Quick Commands

### Build
```bash
# List all modules
./gradlew projects

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :core:common:test

# Run instrumented tests
./gradlew connectedAndroidTest
```

### Code Quality
```bash
# Run lint
./gradlew lint

# Generate coverage report
./gradlew jacocoTestReport
```

## Adding a New Feature Module

### 1. Create Module Structure
```bash
mkdir -p feature/myfeature/src/main/kotlin/com/momoterminal/feature/myfeature/{ui,viewmodel,di}
```

### 2. Create build.gradle.kts
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.momoterminal.feature.myfeature"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    
    implementation(libs.coroutines.android)
}
```

### 3. Create AndroidManifest.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

### 4. Create ViewModel
```kotlin
package com.momoterminal.feature.myfeature.viewmodel

import com.momoterminal.core.common.UiEffect
import com.momoterminal.core.common.UiEvent
import com.momoterminal.core.common.UiState
import com.momoterminal.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyFeatureViewModel @Inject constructor() : 
    BaseViewModel<MyFeatureState, MyFeatureEvent, MyFeatureEffect>(MyFeatureState()) {
    
    override fun onEvent(event: MyFeatureEvent) {
        when (event) {
            is MyFeatureEvent.DoSomething -> doSomething()
        }
    }
    
    private fun doSomething() {
        updateState { copy(isLoading = true) }
        // Your logic here
    }
}

data class MyFeatureState(
    val isLoading: Boolean = false
) : UiState

sealed class MyFeatureEvent : UiEvent {
    data object DoSomething : MyFeatureEvent()
}

sealed class MyFeatureEffect : UiEffect
```

### 5. Create Screen
```kotlin
package com.momoterminal.feature.myfeature.ui

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.feature.myfeature.viewmodel.MyFeatureViewModel

@Composable
fun MyFeatureScreen(
    viewModel: MyFeatureViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    
    // Your UI here
}
```

### 6. Add to settings.gradle.kts
```kotlin
include(":feature:myfeature")
```

### 7. Add to app/build.gradle.kts
```kotlin
implementation(project(":feature:myfeature"))
```

### 8. Add to Navigation
```kotlin
// In app/src/main/java/com/momoterminal/navigation/AppNavigation.kt
composable(Screen.MyFeature.route) {
    MyFeatureScreen()
}
```

## MVI Pattern Usage

### State
```kotlin
data class MyState(
    val data: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) : UiState
```

### Events
```kotlin
sealed class MyEvent : UiEvent {
    data object Load : MyEvent()
    data class ItemClicked(val id: String) : MyEvent()
}
```

### Effects
```kotlin
sealed class MyEffect : UiEffect {
    data class ShowToast(val message: String) : MyEffect()
    data object NavigateBack : MyEffect()
}
```

### ViewModel
```kotlin
override fun onEvent(event: MyEvent) {
    when (event) {
        MyEvent.Load -> loadData()
        is MyEvent.ItemClicked -> handleClick(event.id)
    }
}

private fun loadData() {
    viewModelScope.launch {
        updateState { copy(isLoading = true) }
        repository.getData().collect { result ->
            when (result) {
                is Result.Success -> updateState { 
                    copy(data = result.data, isLoading = false) 
                }
                is Result.Error -> updateState { 
                    copy(error = result.exception.message, isLoading = false) 
                }
                Result.Loading -> updateState { copy(isLoading = true) }
            }
        }
    }
}
```

### Screen
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is MyEffect.ShowToast -> { /* Show toast */ }
                MyEffect.NavigateBack -> { /* Navigate */ }
            }
        }
    }
    
    when {
        state.isLoading -> LoadingState()
        state.error != null -> ErrorState(state.error!!) {
            viewModel.onEvent(MyEvent.Load)
        }
        else -> ContentView(state.data)
    }
}
```

## Repository Pattern

### Interface (in core:domain)
```kotlin
interface MyRepository {
    fun getData(): Flow<Result<List<Item>>>
    suspend fun saveData(item: Item): Result<Unit>
}
```

### Implementation (in core:data)
```kotlin
class MyRepositoryImpl @Inject constructor(
    private val api: MyApi,
    private val dao: MyDao
) : MyRepository, OfflineFirstRepository<List<Item>>() {
    
    override fun getData() = getData(forceRefresh = false)
    
    override suspend fun loadFromCache() = dao.getAll()
    
    override suspend fun fetchFromNetwork() = api.getData()
    
    override suspend fun saveToCache(data: List<Item>) {
        dao.insertAll(data)
    }
}
```

## Dependency Injection

### Module
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object MyModule {
    
    @Provides
    @Singleton
    fun provideMyRepository(
        api: MyApi,
        dao: MyDao
    ): MyRepository = MyRepositoryImpl(api, dao)
}
```

## Common Patterns

### Loading Data
```kotlin
viewModelScope.launch {
    repository.getData().collect { result ->
        when (result) {
            is Result.Success -> updateState { copy(data = result.data) }
            is Result.Error -> sendEffect(ShowError(result.exception.message))
            Result.Loading -> updateState { copy(isLoading = true) }
        }
    }
}
```

### Error Handling
```kotlin
try {
    val result = repository.doSomething()
    result.onSuccess { data ->
        updateState { copy(data = data) }
    }.onError { error ->
        sendEffect(ShowError(error.message))
    }
} catch (e: Exception) {
    sendEffect(ShowError(e.message ?: "Unknown error"))
}
```

### Navigation
```kotlin
// In ViewModel
sendEffect(NavigateToDetail(itemId))

// In Screen
LaunchedEffect(Unit) {
    viewModel.uiEffect.collect { effect ->
        when (effect) {
            is NavigateToDetail -> navController.navigate("detail/${effect.id}")
        }
    }
}
```

## Useful Resources

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Room](https://developer.android.com/training/data-storage/room)
- [Retrofit](https://square.github.io/retrofit/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

## Troubleshooting

### Build Fails
```bash
# Clean and rebuild
./gradlew clean build --refresh-dependencies

# Check for dependency conflicts
./gradlew :app:dependencies
```

### Module Not Found
```bash
# Sync Gradle
./gradlew --refresh-dependencies

# Check settings.gradle.kts includes the module
```

### Hilt Errors
```bash
# Rebuild project
./gradlew clean build

# Check @HiltAndroidApp is in Application class
# Check @AndroidEntryPoint is on Activities/Fragments
```

---

**Happy Coding! 🚀**
