# MomoTerminal Release Checklist

## ✅ Completed

### Code & Build
- [x] Debug build compiles successfully
- [x] Release build compiles successfully (48MB APK)
- [x] All 395 unit tests passing
- [x] ProGuard/R8 minification working
- [x] Lint vital checks passing

### Security
- [x] WhatsApp OTP authentication implemented
- [x] Biometric authentication support
- [x] Encrypted storage (EncryptedSharedPreferences)
- [x] Certificate pinning configured
- [x] No hardcoded credentials
- [x] Backup rules configured (sensitive data excluded)

### Documentation
- [x] Privacy Policy (PRIVACY_POLICY.md)
- [x] Terms of Service (TERMS_OF_SERVICE.md)
- [x] Play Store listing content (PLAY_STORE_LISTING.md)
- [x] README with setup instructions

### Features
- [x] NFC HCE payment terminal
- [x] Multi-provider USSD generation
- [x] SMS transaction capture
- [x] Cloud sync with Supabase
- [x] Transaction analytics
- [x] Offline support

---

## 🔲 Pending

### Before Submission
- [ ] **Create signing keystore** for release APK
- [ ] **Sign release APK/AAB** with keystore
- [ ] **Create feature graphic** (1024x500 PNG)
- [ ] **Take screenshots** (phone + tablet if applicable)
- [ ] **Set up Play Console** account (if not done)

### Play Console Setup
- [ ] Create app listing
- [ ] Upload signed AAB/APK
- [ ] Fill store listing (copy from PLAY_STORE_LISTING.md)
- [ ] Upload feature graphic
- [ ] Upload screenshots
- [ ] Complete Data Safety form
- [ ] Set content rating
- [ ] Set pricing (Free)
- [ ] Select countries for distribution

### Post-Submission
- [ ] Monitor review status
- [ ] Respond to any policy issues
- [ ] Plan update roadmap

---

## Quick Commands

### Build Release APK
```bash
./gradlew assembleRelease
```

### Build Release Bundle (AAB) - Recommended for Play Store
```bash
./gradlew bundleRelease
```

### Run Tests
```bash
./gradlew testDebugUnitTest
```

### Create Signing Keystore
```bash
keytool -genkey -v -keystore momo-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias momo
```

### Sign APK (after creating keystore)
Add to `local.properties`:
```
MOMO_KEYSTORE_FILE=/path/to/momo-release.jks
MOMO_KEYSTORE_PASSWORD=your_password
MOMO_KEY_ALIAS=momo
MOMO_KEY_PASSWORD=your_key_password
```

---

## APK Location
- Unsigned: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Bundle: `app/build/outputs/bundle/release/app-release.aab`
