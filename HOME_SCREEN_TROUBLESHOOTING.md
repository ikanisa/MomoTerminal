# Home Screen Display Error - Troubleshooting Guide

**Date:** December 1, 2025  
**Issue:** Display errors on home screen

---

## Quick Diagnostic Steps

### 1. Check App Logs
```bash
# Clear logs and restart app
adb logcat -c
adb shell am force-stop com.momoterminal
adb shell am start -n com.momoterminal/.MainActivity

# View errors
adb logcat | grep -i "momoterminal\|exception\|error"
```

### 2. Common Issues & Fixes

#### Issue A: Components Not Displaying
**Symptoms:** Blank areas, missing cards, layout issues

**Fix:**
```bash
# Rebuild and reinstall
cd /Users/jeanbosco/workspace/MomoTerminal
./gradlew clean
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Issue B: Text Overflow/Cutoff
**Symptoms:** Text is cut off or overlapping

**Quick Fix:** The HomeScreen uses proper Column with scroll, should work. If seeing this, check device font size (Settings → Display → Font size).

#### Issue C: Icons Not Showing
**Symptoms:** Missing NFC, Settings, or other icons

**Cause:** Material Icons not loading

**Fix:** Already included in dependencies, but verify:
```kotlin
// In build.gradle.kts
implementation("androidx.compose.material:material-icons-extended")
```

#### Issue D: Network Security Config Error
**Symptoms:** App crashes on startup

**Fix:** Already fixed in network_security_config.xml with proper certificate pins.

#### Issue E: Performance Monitor Warnings
**Symptoms:** Warnings in logcat about /proc/stat

**Status:** These are harmless warnings, not errors. Can be ignored or disabled:
```kotlin
// In PerformanceMonitor.kt - wrap CPU usage in try-catch
try {
    calculateCpuUsage()
} catch (e: Exception) {
    // Ignore on devices that don't allow /proc/stat access
}
```

---

## Home Screen Components

### Expected Layout:
```
┌─────────────────────────────────┐
│  MoMo Terminal        [Settings]│ <- Top Bar
├─────────────────────────────────┤
│  [NFC Status]    [Sync Badge]   │ <- Status Row
│                                  │
│  ⚠️ Please Configure            │ <- Warning (if not configured)
│                                  │
│  ┌───────────────────────────┐  │
│  │  📱 Start NFC Payment    │  │ <- Quick Action
│  │  Accept payments via NFC  │  │
│  └───────────────────────────┘  │
│                                  │
│  Recent Transactions [View All] │ <- Section Header
│                                  │
│  ┌───────────────────────────┐  │
│  │  MTN     GHS 50.00    ✓  │  │ <- Transaction Cards
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │  Vodafone GHS 25.00   ✓  │  │
│  └───────────────────────────┘  │
│                                  │
└─────────────────────────────────┘
```

---

## Specific Error Messages

### "No package ID 6a found"
**Status:** Android system warning, harmless  
**Action:** Ignore

### "PerformanceMonitor: Permission denied"
**Status:** Debug build trying to access system files  
**Action:** Ignore or disable in production build

### "Unresolved reference" in Compose
**Status:** Build error - shouldn't happen in installed app  
**Action:** If seeing UI issues, rebuild app

---

## Manual Testing Checklist

Open the app and verify:

- [ ] Top bar shows "MoMo Terminal" and Settings icon
- [ ] NFC status indicator visible (green/red/yellow)
- [ ] Sync badge shows if there are pending transactions
- [ ] Configuration warning appears if not set up
- [ ] "Start NFC Payment" card is visible and clickable
- [ ] "Recent Transactions" section shows
- [ ] Transaction cards display properly (if any exist)
- [ ] "View All" button navigates to transactions screen
- [ ] Scrolling works if content exceeds screen height

---

## Quick Fixes to Try

### Fix 1: Force Restart App
```bash
adb shell am force-stop com.momoterminal
adb shell am start -n com.momoterminal/.MainActivity
```

### Fix 2: Clear App Data
```bash
adb shell pm clear com.momoterminal
# Note: This deletes all data, will need to login again
```

### Fix 3: Reinstall Clean
```bash
adb uninstall com.momoterminal
cd /Users/jeanbosco/workspace/MomoTerminal
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Fix 4: Check Device Compatibility
```bash
# Get device info
adb shell getprop ro.build.version.sdk  # Should be >= 24
adb shell getprop ro.product.model
adb shell wm size  # Check screen resolution
```

---

## If Still Having Issues

Please provide:

1. **Screenshot** of the error
2. **Description** of what you expected vs. what you see
3. **Logcat output** after reproducing the issue:
   ```bash
   adb logcat -d > logcat.txt
   ```
4. **Device info:**
   ```bash
   adb shell getprop ro.product.model
   adb shell getprop ro.build.version.release
   ```

---

## Known Non-Issues

These are **expected** and **not errors**:

✅ PerformanceMonitor warnings in logcat  
✅ "No package ID 6a" warnings  
✅ Some system warnings about uid checks  

These are **actual issues** to fix:

❌ Blank screen on home tab  
❌ App crashes when navigating to home  
❌ Text overlapping or cut off  
❌ Missing icons or buttons  
❌ Layout completely broken  

---

**Created:** December 1, 2025  
**Status:** Ready for troubleshooting  
**Next:** User provides specific error details
