#!/bin/bash
# Deploy Vending Edge Functions to Supabase
# Run this script to deploy all vending-related Edge Functions

set -e  # Exit on error

echo "🚀 Deploying Vending Edge Functions to Supabase..."
echo ""

# Check if Supabase CLI is installed
if ! command -v supabase &> /dev/null; then
    echo "❌ Supabase CLI not found. Please install it first:"
    echo "   npm install -g supabase"
    exit 1
fi

# Check if we're logged in
if ! supabase projects list &> /dev/null; then
    echo "❌ Not logged in to Supabase. Please run:"
    echo "   supabase login"
    exit 1
fi

cd "$(dirname "$0")"
cd supabase

echo "📦 Deploying vending Edge Functions..."
echo ""

# Deploy all vending functions
FUNCTIONS=(
    "create-vending-order"
    "get-vending-machine"
    "get-vending-machines"
    "get-vending-order"
    "get-vending-orders"
    "parse-sms-ai"
)

FAILED=()

for func in "${FUNCTIONS[@]}"; do
    echo "⏳ Deploying $func..."
    if supabase functions deploy "$func" --no-verify-jwt; then
        echo "✅ $func deployed successfully"
    else
        echo "❌ Failed to deploy $func"
        FAILED+=("$func")
    fi
    echo ""
done

# Also deploy updated update-user-profile with sanitization
echo "⏳ Deploying update-user-profile (with input sanitization)..."
if supabase functions deploy "update-user-profile" --no-verify-jwt; then
    echo "✅ update-user-profile deployed successfully"
else
    echo "❌ Failed to deploy update-user-profile"
    FAILED+=("update-user-profile")
fi
echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Deployment Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ ${#FAILED[@]} -eq 0 ]; then
    echo "✅ All functions deployed successfully!"
    echo ""
    echo "🎉 Vending API is now live!"
    echo ""
    echo "Test endpoints:"
    echo "  GET  https://lhbowpbcpwoiparwnwgt.supabase.co/functions/v1/get-vending-machines"
    echo "  GET  https://lhbowpbcpwoiparwnwgt.supabase.co/functions/v1/get-vending-machine?id={id}"
    echo "  POST https://lhbowpbcpwoiparwnwgt.supabase.co/functions/v1/create-vending-order"
    echo ""
else
    echo "⚠️  ${#FAILED[@]} function(s) failed to deploy:"
    for func in "${FAILED[@]}"; do
        echo "   - $func"
    done
    echo ""
    exit 1
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Next steps:"
echo "1. Test endpoints with curl or Postman"
echo "2. Build and install Android app: ./gradlew installDebug"
echo "3. Verify vending screen loads machines"
echo ""
