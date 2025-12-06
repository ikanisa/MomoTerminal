#!/bin/bash
#
# Module Isolation Build Script
# Purpose: Identify which module is failing to compile
# Usage: ./check_modules.sh

set -e  # Exit on first error

cd "$(dirname "$0")"

echo "===================================================================="
echo "MomoTerminal - Module Isolation Build Script"
echo "===================================================================="
echo ""
echo "This script will build each module individually to identify"
echo "which one is causing the build failure."
echo ""

# Clean everything first
echo "Step 1: Cleaning all build outputs..."
./gradlew clean --quiet
echo "✅ Clean complete"
echo ""

# Define modules to test
CORE_MODULES=(
  "core:common"
  "core:domain"
  "core:database"
  "core:data"
  "core:network"
  "core:security"
  "core:ui"
  "core:designsystem"
)

FEATURE_MODULES=(
  "feature:auth"
  "feature:sms"
  "feature:payment"
  "feature:settings"
  "feature:transactions"
  "feature:wallet"
)

# Test core modules
echo "===================================================================="
echo "Step 2: Building Core Modules"
echo "===================================================================="
echo ""

FAILED_MODULE=""

for module in "${CORE_MODULES[@]}"; do
  echo "Building $module..."
  if ./gradlew ":$module:compileDebugKotlin" --quiet > "build_${module//:/_}.log" 2>&1; then
    echo "  ✅ PASSED: $module"
  else
    echo "  ❌ FAILED: $module"
    echo ""
    echo "===================================================================="
    echo "FAILURE FOUND IN: $module"
    echo "===================================================================="
    echo ""
    echo "Error log saved to: build_${module//:/_}.log"
    echo ""
    echo "First 20 lines of error:"
    head -20 "build_${module//:/_}.log"
    echo ""
    echo "Last 30 lines of error:"
    tail -30 "build_${module//:/_}.log"
    echo ""
    FAILED_MODULE="$module"
    break
  fi
done

if [ -n "$FAILED_MODULE" ]; then
  echo "===================================================================="
  echo "ROOT CAUSE IDENTIFIED"
  echo "===================================================================="
  echo ""
  echo "Module: $FAILED_MODULE"
  echo "Log file: build_${FAILED_MODULE//:/_}.log"
  echo ""
  echo "Next steps:"
  echo "1. Review the error messages above"
  echo "2. Fix the compilation errors in $FAILED_MODULE"
  echo "3. Run: ./gradlew :$FAILED_MODULE:compileDebugKotlin"
  echo "4. Once it passes, re-run this script"
  echo ""
  exit 1
fi

echo ""
echo "✅ All core modules compiled successfully!"
echo ""

# Test feature modules
echo "===================================================================="
echo "Step 3: Building Feature Modules"
echo "===================================================================="
echo ""

for module in "${FEATURE_MODULES[@]}"; do
  echo "Building $module..."
  if ./gradlew ":$module:compileDebugKotlin" --quiet > "build_${module//:/_}.log" 2>&1; then
    echo "  ✅ PASSED: $module"
  else
    echo "  ❌ FAILED: $module"
    echo ""
    echo "===================================================================="
    echo "FAILURE FOUND IN: $module"
    echo "===================================================================="
    echo ""
    echo "Error log saved to: build_${module//:/_}.log"
    echo ""
    echo "First 20 lines of error:"
    head -20 "build_${module//:/_}.log"
    echo ""
    echo "Last 30 lines of error:"
    tail -30 "build_${module//:/_}.log"
    echo ""
    FAILED_MODULE="$module"
    break
  fi
done

if [ -n "$FAILED_MODULE" ]; then
  echo "===================================================================="
  echo "ROOT CAUSE IDENTIFIED"
  echo "===================================================================="
  echo ""
  echo "Module: $FAILED_MODULE"
  echo "Log file: build_${FAILED_MODULE//:/_}.log"
  echo ""
  echo "Next steps:"
  echo "1. Review the error messages above"
  echo "2. Fix the compilation errors in $FAILED_MODULE"
  echo "3. Run: ./gradlew :$FAILED_MODULE:compileDebugKotlin"
  echo "4. Once it passes, re-run this script"
  echo ""
  exit 1
fi

echo ""
echo "✅ All feature modules compiled successfully!"
echo ""

# Now try KSP (Hilt code generation)
echo "===================================================================="
echo "Step 4: Testing Hilt Code Generation (KSP)"
echo "===================================================================="
echo ""

echo "Running KSP for feature:sms..."
if ./gradlew ":feature:sms:kspDebugKotlin" > "build_ksp_sms.log" 2>&1; then
  echo "  ✅ PASSED: feature:sms KSP"
else
  echo "  ❌ FAILED: feature:sms KSP"
  echo ""
  echo "Error log saved to: build_ksp_sms.log"
  echo ""
  echo "Last 50 lines of error:"
  tail -50 "build_ksp_sms.log"
  echo ""
  echo "KSP failure usually means:"
  echo "1. Missing @Inject constructor on a dependency"
  echo "2. Missing @HiltAndroidApp annotation"
  echo "3. Missing Hilt module for a binding"
  echo "4. Circular dependency in injection graph"
  echo ""
  exit 1
fi

echo "Running KSP for app module..."
if ./gradlew ":app:kspDebugKotlin" > "build_ksp_app.log" 2>&1; then
  echo "  ✅ PASSED: app KSP"
else
  echo "  ❌ FAILED: app KSP"
  echo ""
  echo "Error log saved to: build_ksp_app.log"
  echo ""
  echo "Last 50 lines of error:"
  tail -50 "build_ksp_app.log"
  echo ""
  exit 1
fi

echo ""
echo "✅ All KSP processing completed successfully!"
echo ""

# Final full build
echo "===================================================================="
echo "Step 5: Full Debug Build"
echo "===================================================================="
echo ""

echo "Running full assembleDebug..."
if ./gradlew assembleDebug > "build_full.log" 2>&1; then
  echo "  ✅ BUILD SUCCESSFUL!"
  echo ""
  echo "===================================================================="
  echo "SUCCESS - App is ready for testing!"
  echo "===================================================================="
  echo ""
  echo "APK Location:"
  echo "  app/build/outputs/apk/debug/app-debug.apk"
  echo ""
  echo "Next steps:"
  echo "1. Run unit tests: ./gradlew testDebugUnitTest"
  echo "2. Install APK: adb install app/build/outputs/apk/debug/app-debug.apk"
  echo "3. Begin manual testing"
  echo ""
else
  echo "  ❌ BUILD FAILED (unexpected)"
  echo ""
  echo "All modules compiled but full build failed."
  echo "This usually means:"
  echo "1. Resource conflict"
  echo "2. Manifest merger issue"
  echo "3. ProGuard/R8 configuration error"
  echo ""
  echo "Error log saved to: build_full.log"
  echo "Last 50 lines:"
  tail -50 "build_full.log"
  exit 1
fi

echo ""
echo "===================================================================="
echo "Build Isolation Complete - All Tests Passed! ✅"
echo "===================================================================="
