#!/bin/bash
# Quick build and install script for Phase 1+2 fixes

set -e

echo "🔨 Building MomoTerminal Debug APK..."
echo ""

cd "$(dirname "$0")"

# Build debug APK
echo "⏳ Compiling..."
./gradlew assembleDebug --quiet

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    
    # Check if device is connected
    if adb devices | grep -q "device$"; then
        echo "📱 Device detected, installing..."
        ./gradlew installDebug --quiet
        
        if [ $? -eq 0 ]; then
            echo "✅ App installed successfully!"
            echo ""
            echo "🚀 Launch the app and test:"
            echo "   1. Login → Check profile loads"
            echo "   2. Settings → Save → Check success message"
            echo "   3. Home → Try payment without MoMo → Check error dialog"
            echo "   4. Wallet → Check balance shows"
            echo ""
            echo "📊 View logs:"
            echo "   adb logcat | grep -E 'HomeViewModel|SettingsViewModel|WalletViewModel'"
        else
            echo "❌ Installation failed"
            exit 1
        fi
    else
        echo "⚠️  No device connected"
        echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
        echo ""
        echo "Connect device and run:"
        echo "  ./gradlew installDebug"
    fi
else
    echo "❌ Build failed"
    echo ""
    echo "Check errors above or run:"
    echo "  ./gradlew assembleDebug"
    exit 1
fi
