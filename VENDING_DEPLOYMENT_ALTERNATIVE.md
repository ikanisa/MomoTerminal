# Vending Deployment - Alternative Approach (Function Limit Workaround)

## Problem
Supabase free tier has reached function limit (64/64 functions deployed). Cannot deploy new vending Edge Functions.

## Solution
Use **direct database RPC calls** instead of Edge Functions. This is actually **better** for this use case:
- Faster (no HTTP overhead)
- Simpler (direct SQL)
- No function limits
- Same security (RLS enforced)

## Deployment Steps

### 1. Deploy Migration via SQL Editor

Copy the contents of `supabase/migrations/20251208190000_vending_multi_cup_system.sql` and run it in Supabase SQL Editor:

**URL**: https://supabase.com/dashboard/project/lhbowpbcpwoiparwnwgt/sql/new

Just paste the entire file and click "Run".

### 2. Update Android Repository to use RPC

Instead of calling Edge Functions, the Android app will call the database RPC functions directly using Supabase client.

**File to update**: `feature/vending/src/main/java/com/momoterminal/feature/vending/data/VendingRepositoryImpl.kt`

Replace HTTP calls with Supabase RPC calls:

```kotlin
// Instead of:
apiService.createOrder(CreateOrderRequest(machineId, quantity))

// Use:
supabase.rpc("create_vending_order", mapOf(
    "p_user_id" to userId,
    "p_machine_id" to machineId,
    "p_quantity" to quantity
))
```

### 3. Benefits of This Approach

✅ **No function limits** - Uses PostgreSQL functions directly
✅ **Faster** - No Edge Function cold starts
✅ **Simpler** - Less moving parts
✅ **Same security** - RLS policies still enforced
✅ **Better for mobile** - Supabase client handles auth automatically

### 4. Migration SQL to Run Now

```sql
-- Run this in Supabase SQL Editor
-- Copy from: supabase/migrations/20251208190000_vending_multi_cup_system.sql
```

### 5. Next: Fix Android UI

Once migration is deployed, fix the 2 broken screens and wire navigation.

---

**Recommendation**: Let's deploy migration via SQL Editor now, then fix Android app to use RPC.

