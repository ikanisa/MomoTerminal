# MomoTerminal - Immediate Action Checklist
**Priority:** Get app running on device TODAY

---

## ✅ COMPLETED
- [x] Database schema designed (all 8 Supabase tables)
- [x] Edge functions created (6 total)
- [x] Device registration infrastructure
- [x] Analytics manager implemented
- [x] Error logger implemented
- [x] API endpoints updated (6 new endpoints)
- [x] Documentation created (2 comprehensive docs)

---

## 🔴 CRITICAL - DO NOW (Est: 1 hour)

### 1. Fix Compile Errors (10 min)
```bash
cd /Users/jeanbosco/workspace/MomoTerminal

# Check for compile errors
./gradlew compileDebugKotlin 2>&1 | grep -A 5 "error:"

# Most likely fix needed:
# File: app/src/main/java/com/momoterminal/presentation/screens/home/HomeScreen.kt
# Line 102: nfcState.isActive()
# Already works - isActive() extension exists in NfcState.kt
```

**If there's an error, options:**
- Option A: Verify import: `import com.momoterminal.nfc.isActive`
- Option B: Replace with: `nfcState is NfcState.Active || nfcState is NfcState.Processing`

### 2. Build APK (5 min)
```bash
./gradlew clean assembleDebug
```

### 3. Install on Device (2 min)
```bash
# Check device connected
adb devices

# Install
./gradlew installDebug

# Or manually
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. Quick Smoke Test (5 min)
- [ ] App launches without crash
- [ ] Can navigate to home screen
- [ ] Can navigate to settings
- [ ] Can navigate to terminal screen
- [ ] No obvious UI errors

---

## 🟡 HIGH PRIORITY - TODAY (Est: 6 hours)

### 5. Integrate Device Registration (30 min)

**File:** `app/src/main/java/com/momoterminal/auth/AuthViewModel.kt`

**Add:**
```kotlin
@Inject lateinit var deviceRepository: DeviceRepository

// In verifyOtp() after successful verification:
viewModelScope.launch {
    val deviceResult = deviceRepository.registerDevice()
    if (deviceResult.isFailure) {
        Timber.w("Device registration failed, will retry later")
    }
}
```

### 6. Add Analytics to Key Screens (2 hours)

**File:** `app/src/main/java/com/momoterminal/presentation/ComposeMainActivity.kt`

**Add:**
```kotlin
@Inject lateinit var analyticsManager: AnalyticsManager

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    analyticsManager.startSession()
}

override fun onPause() {
    super.onPause()
    if (isFinishing) {
        analyticsManager.endSession()
    }
}
```

**File:** Navigation setup (wherever NavHost is created)

**Add:**
```kotlin
navController.addOnDestinationChangedListener { _, destination, _ ->
    destination.route?.let { route ->
        analyticsManager.logScreenView(route)
    }
}
```

### 7. Add Error Logging (2 hours)

**File:** `app/src/main/java/com/momoterminal/error/AppErrorBoundary.kt`

**Add ErrorLogger injection and usage:**
```kotlin
@Inject lateinit var errorLogger: ErrorLogger

// In error handler:
errorLogger.logException(
    exception = throwable,
    severity = ErrorSeverity.HIGH,
    context = mapOf("screen" to "current_screen")
)
```

### 8. Test Core Flows (1.5 hours)
- [ ] Register new user (WhatsApp OTP)
- [ ] Device auto-registers
- [ ] Complete profile
- [ ] Receive test SMS (or use demo)
- [ ] View transaction
- [ ] Check logs in Supabase

---

## 🟢 MEDIUM - THIS WEEK (Est: 8 hours)

### 9. Create Missing Edge Functions (2 hours)

```bash
cd supabase/functions

# Copy template from register-device
cp -r register-device upload-analytics
cp -r register-device upload-errors  
cp -r register-device get-merchant-settings

# Edit each index.ts file with appropriate logic
```

### 10. Merchant Settings Sync (2 hours)

Create `MerchantSettingsRepository.kt` similar to `DeviceRepository.kt`.

### 11. Deploy All Backend (1 hour)

```bash
cd supabase

# Deploy migrations (if any updates)
supabase db push

# Deploy all functions
supabase functions deploy register-device
supabase functions deploy send-whatsapp-otp
supabase functions deploy verify-whatsapp-otp
supabase functions deploy sync-transactions
supabase functions deploy webhook-relay
supabase functions deploy complete-user-profile
supabase functions deploy upload-analytics  # NEW
supabase functions deploy upload-errors     # NEW
supabase functions deploy get-merchant-settings # NEW

# Set secrets
supabase secrets set TWILIO_ACCOUNT_SID=your_sid
supabase secrets set TWILIO_AUTH_TOKEN=your_token
supabase secrets set TWILIO_WHATSAPP_NUMBER=whatsapp:+14155238886
```

### 12. Comprehensive Testing (3 hours)
- See `COMPREHENSIVE_IMPLEMENTATION_PLAN.md` Section 5

---

## 📋 TESTING PRIORITIES

### Must Test Before Alpha
1. **Authentication Flow**
   - [x] OTP sent via WhatsApp
   - [ ] OTP verification
   - [ ] Device registration
   - [ ] Profile completion

2. **SMS Processing**
   - [ ] SMS received
   - [ ] SMS parsed correctly
   - [ ] Transaction saved locally
   - [ ] Transaction synced to cloud
   - [ ] Webhook relayed

3. **NFC (if hardware available)**
   - [ ] NFC tap initiated
   - [ ] USSD dial string sent
   - [ ] Payment confirmation SMS received
   - [ ] Full cycle completed

4. **Offline Mode**
   - [ ] Transactions queue while offline
   - [ ] Sync when back online
   - [ ] No data loss

5. **Multi-Device (if possible)**
   - [ ] Register second device
   - [ ] Both devices see synced transactions
   - [ ] No conflicts

---

## 🎯 SUCCESS CRITERIA

**End of Today:**
- [ ] App builds without errors
- [ ] Installs on device
- [ ] Core screens work
- [ ] Device registration integrated
- [ ] Analytics logging added

**End of Week:**
- [ ] All edge functions deployed
- [ ] End-to-end testing completed
- [ ] 5 alpha testers using app
- [ ] No critical bugs

**Production Ready:**
- [ ] All checklist items in `IMPLEMENTATION_STATUS_SUMMARY.md` completed
- [ ] 50+ transactions processed successfully
- [ ] Crash-free rate > 99%
- [ ] Play Store assets ready

---

## 📞 QUICK REFERENCE

### Key Files Created Today
1. `util/DeviceInfoProvider.kt` - Device info
2. `data/repository/DeviceRepository.kt` - Device registration  
3. `monitoring/AnalyticsManager.kt` - Event tracking
4. `monitoring/ErrorLogger.kt` - Error reporting
5. `data/remote/dto/DeviceDto.kt` - API models

### Key Files to Edit
1. `auth/AuthViewModel.kt` - Add device registration call
2. `presentation/ComposeMainActivity.kt` - Add analytics session
3. `error/AppErrorBoundary.kt` - Add error logger
4. `nfc/NfcManager.kt` - Add NFC event logging (optional today)

### Documentation
1. `COMPREHENSIVE_IMPLEMENTATION_PLAN.md` - Full spec (36KB)
2. `IMPLEMENTATION_STATUS_SUMMARY.md` - Current status (13KB)
3. `IMMEDIATE_ACTION_CHECKLIST.md` - This file (you are here)

### Commands
```bash
# Build
./gradlew clean assembleDebug

# Install
./gradlew installDebug

# Check device
adb devices

# View logs
adb logcat | grep MomoTerminal

# Deploy Supabase
cd supabase && supabase functions deploy <name>
```

---

## 🚀 LET'S GO!

**Start with:** Steps 1-4 (Build & Install) - 20 minutes  
**Then do:** Step 5 (Device Registration) - 30 minutes  
**First milestone:** Working app on your phone in 1 hour!

---

*Last Updated: December 1, 2025 18:50 UTC*
