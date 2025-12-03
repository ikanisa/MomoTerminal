#!/bin/bash

# MomoTerminal - Automated Import Fix Script
# This script updates all import statements to reflect the new modular structure

echo "🔧 Starting automated import fixes..."

# Navigate to project root
cd "$(dirname "$0")/.."

# Backup notification
echo "⚠️  Creating backup branch..."
git checkout -b backup/before-import-fixes 2>/dev/null || echo "Branch already exists"
git checkout main

# Fix TokenManager imports
echo "📦 Fixing TokenManager imports..."
find . -name "*.kt" -type f -not -path "*/build/*" -not -path "*/.gradle/*" -exec sed -i '' 's/import com\.momoterminal\.auth\.TokenManager/import com.momoterminal.core.common.auth.TokenManager/g' {} +

# Fix SecureStorage imports
echo "🔒 Fixing SecureStorage imports..."
find . -name "*.kt" -type f -not -path "*/build/*" -not -path "*/.gradle/*" -exec sed -i '' 's/import com\.momoterminal\.security\.SecureStorage/import com.momoterminal.core.security.SecureStorage/g' {} +

# Fix config imports
echo "⚙️  Fixing config imports..."
find . -name "*.kt" -type f -not -path "*/build/*" -not -path "*/.gradle/*" -exec sed -i '' 's/import com\.momoterminal\.config\.AppConfig/import com.momoterminal.core.common.config.AppConfig/g' {} +
find . -name "*.kt" -type f -not -path "*/build/*" -not -path "*/.gradle/*" -exec sed -i '' 's/import com\.momoterminal\.config\.CountryDetector/import com.momoterminal.core.common.config.CountryDetector/g' {} +
find . -name "*.kt" -type f -not -path "*/build/*" -not -path "*/.gradle/*" -exec sed -i '' 's/import com\.momoterminal\.config\.SupportedCountries/import com.momoterminal.core.common.config.SupportedCountries/g' {} +
find . -name "*.kt" -type f -not -path "*/build/*" -not -path "*/.gradle/*" -exec sed -i '' 's/import com\.momoterminal\.config\.UssdConfig/import com.momoterminal.core.common.config.UssdConfig/g' {} +

# Fix CountryConfig imports
echo "🌍 Fixing CountryConfig imports..."
find . -name "*.kt" -type f -not -path "*/build/*" -not -path "*/.gradle/*" -exec sed -i '' 's/import com\.momoterminal\.data\.model\.CountryConfig/import com.momoterminal.core.common.model.CountryConfig/g' {} +

# Fix DAO imports
echo "💾 Fixing DAO imports..."
find . -name "*.kt" -type f -not -path "*/build/*" -not -path "*/.gradle/*" -exec sed -i '' 's/import com\.momoterminal\.data\.local\.dao\.TransactionDao/import com.momoterminal.core.database.dao.TransactionDao/g' {} +

# Fix entity imports
echo "📊 Fixing entity imports..."
find . -name "*.kt" -type f -not -path "*/build/*" -not -path "*/.gradle/*" -exec sed -i '' 's/import com\.momoterminal\.data\.local\.entity\.TransactionEntity/import com.momoterminal.core.database.entity.TransactionEntity/g' {} +
find . -name "*.kt" -type f -not -path "*/build/*" -not -path "*/.gradle/*" -exec sed -i '' 's/import com\.momoterminal\.data\.local\.entity\.SmsTransactionEntity/import com.momoterminal.core.database.entity.SmsTransactionEntity/g' {} +

echo "✅ Import fixes complete!"
echo ""
echo "📋 Next steps:"
echo "1. Review changes: git diff"
echo "2. Add missing dependencies to build.gradle.kts files (see fix_plan.md)"
echo "3. Test build: ./gradlew assembleDebug"
echo "4. Commit: git add -A && git commit -m 'fix: update imports for modularization'"
