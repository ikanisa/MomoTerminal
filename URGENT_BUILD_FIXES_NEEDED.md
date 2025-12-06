# 🚨 URGENT: Critical Build Fixes Required

**Date**: December 6, 2025  
**Status**: ❌ **BUILD FAILING**  
**Priority**: P0 - BLOCKER  
**Estimated Fix Time**: 4-8 hours

---

## Executive Summary

The MomoTerminal app currently has **4 critical compilation errors** preventing build, testing, and deployment. All other work must be paused until these are resolved.

**Current Build Status**: ❌ FAILED  
**Last Successful Build**: Unknown  
**Blocking Issues**: 4 critical bugs

---

## Critical Issues to Fix

### 🔴 Issue #1: Circular Module Dependency

**Error**:
```
Circular dependency between the following tasks:
:feature:nfc:bundleLibCompileToJarDebug
 └── :feature:payment:bundleLibCompileToJarDebug
      └── :feature:nfc:bundleLibCompileToJarDebug (*)
```

**Root Cause**:
- `feature:nfc/build.gradle.kts` line 51: `implementation(project(":feature:payment"))`
- `feature:payment/build.gradle.kts` line 36: `implementation(project(":feature:nfc"))`
- Both modules depend on each other → circular dependency

**Fix Steps**:

1. **Move `NfcPaymentData` to `core:domain`**:
   ```bash
   # File is already copied to core/domain in uncommitted changes
   # Just need to update package name
   ```

2. **Update package in `core/domain/.../NfcPaymentData.kt`**:
   ```kotlin
   package com.momoterminal.core.domain.model
   ```

3. **Create type alias in `feature/nfc/NfcPaymentData.kt`**:
   ```kotlin
   package com.momoterminal.feature.nfc
   
   // Re-export from core.domain for backward compatibility
   typealias NfcPaymentData = com.momoterminal.core.domain.model.NfcPaymentData
   ```

4. **Update import in `feature/payment/ussd/UssdHelper.kt`**:
   ```kotlin
   import com.momoterminal.core.domain.model.NfcPaymentData
   ```

5. **Remove circular dependency from `feature/payment/build.gradle.kts`**:
   ```kotlin
   dependencies {
       implementation(project(":core:common"))
       implementation(project(":core:domain"))
       implementation(project(":core:ui"))
       implementation(project(":core:data"))
       // REMOVE: implementation(project(":feature:nfc"))
       implementation(project(":core:database"))
       ...
   }
   ```

6. **Remove circular dependency from `feature/nfc/build.gradle.kts`**:
   ```kotlin
   dependencies {
       ...
       implementation(project(":core:designsystem"))
       // REMOVE: implementation(project(":feature:payment"))
       implementation(libs.coroutines.android)
       ...
   }
   ```

**Verification**:
```bash
./gradlew clean build
```

---

### 🔴 Issue #2: Missing AppConfig Import

**Error**:
```
e: file:///feature/nfc/src/main/java/.../NfcManager.kt:125:25 
Unresolved reference 'AppConfig'.
```

**Root Cause**:
- `AppConfig` class exists in `core:common` module
- Missing import statement in `NfcManager.kt`

**Fix**:

1. **Add import to `feature/nfc/src/main/java/.../NfcManager.kt`**:
   ```kotlin
   package com.momoterminal.feature.nfc
   
   import android.content.Context
   import android.content.Intent
   import android.nfc.NfcAdapter
   import android.os.Handler
   import android.os.Looper
   import android.util.Log
   import com.momoterminal.core.common.config.AppConfig  // ← ADD THIS
   import dagger.hilt.android.qualifiers.ApplicationContext
   import kotlinx.coroutines.flow.MutableStateFlow
   ...
   ```

**Verification**:
```bash
./gradlew :feature:nfc:compileDebugKotlin
```

---

### 🔴 Issue #3: VendorSmsProcessor in Wrong Module

**Error**:
```
e: [ksp] InjectProcessingStep was unable to process 'vendorProcessor' 
because 'error.NonExistentClass' could not be resolved.
```

**Root Cause**:
- `VendorSmsProcessor` class is in `app` module: `app/src/main/java/com/momoterminal/sms/VendorSmsProcessor.kt`
- `SmsReceiver` in `feature:sms` module tries to inject it
- Feature modules cannot depend on app module (wrong dependency direction)

**Fix Option A (Quick)**: Move class to correct module

1. **Move file**:
   ```bash
   mkdir -p feature/sms/src/main/java/com/momoterminal/feature/sms/processor
   mv app/src/main/java/com/momoterminal/sms/VendorSmsProcessor.kt \
      feature/sms/src/main/java/com/momoterminal/feature/sms/processor/
   ```

2. **Update package in file**:
   ```kotlin
   package com.momoterminal.feature.sms.processor  // ← UPDATE
   ```

3. **Update import in `SmsReceiver.kt`**:
   ```kotlin
   import com.momoterminal.feature.sms.processor.VendorSmsProcessor
   ```

**Fix Option B (Better Architecture)**: Create interface in domain

1. **Create interface in `core:domain`**:
   ```kotlin
   package com.momoterminal.core.domain.sms
   
   interface SmsProcessor {
       fun processSms(sender: String, message: String): SmsProcessingResult
   }
   ```

2. **Implement in `feature:sms`**:
   ```kotlin
   @Singleton
   class VendorSmsProcessorImpl @Inject constructor(...) : SmsProcessor {
       override fun processSms(...) { ... }
   }
   ```

3. **Bind in Hilt module**:
   ```kotlin
   @Module
   @InstallIn(SingletonComponent::class)
   abstract class SmsModule {
       @Binds
       abstract fun bindSmsProcessor(impl: VendorSmsProcessorImpl): SmsProcessor
   }
   ```

**Recommended**: Use Option A for quick fix, refactor to Option B later.

**Verification**:
```bash
./gradlew :feature:sms:compileDebugKotlin
./gradlew :feature:sms:kspDebugKotlin
```

---

### 🔴 Issue #4: Hilt Code Generation Failures

**Error**:
```
e: [ksp] InjectProcessingStep was unable to process 'HomeViewModel(error.NonExistentClass,...)' 
```

**Root Cause**:
- Cascading failure from Issue #2 (NfcManager not compiling)
- Hilt cannot generate DI code when dependencies fail to compile

**Fix**:
1. Fix Issue #2 first
2. Clean build
3. Rebuild

**Verification**:
```bash
./gradlew clean
./gradlew :app:kspDebugKotlin
./gradlew assembleDebug
```

---

## Complete Fix Workflow

### Step 1: Apply All Fixes
```bash
# Fix Issue #1: Circular Dependency
# 1. NfcPaymentData is already in core/domain (from uncommitted changes)
# 2. Just need to finalize the changes

# Fix Issue #2: Add AppConfig import
# Edit: feature/nfc/src/main/java/com/momoterminal/feature/nfc/NfcManager.kt
# Add: import com.momoterminal.core.common.config.AppConfig

# Fix Issue #3: Move VendorSmsProcessor
mkdir -p feature/sms/src/main/java/com/momoterminal/feature/sms/processor
cp app/src/main/java/com/momoterminal/sms/VendorSmsProcessor.kt \
   feature/sms/src/main/java/com/momoterminal/feature/sms/processor/
# Update package name in file
# Update imports in SmsReceiver.kt
```

### Step 2: Clean Build
```bash
./gradlew clean
./gradlew assembleDebug
```

### Step 3: Verify Tests
```bash
./gradlew testDebugUnitTest
```

### Step 4: Commit Fixes
```bash
git add -A
git commit -m "fix: Resolve circular dependency and compilation errors

- Move NfcPaymentData to core:domain to break circular dependency
- Add AppConfig import to NfcManager
- Move VendorSmsProcessor to feature:sms module
- Verify Hilt code generation succeeds

Fixes: #1 #2 #3 #4"
```

---

## Expected Results After Fixes

### Build Output
```bash
BUILD SUCCESSFUL in 1m 23s
524 actionable tasks: 524 executed
```

### Test Output
```bash
> Task :app:testDebugUnitTest
368 tests completed, 368 passed

BUILD SUCCESSFUL
```

### APK Generated
```bash
app/build/outputs/apk/debug/app-debug.apk (~ 66MB)
```

---

## Dependencies

All fixes can be done independently except:
- **Issue #4** depends on **Issue #2** (cascading fix)
- Recommended order: 1 → 3 → 2 → 4 (verify)

---

## Resources

### Files to Modify
1. `core/domain/src/main/kotlin/.../model/NfcPaymentData.kt` (update package)
2. `feature/nfc/src/main/java/.../NfcPaymentData.kt` (create type alias)
3. `feature/nfc/src/main/java/.../NfcManager.kt` (add import)
4. `feature/nfc/build.gradle.kts` (remove payment dependency)
5. `feature/payment/build.gradle.kts` (remove nfc dependency)
6. `feature/payment/src/main/kotlin/.../ussd/UssdHelper.kt` (update import)
7. `app/src/main/java/.../sms/VendorSmsProcessor.kt` (move to feature:sms)
8. `feature/sms/src/main/java/.../receiver/SmsReceiver.kt` (update import)

### Reference Documentation
- [QA_UAT_REPORT.md](./QA_UAT_REPORT.md) - Full QA review
- [PLAY_STORE_READY.md](./PLAY_STORE_READY.md) - Launch checklist
- [PRE_PRODUCTION_CHECKLIST.md](./PRE_PRODUCTION_CHECKLIST.md)

---

## Developer Notes

**Why This Happened**:
- Feature modules (`feature:*`) should only depend on core modules (`core:*`)
- Feature modules should NEVER depend on each other
- Shared domain models belong in `core:domain`
- App module is the composition root - features cannot depend on it

**Best Practices Going Forward**:
1. Keep shared models in `core:domain`
2. Use dependency inversion (interfaces in domain, implementations in features)
3. Review module dependencies before adding `implementation(project(...))`
4. Run `./gradlew build` after module structure changes

---

**Created**: December 6, 2025, 15:25 UTC  
**Priority**: P0 - CRITICAL  
**Assignee**: Development Team  
**Estimated Fix Time**: 4-8 hours

---

## Quick Commands Reference

```bash
# Clean everything
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run specific module tests
./gradlew :feature:nfc:test
./gradlew :feature:payment:test

# Check dependencies
./gradlew :feature:nfc:dependencies
./gradlew :feature:payment:dependencies

# Build with full logs
./gradlew assembleDebug --stacktrace --info

# Verify no circular dependencies
./gradlew checkDependencies  # (if task exists)
```

---

**Next Steps After Build Fixes**:
1. ✅ Verify build succeeds
2. ✅ Run all unit tests
3. ✅ Generate debug APK
4. ✅ Install on device
5. ✅ Basic smoke test
6. → Proceed to full QA/UAT testing

---

**END OF URGENT FIX DOCUMENT**
