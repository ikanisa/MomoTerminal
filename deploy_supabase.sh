#!/bin/bash

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║              SUPABASE DEPLOYMENT HELPER SCRIPT                   ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

# Enable Supabase credentials in local.properties
echo "Step 1: Enabling Supabase credentials in local.properties..."
sed -i '' 's/# SUPABASE_URL=/SUPABASE_URL=/g' local.properties
sed -i '' 's/# SUPABASE_ANON_KEY=/SUPABASE_ANON_KEY=/g' local.properties

echo "✅ Supabase credentials enabled"
echo ""

# Show SQL file location
echo "Step 2: Deploy database migration"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Option A: Using Supabase Dashboard (Recommended)"
echo "   1. Open: https://supabase.com/dashboard/project/lhbowpbcpwoiparwnwgt/sql"
echo "   2. Copy contents of: supabase/migrations/001_create_auth_tables.sql"
echo "   3. Paste into SQL Editor"
echo "   4. Click 'Run'"
echo ""
echo "Option B: Using CLI (if psql is available)"
echo "   Run: cat supabase/migrations/001_create_auth_tables.sql | pbcopy"
echo "   Then paste into Supabase SQL Editor"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Copy SQL to clipboard if pbcopy exists
if command -v pbcopy &> /dev/null; then
    cat supabase/migrations/001_create_auth_tables.sql | pbcopy
    echo "✅ SQL migration copied to clipboard! Just paste it in the dashboard."
    echo ""
fi

echo "Step 3: Verify deployment"
echo "   After running the SQL, verify tables exist:"
echo "   - otp_codes"
echo "   - user_profiles"
echo ""

echo "Step 4: Test the app"
echo "   Build and run: ./gradlew installDebug"
echo ""

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║                    DEPLOYMENT CHECKLIST                          ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""
echo "  [ ] Enable Supabase credentials in local.properties"
echo "  [ ] Run SQL migration in Supabase Dashboard"
echo "  [ ] Verify tables created (otp_codes, user_profiles)"
echo "  [ ] Build APK: ./gradlew assembleDebug"
echo "  [ ] Install on phone: adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo "  [ ] Test WhatsApp OTP flow"
echo ""
