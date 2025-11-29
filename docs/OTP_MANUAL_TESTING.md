# WhatsApp OTP - Manual Testing Guide

## Prerequisites

### Required Setup:
1. **Supabase Project**
   - Project URL configured in `local.properties`
   - Anonymous key configured
   - Phone Auth enabled
   - WhatsApp provider configured with template

2. **Test Device/Emulator**
   - Android device with WhatsApp installed
   - Or emulator with Google Play Services
   - Internet connection

3. **Test Phone Number**
   - Valid phone number in E.164 format
   - Has WhatsApp account
   - Example: +250788767816

## Test Scenarios

### Scenario 1: Successful Login Flow

**Objective**: Verify complete OTP authentication flow works end-to-end

**Steps**:
1. Launch the app
2. Navigate to login screen
3. Enter valid phone number: `+250788767816`
4. Tap "Send OTP" button
5. Wait for loading indicator
6. Check WhatsApp for OTP message
7. Enter the 6-digit OTP code
8. Tap "Verify" button
9. Observe navigation to home screen

**Expected Results**:
- ✅ "Send OTP" shows loading state
- ✅ WhatsApp message received within 30 seconds
- ✅ OTP code is 6 digits
- ✅ "Verify" button enables when code entered
- ✅ Successful login navigates to home
- ✅ User session is persisted
- ✅ No error messages displayed

**Test Data**:
```
Phone: +250788767816
Expected OTP: (from WhatsApp)
```

### Scenario 2: Invalid Phone Number

**Objective**: Verify validation of phone number format

**Steps**:
1. Launch app
2. Enter invalid phone: `123` or `invalid`
3. Tap "Send OTP"
4. Observe error message

**Expected Results**:
- ✅ Error message displayed: "Invalid phone number format"
- ✅ No OTP sent
- ✅ User can correct and retry

**Test Data**:
```
Invalid Formats:
- 123
- abc
- 0788767816 (missing country code)
- +250 (incomplete)
```

### Scenario 3: Wrong OTP Code

**Objective**: Verify handling of incorrect OTP

**Steps**:
1. Complete Scenario 1 steps 1-6
2. Enter wrong OTP: `999999`
3. Tap "Verify"
4. Observe error message

**Expected Results**:
- ✅ Error displayed: "Invalid OTP code"
- ✅ OTP input field cleared or highlighted
- ✅ User can request new OTP
- ✅ Session NOT created

**Test Data**:
```
Wrong OTP: 999999
Wrong OTP: 123456 (if not the actual code)
Wrong OTP: 00000
```

### Scenario 4: OTP Expiration

**Objective**: Verify OTP expires after timeout

**Steps**:
1. Request OTP
2. Receive OTP code
3. Wait 10 minutes (Supabase default timeout)
4. Enter the expired OTP
5. Tap "Verify"

**Expected Results**:
- ✅ Error: "OTP code has expired"
- ✅ User prompted to request new OTP
- ✅ "Resend OTP" button available

### Scenario 5: Network Disconnection

**Objective**: Verify graceful handling of network issues

**Steps**:
1. Enable Airplane mode
2. Enter phone number
3. Tap "Send OTP"
4. Observe error handling
5. Disable Airplane mode
6. Retry

**Expected Results**:
- ✅ Error: "Network connection failed"
- ✅ Retry button available
- ✅ No crash
- ✅ Works when network restored

### Scenario 6: Session Persistence

**Objective**: Verify session survives app restart

**Steps**:
1. Complete successful login (Scenario 1)
2. Close app completely
3. Reopen app
4. Observe initial screen

**Expected Results**:
- ✅ User remains logged in
- ✅ Navigates to home screen automatically
- ✅ Access token still valid
- ✅ No re-authentication required

### Scenario 7: Logout

**Objective**: Verify logout clears session

**Steps**:
1. Login successfully
2. Navigate to settings/profile
3. Tap "Logout"
4. Confirm logout
5. Close and reopen app

**Expected Results**:
- ✅ Navigates to login screen
- ✅ Session cleared
- ✅ Tokens removed
- ✅ Cannot access protected screens
- ✅ Must login again

### Scenario 8: Multiple OTP Requests

**Objective**: Verify handling of OTP resend

**Steps**:
1. Request OTP for phone number
2. Wait 2 minutes
3. Tap "Resend OTP"
4. Observe new OTP sent

**Expected Results**:
- ✅ Previous OTP invalidated
- ✅ New OTP sent to WhatsApp
- ✅ New OTP works for verification
- ✅ Old OTP fails verification

### Scenario 9: Empty/Incomplete OTP

**Objective**: Verify input validation

**Steps**:
1. Request OTP
2. Enter only 3 digits: `123`
3. Attempt to verify
4. Enter non-numeric: `abc123`
5. Attempt to verify

**Expected Results**:
- ✅ Verify button disabled for incomplete code
- ✅ Only numeric input accepted
- ✅ Must be exactly 6 digits

### Scenario 10: Development Bypass (DEBUG only)

**Objective**: Verify dev login works for testing

**Steps**:
1. Build DEBUG version
2. Enter phone: `0788767816`
3. Enter OTP: `123456`
4. Tap verify

**Expected Results**:
- ✅ Instant login (no network call)
- ✅ Mock session created
- ✅ Navigates to home
- ⚠️ Only works in DEBUG builds

**Test Data**:
```
Debug Phone: 0788767816
Debug OTP: 123456
```

## Error Messages Reference

| Scenario | Expected Error Message |
|----------|----------------------|
| Invalid Phone | "Invalid phone number format" |
| Wrong OTP | "Invalid OTP code" |
| Expired OTP | "OTP code has expired" |
| Network Error | "Network connection failed" |
| Empty OTP | "OTP code cannot be empty" |
| Server Error | "Authentication service unavailable" |

## Performance Benchmarks

### Expected Response Times:

| Operation | Expected Time | Warning Threshold |
|-----------|--------------|-------------------|
| Send OTP | < 3 seconds | > 5 seconds |
| Verify OTP | < 2 seconds | > 4 seconds |
| Session Load | < 1 second | > 2 seconds |
| Logout | < 500ms | > 1 second |

## Regression Testing

**Run these after ANY code changes:**

1. ✅ Basic login flow (Scenario 1)
2. ✅ Session persistence (Scenario 6)
3. ✅ Logout flow (Scenario 7)
4. ✅ Error handling (Scenario 2, 3)

## Device Matrix Testing

**Test on:**
- Android 8.0 (Oreo) - Minimum supported
- Android 10
- Android 12
- Android 14 (Latest)

**Screen Sizes:**
- Small (< 5")
- Medium (5-6")
- Large (> 6")
- Tablet

## Accessibility Testing

1. **Screen Reader**
   - Enable TalkBack
   - Navigate through OTP flow
   - Verify all elements announced

2. **Large Text**
   - Enable system large text
   - Verify UI remains usable

3. **High Contrast**
   - Enable high contrast mode
   - Verify text readable

## Security Testing

1. **Token Storage**
   - Verify tokens in SecureStorage
   - Check encryption
   - Verify cleared on logout

2. **Session Timeout**
   - Test token expiration
   - Verify auto-refresh
   - Check logout on failure

3. **Debug Bypass**
   - Verify ONLY in DEBUG builds
   - Test production build doesn't allow it

## Test Report Template

```
Test Date: ___________
Tester: ___________
Build: Debug / Release
Version: ___________

Scenario 1: Pass / Fail - Notes: ___________
Scenario 2: Pass / Fail - Notes: ___________
Scenario 3: Pass / Fail - Notes: ___________
Scenario 4: Pass / Fail - Notes: ___________
Scenario 5: Pass / Fail - Notes: ___________
Scenario 6: Pass / Fail - Notes: ___________
Scenario 7: Pass / Fail - Notes: ___________
Scenario 8: Pass / Fail - Notes: ___________
Scenario 9: Pass / Fail - Notes: ___________
Scenario 10: Pass / Fail - Notes: ___________

Overall Status: Pass / Fail
Issues Found: ___________
Recommendations: ___________
```

## Common Issues & Solutions

### Issue 1: OTP Not Received
**Symptoms**: WhatsApp message doesn't arrive  
**Possible Causes**:
- Supabase WhatsApp provider not configured
- Phone number not in E.164 format
- WhatsApp template not approved
- Rate limiting

**Solutions**:
1. Check Supabase dashboard logs
2. Verify phone number format
3. Check WhatsApp template status
4. Wait 1 minute between requests

### Issue 2: "Invalid OTP" for Correct Code
**Symptoms**: Correct OTP shows as invalid  
**Possible Causes**:
- OTP expired (>10 min)
- Network delay
- Supabase session issue

**Solutions**:
1. Request new OTP
2. Verify entered correctly
3. Check device time is accurate

### Issue 3: Session Not Persisting
**Symptoms**: User logged out after app restart  
**Possible Causes**:
- Token storage failed
- Session manager error
- Token expired

**Solutions**:
1. Check SecureStorage permissions
2. Verify token expiration time
3. Check session manager logs

## Sign-off

**QA Approval**: ___________  
**Date**: ___________  
**Status**: Ready for Production / Needs Fixes  
**Comments**: ___________

---
**Version**: 1.0  
**Last Updated**: 2024-11-29
