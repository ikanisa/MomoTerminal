# Build Fixes Required - MomoTerminal

## Status: ❌ PROJECT CANNOT BUILD

**Generated:** 2025-12-06T17:54:35.034Z

## Critical Issues Summary

The MomoTerminal Android app has **3 critical blocking issues** that prevent compilation and installation:

1. ✗ **Circular Module Dependencies** (BLOCKING)
2. ✗ **KSP/Hilt Code Generation Failures** (BLOCKING)
3. ✗ **Missing Type Resolution in Multi-Module Setup** (BLOCKING)

---

## Issue 1: Circular Module Dependencies

### Problem
Feature modules have circular dependencies that Gradle cannot resolve:

```
feature:nfc → feature:payment → feature:nfc
```

### Evidence
```kotlin
// feature/nfc/build.gradle.kts:51
implementation(project(":feature:payment")) // For PaymentState

// feature/payment/build.gradle.kts:36
implementation(project(":feature:nfc"))
```

### Impact
```
BUILD FAILED: Circular dependency between the following tasks:
:feature:nfc:bundleLibCompileToJarDebug
\--- :feature:nfc:transformDebugClassesWithAsm
     +--- :feature:payment:bundleLibRuntimeToJarDebug
     |    \--- :feature:payment:transformDebugClassesWithAsm
     |         +--- :feature:nfc:bundleLibCompileToJarDebug (*)
```

### Root Cause
- `NfcPaymentData` is defined in `feature:nfc` but needed in `feature:payment`
- `PaymentState` is referenced from `feature:nfc` → `feature:payment`
- Both modules try to use each other's classes

### ✅ Solution

**Step 1: Move shared types to core:domain**

The type already exists in the right place:
```
core/domain/src/main/kotlin/com/momoterminal/core/domain/model/NfcPaymentData.kt ✓
```

**Step 2: Remove circular dependencies**

```kotlin
// feature/nfc/build.gradle.kts
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))  // ← Use this for NfcPaymentData
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    // REMOVE: implementation(project(":feature:payment"))
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.runtime)
    implementation(libs.coroutines.android)
    implementation(libs.gson)
}
```

```kotlin
// feature/payment/build.gradle.kts
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))  // ← Use this for NfcPaymentData
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    // REMOVE: implementation(project(":feature:nfc"))
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.coroutines.android)
    implementation(libs.gson)
}
```

**Step 3: Update imports across codebase**

Replace all occurrences:
```bash
# Find all files using the wrong import
grep -r "import com.momoterminal.feature.nfc.NfcPaymentData" --include="*.kt" .

# Files to update:
# - feature/payment/src/main/kotlin/com/momoterminal/feature/payment/ussd/UssdHelper.kt
# - app/src/main/java/com/momoterminal/presentation/components/terminal/ProviderSelector.kt
# - app/src/main/java/com/momoterminal/ussd/UssdHelper.kt
# - feature/nfc/src/main/java/com/momoterminal/feature/nfc/NfcManager.kt
```

Change to:
```kotlin
import com.momoterminal.core.domain.model.NfcPaymentData
```

**Step 4: Handle PaymentState references**

In `feature/nfc/src/main/java/com/momoterminal/feature/nfc/NfcManager.kt`:

```kotlin
// CURRENT (BROKEN):
com.momoterminal.feature.payment.nfc.PaymentState.setPaymentData(...)

// OPTION A: Move PaymentState to core:domain
// OPTION B: Remove this coupling entirely (RECOMMENDED)
// Comment out for now:
// TODO: Refactor PaymentState to avoid circular dependency
// com.momoterminal.feature.payment.nfc.PaymentState.setPaymentData(...)
```

---

## Issue 2: KSP/Hilt Code Generation Failures

### Problem
KSP (Kotlin Symbol Processing) fails to resolve dependencies during Hilt code generation in the app module.

### Evidence
```
e: [ksp] InjectProcessingStep was unable to process 'HomeViewModel(error.NonExistentClass,...)'
because 'error.NonExistentClass' could not be resolved.

Dependency trace:
    => element (CLASS): com.momoterminal.presentation.screens.home.HomeViewModel
    => element (CONSTRUCTOR): HomeViewModel(error.NonExistentClass,...)
    => type (ERROR parameter type): error.NonExistentClass
```

### Affected Files
1. `app/src/main/java/com/momoterminal/presentation/screens/home/HomeViewModel.kt`
   - Cannot resolve `NfcManager` from `feature:nfc`
   
2. `app/src/main/java/com/momoterminal/presentation/screens/nfc/NfcTerminalViewModel.kt`
   - Cannot resolve `NfcManager` from `feature:nfc`
   
3. `app/src/main/java/com/momoterminal/sms/VendorSmsProcessor.kt`
   - Cannot resolve `SupabaseClient`

### Root Cause
KSP runs before feature modules complete compilation due to:
1. Gradle task ordering issues
2. Configuration cache inconsistencies
3. The circular dependencies causing unpredictable build order

### ✅ Solution

**Step 1: Fix task ordering in app/build.gradle.kts**

Add at the end of the file:
```kotlin
// Ensure feature modules build before KSP processes app module
afterEvaluate {
    tasks.matching { it.name.startsWith("ksp") }.configureEach {
        mustRunAfter(
            ":feature:nfc:compileDebugKotlin",
            ":feature:payment:compileDebugKotlin",
            ":feature:sms:compileDebugKotlin",
            ":feature:auth:compileDebugKotlin",
            ":feature:wallet:compileDebugKotlin",
            ":feature:settings:compileDebugKotlin",
            ":feature:transactions:compileDebugKotlin"
        )
    }
}
```

**Step 2: Verify Hilt annotations**

Ensure all @Inject constructors are in properly annotated classes:
```kotlin
// feature/nfc/src/main/java/com/momoterminal/feature/nfc/NfcManager.kt
@Singleton
class NfcManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ...
}
```

**Step 3: Clear all caches**

```bash
./gradlew --stop
rm -rf .gradle
rm -rf ~/.gradle/caches
rm -rf app/build
rm -rf feature/*/build
rm -rf core/*/build
./gradlew clean
```

---

## Issue 3: Missing Type Resolution

### Problem
Even after fixing imports, some files fail to compile because types aren't resolved.

### Evidence
```
e: feature/payment/.../UssdHelper.kt:6:43: Unresolved reference 'NfcPaymentData'
e: feature/payment/.../UssdHelper.kt:17:36: Unresolved reference 'NfcPaymentData'
```

### Root Cause
The `NfcPaymentData.kt` file in `feature/nfc` is just a type alias:
```kotlin
// feature/nfc/src/main/java/com/momoterminal/feature/nfc/NfcPaymentData.kt
package com.momoterminal.feature.nfc

// Re-export from core.domain for backward compatibility
typealias NfcPaymentData = com.momoterminal.core.domain.model.NfcPaymentData
```

This creates confusion when files import from `feature.nfc` package but the actual class is in `core.domain`.

### ✅ Solution

**Option A: Remove the type alias file** (RECOMMENDED)
```bash
rm feature/nfc/src/main/java/com/momoterminal/feature/nfc/NfcPaymentData.kt
```

This forces all code to use the correct import from `core.domain`.

**Option B: Keep type alias and update imports**

If keeping for backward compatibility, ensure ALL files import from the type alias location consistently (not recommended - creates confusion).

---

## Implementation Plan

### Phase 1: Remove Circular Dependencies (30 minutes)

1. **Update build.gradle.kts files**
   ```bash
   # Remove circular dependencies
   # Edit: feature/nfc/build.gradle.kts
   # Edit: feature/payment/build.gradle.kts
   ```

2. **Update imports across codebase**
   ```bash
   # Use sed or IDE refactoring
   find feature app -name "*.kt" -type f -exec sed -i '' \
     's/import com\.momoterminal\.feature\.nfc\.NfcPaymentData/import com.momoterminal.core.domain.model.NfcPaymentData/g' {} +
   ```

3. **Remove type alias file**
   ```bash
   rm feature/nfc/src/main/java/com/momoterminal/feature/nfc/NfcPaymentData.kt
   ```

4. **Comment out PaymentState references**
   ```kotlin
   // In NfcManager.kt, comment out cross-module state updates
   // TODO: Implement proper event-based communication
   ```

### Phase 2: Fix KSP Issues (20 minutes)

1. **Add task ordering to app/build.gradle.kts**
   - See solution above

2. **Clean all build artifacts**
   ```bash
   ./gradlew --stop
   rm -rf .gradle build */build
   ```

3. **Rebuild incrementally**
   ```bash
   ./gradlew :core:domain:build
   ./gradlew :feature:nfc:build
   ./gradlew :feature:payment:build
   ./gradlew :app:assembleDebug
   ```

### Phase 3: Handle Edge Cases (15 minutes)

1. **Fix app module imports**
   - Update `ProviderSelector.kt`
   - Update `UssdHelper.kt` in app module
   - Verify all ViewModels can inject dependencies

2. **Fix feature:sms module**
   - Update `SmsReceiver.kt` to use `android.util.Log` instead of `Timber`
   - Remove unused `VendorSmsProcessor` references if not essential

3. **Test build**
   ```bash
   ./gradlew clean assembleDebug
   ```

### Phase 4: Install and Test (10 minutes)

1. **Install on device**
   ```bash
   ./gradlew installDebug
   ```

2. **Launch app**
   ```bash
   adb shell am start -n com.momopay.terminal/.ui.splash.SplashActivity
   ```

3. **Verify core functionality**
   - App launches
   - Home screen loads
   - Navigation works
   - No crashes

---

## Quick Fix Commands

```bash
#!/bin/bash
# Quick fix script - run from project root

echo "🔧 Step 1: Stop Gradle daemon"
./gradlew --stop

echo "🔧 Step 2: Remove circular dependencies"
# Backup files
cp feature/nfc/build.gradle.kts feature/nfc/build.gradle.kts.bak
cp feature/payment/build.gradle.kts feature/payment/build.gradle.kts.bak

# Remove feature:payment from nfc
sed -i '' '/implementation(project(":feature:payment"))/d' feature/nfc/build.gradle.kts

# Remove feature:nfc from payment  
sed -i '' '/implementation(project(":feature:nfc"))/d' feature/payment/build.gradle.kts

echo "🔧 Step 3: Update imports"
find feature app -name "*.kt" -type f -exec sed -i '' \
  's/import com\.momoterminal\.feature\.nfc\.NfcPaymentData/import com.momoterminal.core.domain.model.NfcPaymentData/g' {} +

echo "🔧 Step 4: Remove type alias file"
rm -f feature/nfc/src/main/java/com/momoterminal/feature/nfc/NfcPaymentData.kt

echo "🔧 Step 5: Clean build"
rm -rf .gradle build */build

echo "🔧 Step 6: Build"
./gradlew clean assembleDebug

echo "🔧 Step 7: Install"
./gradlew installDebug

echo "🔧 Step 8: Launch"
adb shell am start -n com.momopay.terminal/.ui.splash.SplashActivity

echo "✅ Done! Check device for app."
```

---

## Alternative: Temporary Workarounds

If the above fixes don't work immediately, try these workarounds:

### Workaround 1: Disable Problematic ViewModels

Temporarily rename files to exclude them from compilation:
```bash
mv app/src/main/java/com/momoterminal/presentation/screens/home/HomeViewModel.kt \
   app/src/main/java/com/momoterminal/presentation/screens/home/HomeViewModel.kt.disabled
   
mv app/src/main/java/com/momoterminal/presentation/screens/nfc/NfcTerminalViewModel.kt \
   app/src/main/java/com/momoterminal/presentation/screens/nfc/NfcTerminalViewModel.kt.disabled
```

This will break those screens but allow the app to build.

### Workaround 2: Use an Older Commit

```bash
# Find last working build
git log --oneline --all | grep -i "build\|success\|working"

# Try commit before circular dependency was introduced
git checkout <commit-hash>
./gradlew clean assembleDebug
```

### Workaround 3: Build Without Dependency Verification

```bash
./gradlew assembleDebug --no-configuration-cache --rerun-tasks
```

---

## Prevention: Future Best Practices

### 1. Module Dependency Rules

**Allowed:**
```
app → feature:* → core:*
feature:a ✗ feature:b (NO cross-feature dependencies)
core:ui → core:domain
core:data → core:domain
```

**Forbidden:**
```
feature:nfc ✗ feature:payment
feature:* ✗ app
core:domain ✗ core:ui
```

### 2. Shared Types Location

| Type | Correct Location | Wrong Location |
|------|-----------------|----------------|
| Domain models | `core:domain/model/` | `feature:*/` |
| UI components | `core:ui/` | `app/` |
| Network DTOs | `core:network/dto/` | `core:domain/` |
| Database entities | `core:database/entity/` | `core:domain/` |

### 3. Dependency Injection Best Practices

```kotlin
// ✅ CORRECT: Feature provides implementation
@Module
@InstallIn(SingletonComponent::class)
object NfcModule {
    @Provides
    @Singleton
    fun provideNfcManager(
        @ApplicationContext context: Context
    ): NfcManager = NfcManager(context)
}

// ✅ CORRECT: App consumes via interface
class HomeViewModel @Inject constructor(
    private val nfcManager: NfcManager  // Resolved via Hilt
)

// ❌ WRONG: Direct cross-feature injection
class PaymentViewModel @Inject constructor(
    private val nfcManager: com.momoterminal.feature.nfc.NfcManager  // Creates dependency
)
```

### 4. Build Verification CI

Add to `.github/workflows/build.yml`:
```yaml
name: Build Check
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Grant execute permission
        run: chmod +x gradlew
      - name: Clean build
        run: ./gradlew clean
      - name: Check for circular dependencies
        run: ./gradlew buildEnvironment
      - name: Build debug APK
        run: ./gradlew assembleDebug
      - name: Run unit tests
        run: ./gradlew testDebugUnitTest
```

---

## Success Criteria

The build is fixed when:

- [  ] `./gradlew clean assembleDebug` completes successfully
- [  ] No circular dependency errors
- [  ] No KSP/Hilt processing errors
- [  ] APK is generated in `app/build/outputs/apk/debug/`
- [  ] `./gradlew installDebug` installs on connected device
- [  ] App launches without crashes
- [  ] No import errors in IDE (Android Studio)
- [  ] All feature modules build independently

---

## Estimated Time

| Phase | Time | Difficulty |
|-------|------|-----------|
| Phase 1: Remove circular deps | 30 min | Medium |
| Phase 2: Fix KSP issues | 20 min | Hard |
| Phase 3: Handle edge cases | 15 min | Medium |
| Phase 4: Install and test | 10 min | Easy |
| **Total** | **75 min** | **Medium-Hard** |

---

## Support

If issues persist after following this guide:

1. Check Gradle daemon: `./gradlew --status`
2. View full logs: `./gradlew assembleDebug --stacktrace --info`
3. Verify Kotlin version compatibility
4. Check Android Gradle Plugin version (currently 8.5.2, may need update)
5. Ensure Java 17 is being used

---

## Appendix: Current Dependency Graph

```
app
├── feature:nfc ❌ (circular)
│   ├── core:domain ✓
│   ├── core:data ✓
│   ├── core:ui ✓
│   └── feature:payment ❌ (CIRCULAR!)
├── feature:payment ❌ (circular)
│   ├── core:domain ✓
│   ├── core:data ✓
│   └── feature:nfc ❌ (CIRCULAR!)
├── feature:sms
│   └── core:database ✓
└── core:domain ✓

Legend:
✓ = Valid dependency
❌ = Invalid/circular dependency
```

## Appendix: Files to Modify

### Critical (Must Fix)
1. `feature/nfc/build.gradle.kts` - Remove feature:payment dependency
2. `feature/payment/build.gradle.kts` - Remove feature:nfc dependency
3. `feature/nfc/src/main/java/com/momoterminal/feature/nfc/NfcPaymentData.kt` - DELETE
4. `feature/payment/src/main/kotlin/com/momoterminal/feature/payment/ussd/UssdHelper.kt` - Update import
5. `app/src/main/java/com/momoterminal/presentation/components/terminal/ProviderSelector.kt` - Update import
6. `app/src/main/java/com/momoterminal/ussd/UssdHelper.kt` - Update import

### Important (Should Fix)
7. `feature/nfc/src/main/java/com/momoterminal/feature/nfc/NfcManager.kt` - Comment out PaymentState
8. `app/build.gradle.kts` - Add task ordering
9. `feature/sms/src/main/java/com/momoterminal/feature/sms/receiver/SmsReceiver.kt` - Already fixed (using Log)

### Optional (Nice to Have)
10. Add missing imports (AppConfig in NfcManager)
11. Clean up unused ViewModels if they cause issues
12. Update README with build instructions

---

**End of Report**
