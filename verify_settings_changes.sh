#!/bin/bash

echo "========================================="
echo "Settings Implementation Verification"
echo "========================================="
echo ""

echo "1. Checking generic terminology in strings.xml..."
if grep -q "Mobile Money Code" app/src/main/res/values/strings.xml; then
    echo "✅ 'Mobile Money Code' found"
else
    echo "❌ 'Mobile Money Code' NOT found"
fi

if grep -q "MTN Mobile Money" app/src/main/res/values/strings.xml; then
    echo "✅ 'MTN Mobile Money' found"
else
    echo "❌ 'MTN Mobile Money' NOT found"
fi

echo ""
echo "2. Checking confirmation dialog strings..."
DIALOGS=("confirm_sms_permission_title" "confirm_camera_permission_title" "confirm_nfc_terminal_title" "confirm_biometric_title")
for dialog in "${DIALOGS[@]}"; do
    if grep -q "$dialog" app/src/main/res/values/strings.xml; then
        echo "✅ $dialog found"
    else
        echo "❌ $dialog NOT found"
    fi
done

echo ""
echo "3. Checking ViewModel dialog methods..."
METHODS=("requestSmsPermission" "requestCameraPermission" "requestNfcTerminalToggle" "requestBiometricToggle")
for method in "${METHODS[@]}"; do
    if grep -q "fun $method" app/src/main/java/com/momoterminal/presentation/screens/settings/SettingsViewModel.kt; then
        echo "✅ $method() method found"
    else
        echo "❌ $method() method NOT found"
    fi
done

echo ""
echo "4. Checking UI dialog implementations..."
UI_DIALOGS=("showSmsPermissionDialog" "showCameraPermissionDialog" "showNfcTerminalDialog" "showBiometricDialog")
for dialog in "${UI_DIALOGS[@]}"; do
    if grep -q "$dialog" app/src/main/java/com/momoterminal/presentation/screens/settings/SettingsScreen.kt; then
        echo "✅ $dialog in UI"
    else
        echo "❌ $dialog NOT in UI"
    fi
done

echo ""
echo "5. Checking SupportedCountries updates..."
if grep -q '"MTN Mobile Money"' core/common/src/main/kotlin/com/momoterminal/core/common/config/SupportedCountries.kt; then
    echo "✅ 'MTN Mobile Money' in SupportedCountries"
else
    echo "❌ 'MTN Mobile Money' NOT in SupportedCountries"
fi

echo ""
echo "6. Checking for remaining 'MoMo' in user-facing strings..."
MOMO_COUNT=$(grep -i "momo" app/src/main/res/values/strings.xml | grep -v "momo_" | grep -v "<!--" | grep -v "Mobile Money" | wc -l)
if [ "$MOMO_COUNT" -eq 0 ]; then
    echo "✅ No standalone 'MoMo' found in strings.xml"
else
    echo "⚠️  Found $MOMO_COUNT potential 'MoMo' references to review"
    grep -n -i "momo" app/src/main/res/values/strings.xml | grep -v "momo_" | grep -v "<!--" | grep -v "Mobile Money" | head -5
fi

echo ""
echo "7. Verifying state properties..."
STATE_PROPS=("showSmsPermissionDialog" "showCameraPermissionDialog" "showNfcTerminalDialog" "showBiometricDialog" "isSaving" "saveError")
for prop in "${STATE_PROPS[@]}"; do
    if grep -q "$prop" app/src/main/java/com/momoterminal/presentation/screens/settings/SettingsViewModel.kt; then
        echo "✅ $prop in state"
    else
        echo "❌ $prop NOT in state"
    fi
done

echo ""
echo "========================================="
echo "Verification Complete"
echo "========================================="
