# Quick Fix Reference - User Guide

## What Was Fixed

### 1. 💾 MOMO Number Now Saves to Database
**Before:** Your MOMO number was saved locally but not synced to the cloud.
**After:** Every time you save settings, it syncs to both your device and Supabase database.

**To Use:**
1. Go to Settings
2. Enter your MOMO number
3. Tap "Save Configuration"
4. ✅ Data is now saved both locally and in the cloud

---

### 2. 🌍 Smarter Country Selection
**Before:** Overwhelming list of 50+ countries shown at once.
**After:** Only primary markets shown by default with option to expand.

**To Use:**
1. Go to Settings → Mobile Money Setup
2. Tap the country card
3. See primary markets (marked with ⭐)
4. Tap "Show All Countries" if needed

**Primary Markets:**
- 🇷🇼 Rwanda (MTN MoMo)
- 🇨🇩 DR Congo (Orange Money)
- 🇧🇮 Burundi (EcoCash)
- 🇹🇿 Tanzania (M-Pesa)
- 🇿🇲 Zambia (MTN MoMo)

---

### 3. 🤖 Smart Auto-Fill
**Before:** Had to manually enter everything.
**After:** Your WhatsApp number automatically fills as your MOMO number.

**What Auto-Fills:**
- ✅ WhatsApp number → MOMO number
- ✅ Profile country → MOMO country
- ✅ User can easily edit any pre-filled value

**To Use:**
1. Complete WhatsApp OTP login
2. Go to Settings
3. Your number is already there!
4. Edit if it's different from your MOMO number

---

### 4. ⚙️ Revoke Permissions
**Before:** Could only grant permissions, not revoke.
**After:** Full bidirectional permission control.

**To Use:**
1. Go to Settings → Permissions & Controls
2. Granted permissions show ✅ and ⚙️ icon
3. Tap ⚙️ icon to open system settings
4. Revoke permission from system settings

**Permissions You Can Manage:**
- 📱 SMS Access
- 📷 Camera
- 🔔 Notifications
- 📡 NFC
- 🔋 Battery Optimization

---

### 5. ⌨️ Better Keyboard Experience
**Before:** Had to manually dismiss keyboard.
**After:** Press "Done" to auto-save valid numbers.

**To Use:**
1. Enter MOMO number
2. Press "Done" on keyboard
3. If valid, settings auto-save
4. If invalid, shows error message

---

### 6. 🏷️ NFC Status
**What's Available:**
- NFC Manager (handles state)
- NFC Terminal toggle in Settings
- Payment broadcasting ready

**To Test:**
1. Go to Settings → Permissions & Controls
2. Enable NFC in system settings
3. Toggle "NFC Terminal Mode" ON
4. Go to Terminal screen
5. Activate NFC for a payment

**Note:** Requires physical NFC-enabled device to test tap-to-pay.

---

## Database Sync

Your settings now sync to Supabase:
- Country code
- MOMO country
- MOMO phone number
- MOMO code preference
- Biometric setting
- NFC terminal enabled
- Language preference

**Check Sync Status:**
```sql
-- In Supabase SQL Editor
SELECT 
  phone_number,
  momo_country_code,
  momo_phone,
  use_momo_code,
  nfc_terminal_enabled,
  language,
  updated_at
FROM user_profiles
WHERE phone_number = '+250XXXXXXXXX';
```

---

## Troubleshooting

### MOMO Number Not Saving?
1. Check internet connection
2. Verify you're logged in
3. Look for error messages
4. Check Supabase logs

### Country List Empty?
1. Check internet connection
2. Restart app
3. Clear app cache if needed

### Permissions Not Working?
1. Go to Settings → Apps → MomoTerminal
2. Check app permissions
3. Grant required permissions
4. Restart app

### NFC Not Activating?
1. Verify device has NFC hardware
2. Enable NFC in system settings
3. Toggle NFC Terminal Mode in app
4. Check AndroidManifest.xml has NFC permissions

---

## Testing Checklist

Before releasing to users:

- [ ] Save MOMO number → Check Supabase database
- [ ] Country picker shows primary markets first
- [ ] WhatsApp number auto-fills
- [ ] Edit and save MOMO number works
- [ ] Permission settings icon appears when granted
- [ ] Keyboard "Done" saves valid number
- [ ] Language selection persists
- [ ] NFC toggle works
- [ ] Settings sync to cloud
- [ ] Offline mode queues updates

---

## Next Steps

1. **Deploy Migration:**
   ```bash
   supabase db push
   ```

2. **Deploy Edge Function:**
   ```bash
   supabase functions deploy update-user-profile
   ```

3. **Build & Test:**
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Test on Real Device:**
   - Complete WhatsApp auth
   - Configure MOMO number
   - Test NFC toggle
   - Verify database sync

5. **Monitor Logs:**
   ```bash
   adb logcat | grep -i "momo\|nfc\|settings"
   ```

---

## Support

If issues persist:
1. Check logcat for errors
2. Verify Supabase connection
3. Test Edge Function directly
4. Review migration applied successfully

---

**Build Status:** ✅ SUCCESS
**Files Changed:** 6 modified, 2 new
**Tests Required:** Device with NFC capability
