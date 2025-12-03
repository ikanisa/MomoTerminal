# OS Integration - Implementation Checklist

## ✅ Phase 1: Module Setup (COMPLETE)

- [x] Create `:core:os-integration` module
- [x] Add build.gradle.kts configuration
- [x] Add AndroidManifest.xml with permissions
- [x] Add to settings.gradle.kts
- [x] Create package structure

## ✅ Phase 2: Core Implementation (COMPLETE)

- [x] Implement NotificationManager
- [x] Implement DeepLinkHandler
- [x] Implement ShortcutManager
- [x] Implement LocationProvider
- [x] Implement CameraProvider
- [x] Implement BiometricProvider
- [x] Create DI module

## ⏳ Phase 3: App Integration (TODO)

### MainActivity Updates
- [ ] Inject DeepLinkHandler
- [ ] Handle intent deep links in onCreate
- [ ] Handle intent deep links in onNewIntent
- [ ] Pass deep link handler to navigation

### Navigation Updates
- [ ] Add deep links to all composable routes
- [ ] Define URI patterns for each screen
- [ ] Test deep link navigation

### AndroidManifest Updates
- [ ] Add deep link intent filters
- [ ] Add app link intent filters (with autoVerify)
- [ ] Add shortcuts meta-data
- [ ] Register widget receivers

## ⏳ Phase 4: Feature Integration (TODO)

### Notifications
- [ ] Identify notification triggers in features
- [ ] Inject AppNotificationManager in ViewModels
- [ ] Send notifications with appropriate deep links
- [ ] Test notification taps navigate correctly

### Shortcuts
- [ ] Implement ShortcutProvider in features
- [ ] Update shortcuts on app launch
- [ ] Update shortcuts when data changes
- [ ] Test shortcut navigation

### Widgets
- [ ] Implement WidgetDataProvider in features
- [ ] Create widget layouts (if using RemoteViews)
- [ ] Register widget receivers in manifest
- [ ] Test widget updates and clicks

### Location
- [ ] Identify location-aware features
- [ ] Inject LocationProvider in ViewModels
- [ ] Request permissions in Compose
- [ ] Use location data for features

### Camera
- [ ] Identify camera-using features
- [ ] Inject CameraProvider in ViewModels
- [ ] Request permissions in Compose
- [ ] Process captured images

### Biometric
- [ ] Identify secure actions
- [ ] Inject BiometricProvider in ViewModels
- [ ] Add biometric authentication before sensitive operations
- [ ] Implement fallback authentication

## ⏳ Phase 5: Testing (TODO)

### Unit Tests
- [ ] Test DeepLinkHandler parsing
- [ ] Test NotificationModel creation
- [ ] Test ShortcutManager operations
- [ ] Test provider result types

### Integration Tests
- [ ] Test notification → deep link → navigation flow
- [ ] Test shortcut → deep link → navigation flow
- [ ] Test widget click → deep link → navigation flow
- [ ] Test location permission → data flow
- [ ] Test camera permission → capture flow
- [ ] Test biometric → authentication flow

### Manual Testing
- [ ] Send notification, tap, verify navigation
- [ ] Long-press app icon, tap shortcut, verify navigation
- [ ] Add widget, tap action, verify navigation
- [ ] Test location permission flow
- [ ] Test camera permission flow
- [ ] Test biometric authentication flow

## ⏳ Phase 6: App Links Verification (TODO)

### Setup
- [ ] Generate SHA-256 certificate fingerprint
- [ ] Create assetlinks.json file
- [ ] Upload to https://yourdomain.com/.well-known/assetlinks.json
- [ ] Verify with Google's testing tool

### Testing
- [ ] Test HTTPS links open app (not browser)
- [ ] Test app link verification status
- [ ] Test fallback to browser if verification fails

## ⏳ Phase 7: Production Readiness (TODO)

### Performance
- [ ] Profile widget update performance
- [ ] Optimize shortcut update frequency
- [ ] Cache location results appropriately
- [ ] Release camera resources promptly

### Security
- [ ] Validate all deep link parameters
- [ ] Don't include sensitive data in notifications
- [ ] Don't display sensitive data in widgets
- [ ] Implement biometric fallback

### User Experience
- [ ] Add permission rationale dialogs
- [ ] Handle permission denials gracefully
- [ ] Provide clear error messages
- [ ] Test on multiple Android versions

### Analytics
- [ ] Track notification open rates
- [ ] Track shortcut usage
- [ ] Track widget interactions
- [ ] Track deep link sources

## 📋 Quick Commands

```bash
# Build os-integration module
./gradlew :core:os-integration:build

# Run tests
./gradlew :core:os-integration:test

# Build entire app
./gradlew build

# Install debug APK
./gradlew installDebug

# Test deep link (via adb)
adb shell am start -W -a android.intent.action.VIEW -d "app://feature/item/123" com.momoterminal

# Test HTTPS app link (via adb)
adb shell am start -W -a android.intent.action.VIEW -d "https://momoterminal.com/feature/item/123" com.momoterminal

# Check app link verification status
adb shell pm get-app-links com.momoterminal
```

## 🎯 Priority Order

### High Priority (Do First)
1. ✅ Module setup
2. ✅ Core implementation
3. ⏳ MainActivity integration
4. ⏳ Navigation deep links
5. ⏳ Basic notification integration

### Medium Priority (Do Next)
1. ⏳ Shortcuts implementation
2. ⏳ Location provider integration
3. ⏳ Biometric authentication
4. ⏳ Testing

### Low Priority (Do Later)
1. ⏳ Widgets implementation
2. ⏳ Camera provider integration
3. ⏳ App links verification
4. ⏳ Analytics

## 📝 Notes

### Deep Link Testing
```bash
# Test from terminal
adb shell am start -W -a android.intent.action.VIEW \
  -d "app://feature/item/123" \
  com.momoterminal

# Test from browser (create test HTML)
<a href="app://feature/item/123">Open in App</a>
<a href="https://momoterminal.com/feature/item/123">Open in App (HTTPS)</a>
```

### Notification Testing
```kotlin
// In any ViewModel, trigger notification
notificationManager.show(
    NotificationModel(
        id = 1,
        channelType = NotificationChannelType.GENERAL,
        title = "Test",
        message = "Tap to test deep link",
        deepLink = "app://feature/item/123"
    )
)
```

### Shortcut Testing
```kotlin
// Update shortcuts programmatically
shortcutManager.updateShortcuts(listOf(
    AppShortcut(
        id = "test",
        shortLabel = "Test",
        longLabel = "Test Shortcut",
        iconResId = R.drawable.ic_test,
        deepLink = "app://feature/item/123",
        rank = 0
    )
))

// Then long-press app icon to see shortcuts
```

## ⚠️ Common Issues

### Issue: Deep links not working
**Solution**: Check intent filters in AndroidManifest.xml, verify URI patterns match

### Issue: Notifications not showing
**Solution**: Check notification channel creation, verify permissions granted

### Issue: Shortcuts not appearing
**Solution**: Check max 4 dynamic shortcuts, verify static shortcuts XML

### Issue: Location permission denied
**Solution**: Add permission rationale, handle denial gracefully

### Issue: Biometric not available
**Solution**: Check device capability, provide fallback authentication

## ✨ Success Criteria

- [ ] All deep links navigate correctly
- [ ] Notifications open correct screens
- [ ] Shortcuts work from launcher
- [ ] Widgets update and respond to clicks
- [ ] Location permission flow works
- [ ] Camera permission flow works
- [ ] Biometric authentication works
- [ ] All tests pass
- [ ] No crashes or errors
- [ ] Performance is acceptable

---

**Current Status**: Phase 2 Complete (Core Implementation)
**Next Step**: Phase 3 (App Integration)
**Estimated Time**: 2-3 hours for full integration
