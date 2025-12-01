# Provider Enum Migration - Remaining Work

**Status:** ⚠️ In Progress  
**Issue:** Some files still reference old Provider enum names

---

## Files That Need Updating

### 1. ProviderSelector.kt
**Location:** `app/src/main/java/com/momoterminal/presentation/components/terminal/ProviderSelector.kt`

**Issues:**
- References old `NfcPaymentData.Provider` instead of `domain.model.Provider`
- Needs import statement update
- Provider enum values need to be updated to new names

**Fix:**
```kotlin
// Add import
import com.momoterminal.domain.model.Provider

// Update provider list to use:
Provider.MTN_GHANA
Provider.VODAFONE_GHANA  
Provider.AIRTELTIGO_GHANA
```

### 2. TerminalScreen.kt
**Location:** `app/src/main/java/com/momoterminal/presentation/screens/terminal/TerminalScreen.kt`

**Issue:** Line 120 references old Provider

**Fix:**
```kotlin
import com.momoterminal.domain.model.Provider
```

### 3. TerminalViewModel.kt
**Location:** `app/src/main/java/com/momoterminal/presentation/screens/terminal/TerminalViewModel.kt`

**Issue:** Lines 36 reference old Provider

**Fix:**
```kotlin
import com.momoterminal.domain.model.Provider

// Update provider references
```

### 4. TransactionDetailScreen.kt
**Location:** `app/src/main/java/com/momoterminal/presentation/screens/transaction/TransactionDetailScreen.kt`

**Issue:** Line 261 - when expression needs all new Provider cases

**Fix:**
```kotlin
when (provider) {
    Provider.MTN_GHANA, Provider.MTN_EAST_AFRICA -> // MTN
    Provider.VODAFONE_GHANA -> // Vodafone
    Provider.AIRTELTIGO_GHANA -> // AirtelTigo
    Provider.AIRTEL_EAST_AFRICA -> // Airtel
    Provider.TIGO -> // Tigo
    Provider.VODACOM -> // Vodacom
    Provider.HALOTEL -> // Halotel
    Provider.LUMICASH -> // Lumicash
    Provider.ECOCASH -> // EcoCash
}
```

### 5. WebhookFormViewModel.kt  
**Location:** `app/src/main/java/com/momoterminal/presentation/screens/webhooks/WebhookFormViewModel.kt`

**Issues:** Lines 54-62 - Unresolved references to webhook properties

**This might be a separate issue** - check if WebhookRepository has getWebhookById method

---

## Quick Fix Strategy

### Option 1: Global Find & Replace
```bash
# In Android Studio / IntelliJ
1. Cmd+Shift+R (Replace in Files)
2. Find: "NfcPaymentData.Provider"
   Replace: "Provider"
3. Find: "import com.momoterminal.nfc.NfcPaymentData.Provider"
   Replace: "import com.momoterminal.domain.model.Provider"
```

### Option 2: Manual Fix (Recommended for accuracy)
1. Open each file listed above
2. Add import: `import com.momoterminal.domain.model.Provider`
3. Update Provider enum values to new names
4. Handle when expressions with all cases

---

## Testing After Fix

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Priority

This migration is **not critical** for Play Store submission but should be completed before beta testing to ensure app stability.

**Estimated Time:** 30-45 minutes

---

_Created: December 1, 2025_
