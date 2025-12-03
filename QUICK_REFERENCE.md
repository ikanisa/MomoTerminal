# Quick Reference - Modular Super App

## 🚀 Getting Started

### 1. Sync Gradle
```bash
# In Android Studio: File → Sync Project with Gradle Files
# Or via command line:
./gradlew --refresh-dependencies
```

### 2. Build Everything
```bash
./gradlew build
```

### 3. Run App
```bash
./gradlew installDebug
# Or press Run in Android Studio
```

## 📦 Module Structure

```
11 Modules Total:
├── 7 Core Modules (reusable infrastructure)
└── 4 Feature Modules (business features)
```

### Core Modules
```
:core:common       → Utilities, Result wrapper
:core:designsystem → Material 3 theme
:core:ui           → BaseViewModel, UI base classes
:core:network      → API services, Supabase
:core:database     → Room database
:core:data         → Repositories
:core:domain       → Domain models, interfaces
```

### Feature Modules
```
:feature:payment      → NFC + USSD payment
:feature:auth         → Authentication
:feature:transactions → Transaction history
:feature:settings     → App settings
```

## 🔧 Common Commands

### Build Specific Module
```bash
./gradlew :core:common:build
./gradlew :feature:payment:build
```

### Clean Build
```bash
./gradlew clean build
```

### Run Tests
```bash
./gradlew test
./gradlew :core:common:test
```

### Assemble APK
```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## 📝 Package Structure

### Old → New Mapping

| Old Package | New Package |
|-------------|-------------|
| `com.momoterminal.util` | `com.momoterminal.core.common` |
| `com.momoterminal.designsystem` | `com.momoterminal.core.designsystem` |
| `com.momoterminal.api` | `com.momoterminal.core.network.api` |
| `com.momoterminal.supabase` | `com.momoterminal.core.network.supabase` |
| `com.momoterminal.data.local` | `com.momoterminal.core.database` |
| `com.momoterminal.data.repository` | `com.momoterminal.core.data.repository` |
| `com.momoterminal.domain` | `com.momoterminal.core.domain` |
| `com.momoterminal.nfc` | `com.momoterminal.feature.payment.nfc` |
| `com.momoterminal.ussd` | `com.momoterminal.feature.payment.ussd` |
| `com.momoterminal.auth` | `com.momoterminal.feature.auth` |

## 🎯 Adding a New Feature

### Step-by-Step

1. **Create module directory**
```bash
mkdir -p feature/myfeature/src/main/kotlin/com/momoterminal/feature/myfeature
```

2. **Copy build.gradle.kts**
```bash
cp feature/payment/build.gradle.kts feature/myfeature/
sed -i '' 's/payment/myfeature/g' feature/myfeature/build.gradle.kts
```

3. **Add to settings.gradle.kts**
```kotlin
include(":feature:myfeature")
```

4. **Create AndroidManifest.xml**
```bash
echo '<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />' > feature/myfeature/src/main/AndroidManifest.xml
```

5. **Create feature files**
```kotlin
// MyFeatureContract.kt
data class MyFeatureUiState(...) : UiState
sealed interface MyFeatureUiEvent : UiEvent
sealed interface MyFeatureUiEffect : UiEffect

// MyFeatureViewModel.kt
@HiltViewModel
class MyFeatureViewModel @Inject constructor() : 
    BaseViewModel<MyFeatureUiState, MyFeatureUiEvent, MyFeatureUiEffect>(...)

// MyFeatureScreen.kt
@Composable
fun MyFeatureScreen(viewModel: MyFeatureViewModel = hiltViewModel()) { ... }
```

6. **Add to app dependencies**
```kotlin
// app/build.gradle.kts
implementation(project(":feature:myfeature"))
```

## 🔍 Troubleshooting

### Import Errors
```bash
# Run import fix script
./fix_imports.sh
```

### Build Errors
```bash
# Clean and rebuild
./gradlew clean build --refresh-dependencies
```

### Gradle Sync Issues
```bash
# Invalidate caches in Android Studio
# File → Invalidate Caches / Restart
```

### Module Not Found
```bash
# Check settings.gradle.kts includes the module
# Check module has build.gradle.kts
# Check module has AndroidManifest.xml
```

## 📚 Key Files

### Configuration
- `settings.gradle.kts` - Module list
- `app/build.gradle.kts` - App dependencies
- `core/*/build.gradle.kts` - Core module configs
- `feature/*/build.gradle.kts` - Feature module configs

### Documentation
- `IMPLEMENTATION_COMPLETE.md` - Full implementation details
- `REFACTORING_SUMMARY.md` - Transformation overview
- `QUICK_REFERENCE.md` - This file

### Scripts
- `fix_imports.sh` - Fix package imports
- `verify_build.sh` - Verify all modules build

## 🎨 Code Patterns

### Using Result Wrapper
```kotlin
import com.momoterminal.core.common.Result

repository.getData()
    .collect { result ->
        when (result) {
            is Result.Loading -> updateState { copy(isLoading = true) }
            is Result.Success -> updateState { copy(data = result.data) }
            is Result.Error -> updateState { copy(error = result.exception.message) }
        }
    }
```

### Using BaseViewModel
```kotlin
import com.momoterminal.core.ui.BaseViewModel

@HiltViewModel
class MyViewModel @Inject constructor() : 
    BaseViewModel<MyUiState, MyUiEvent, MyUiEffect>(
        initialState = MyUiState()
    ) {
    
    override fun onEvent(event: MyUiEvent) {
        when (event) {
            is MyUiEvent.Load -> loadData()
        }
    }
    
    private fun loadData() {
        updateState { copy(isLoading = true) }
        // ... load data
    }
}
```

### Using Design System
```kotlin
import com.momoterminal.core.designsystem.component.*
import com.momoterminal.core.designsystem.theme.*

@Composable
fun MyScreen() {
    MomoButton(
        text = "Click Me",
        onClick = { }
    )
}
```

## 🔗 Dependencies

### Core Module Dependencies
```kotlin
// core:common - No dependencies
// core:ui - Depends on: common
// core:domain - Depends on: common
// core:network - No internal dependencies
// core:database - No internal dependencies
// core:data - Depends on: common, domain, network, database
// core:designsystem - No dependencies
```

### Feature Module Dependencies
```kotlin
// All feature modules depend on:
implementation(project(":core:common"))
implementation(project(":core:domain"))
implementation(project(":core:ui"))
implementation(project(":core:designsystem"))
```

## ⚡ Performance Tips

### Parallel Builds
```bash
# Already enabled in gradle.properties
org.gradle.parallel=true
```

### Build Cache
```bash
# Already enabled
org.gradle.caching=true
```

### Configuration Cache
```bash
./gradlew build --configuration-cache
```

## 📊 Module Sizes

```
Core Modules:     ~6,000 LOC
Feature Modules:  ~3,000 LOC
Total:            ~9,000 LOC
```

## ✅ Checklist

### Before Committing
- [ ] All modules build successfully
- [ ] No import errors
- [ ] Tests pass
- [ ] App runs without crashes
- [ ] No TODO/FIXME comments

### Before Release
- [ ] Update version in version.properties
- [ ] Run full test suite
- [ ] Build release APK
- [ ] Test on multiple devices
- [ ] Update CHANGELOG

---

**Quick Help**: If stuck, check `IMPLEMENTATION_COMPLETE.md` for detailed info.
