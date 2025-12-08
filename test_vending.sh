#!/bin/bash

# 🧃 MomoTerminal Vending Integration - Quick Test Script
# This script installs the app and opens vending screen for testing

set -e

echo "🧃 MomoTerminal Vending Integration Test"
echo "========================================="
echo ""

# Check if device is connected
echo "📱 Checking for connected device..."
if ! adb devices | grep -q "device$"; then
    echo "❌ No device connected. Please connect a device or start an emulator."
    exit 1
fi

echo "✅ Device found!"
echo ""

# Install APK
echo "📦 Installing app..."
./gradlew :app:installDebug --no-daemon

echo ""
echo "✅ App installed successfully!"
echo ""

# Launch app
echo "🚀 Launching MomoTerminal..."
adb shell am start -n com.momoterminal/.MainActivity

echo ""
echo "✅ App launched!"
echo ""

# Wait a bit
sleep 2

echo "📋 Testing Checklist:"
echo "  1. ✓ App should open to HomeScreen"
echo "  2. ✓ Look for '🧃 Get Juice from Vending' button"
echo "  3. □ Tap the button to open vending machines"
echo "  4. □ Select a machine to view details"
echo "  5. □ Complete payment flow"
echo "  6. □ View vending code"
echo "  7. □ Test order history"
echo "  8. □ Test help screen"
echo ""

echo "📊 To view logs:"
echo "  adb logcat | grep -i 'momo\\|vending\\|payment'"
echo ""

echo "🎉 Ready for testing!"
