#!/bin/bash

echo "🔨 Building modular architecture..."
echo ""

# Build core modules first
echo "📦 Building core modules..."
./gradlew :core:common:build --no-daemon --quiet && echo "✅ core:common" || echo "❌ core:common"
./gradlew :core:ui:build --no-daemon --quiet && echo "✅ core:ui" || echo "❌ core:ui"
./gradlew :core:domain:build --no-daemon --quiet && echo "✅ core:domain" || echo "❌ core:domain"
./gradlew :core:designsystem:build --no-daemon --quiet && echo "✅ core:designsystem" || echo "❌ core:designsystem"
./gradlew :core:network:build --no-daemon --quiet && echo "✅ core:network" || echo "❌ core:network"
./gradlew :core:database:build --no-daemon --quiet && echo "✅ core:database" || echo "❌ core:database"
./gradlew :core:data:build --no-daemon --quiet && echo "✅ core:data" || echo "❌ core:data"

echo ""
echo "🎯 Building feature modules..."
./gradlew :feature:payment:build --no-daemon --quiet && echo "✅ feature:payment" || echo "❌ feature:payment"
./gradlew :feature:auth:build --no-daemon --quiet && echo "✅ feature:auth" || echo "❌ feature:auth"
./gradlew :feature:transactions:build --no-daemon --quiet && echo "✅ feature:transactions" || echo "❌ feature:transactions"
./gradlew :feature:settings:build --no-daemon --quiet && echo "✅ feature:settings" || echo "❌ feature:settings"

echo ""
echo "📱 Building app module..."
./gradlew :app:assembleDebug --no-daemon --quiet && echo "✅ app:assembleDebug" || echo "❌ app:assembleDebug"

echo ""
echo "✨ Build verification complete!"
