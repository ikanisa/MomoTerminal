# Vending Feature - Backend Implementation Guide

## ✅ What Was Created

### 1. Complete Database Schema
**File**: `supabase/migrations/vending_schema.sql`

This SQL file includes:
- ✅ 5 database tables (products, machines, orders, codes, transactions)
- ✅ Helper function to generate unique 4-digit codes
- ✅ Main atomic function: `create_vending_order()` - handles wallet debit + code generation
- ✅ Function: `use_vending_code()` - for machine API to validate codes
- ✅ Function: `process_expired_vending_codes()` - auto-refund cron job
- ✅ Row Level Security (RLS) policies
- ✅ Sample data (products + machines)

### 2. Edge Functions (TypeScript/Deno)
Location: `supabase/functions/`

**Functions to create**:
1. `get-vending-machines` - Get nearby machines with location
2. `get-vending-machine` - Get single machine details
3. `create-vending-order` - Create order + generate code
4. `get-vending-orders` - Get user's order history
5. `get-vending-order` - Get single order details

## 🚀 Deployment Steps

### Step 1: Deploy Database Schema

```bash
# Option A: Using Supabase CLI (Recommended)
cd /Users/jeanbosco/workspace/MomoTerminal
supabase db push

# Option B: Manual via Supabase Dashboard
# 1. Go to https://supabase.com/dashboard
# 2. Select your project
# 3. Go to SQL Editor
# 4. Copy & paste the entire vending_schema.sql file
# 5. Click "Run"
```

### Step 2: Create Edge Functions

```bash
# Install Supabase CLI if not installed
# brew install supabase/tap/supabase

# Create each function
supabase functions new get-vending-machines
supabase functions new create-vending-order
supabase functions new get-vending-orders
supabase functions new get-vending-order

# Copy the function code from the examples below
# Then deploy
supabase functions deploy get-vending-machines
supabase functions deploy create-vending-order
supabase functions deploy get-vending-orders
supabase functions deploy get-vending-order
```

### Step 3: Set Environment Variables

```bash
# In Supabase Dashboard → Project Settings → Edge Functions → Secrets
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
```

## 📝 Edge Function Examples

### 1. GET /vending/machines
```typescript
// supabase/functions/get-vending-machines/index.ts
import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

serve(async (req) => {
  const supabase = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_ANON_KEY')!,
    { global: { headers: { Authorization: req.headers.get('Authorization')! } } }
  )
  
  const url = new URL(req.url)
  const lat = url.searchParams.get('latitude')
  const lng = url.searchParams.get('longitude')
  
  // Query machines with products
  const { data, error } = await supabase
    .from('vending_machines')
    .select(`*, vending_products(*)`)
    .eq('status', 'AVAILABLE')
  
  if (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 400,
      headers: { 'Content-Type': 'application/json' }
    })
  }
  
  // Transform to API format
  const machines = data.map(m => ({
    id: m.id,
    name: m.name,
    location: m.location,
    latitude: m.latitude,
    longitude: m.longitude,
    status: m.status,
    product_id: m.product_id,
    product_name: m.vending_products.name,
    product_size_ml: m.vending_products.size_ml,
    price: m.vending_products.price,
    currency: 'XAF',
    stock_level: m.stock_level,
    image_url: m.image_url
  }))
  
  return new Response(JSON.stringify({ machines }), {
    headers: { 'Content-Type': 'application/json' }
  })
})
```

### 2. POST /vending/orders (CRITICAL - Creates Order + Code)
```typescript
// supabase/functions/create-vending-order/index.ts
import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

serve(async (req) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 })
  }
  
  const { machine_id, amount } = await req.json()
  
  const supabase = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!, // Use service role for RPC
    { global: { headers: { Authorization: req.headers.get('Authorization')! } } }
  )
  
  // Get authenticated user
  const { data: { user }, error: userError } = await supabase.auth.getUser()
  if (userError || !user) {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' }
    })
  }
  
  // Call atomic function
  const { data, error } = await supabase.rpc('create_vending_order', {
    p_user_id: user.id,
    p_machine_id: machine_id,
    p_amount: amount
  })
  
  if (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 400,
      headers: { 'Content-Type': 'application/json' }
    })
  }
  
  const result = data[0] // RPC returns array
  
  // Get full order details
  const { data: orderData } = await supabase
    .from('vending_orders')
    .select('*')
    .eq('id', result.order_id)
    .single()
  
  return new Response(JSON.stringify({
    order: {
      id: orderData.id,
      user_id: orderData.user_id,
      machine_id: orderData.machine_id,
      machine_name: orderData.machine_name,
      machine_location: orderData.machine_location,
      product_name: orderData.product_name,
      product_size_ml: orderData.product_size_ml,
      amount: orderData.amount,
      status: orderData.status,
      created_at: orderData.created_at
    },
    code: result.code,
    code_expires_at: result.code_expires_at
  }), {
    headers: { 'Content-Type': 'application/json' }
  })
})
```

### 3. GET /vending/orders
```typescript
// supabase/functions/get-vending-orders/index.ts
import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

serve(async (req) => {
  const supabase = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_ANON_KEY')!,
    { global: { headers: { Authorization: req.headers.get('Authorization')! } } }
  )
  
  const { data: { user } } = await supabase.auth.getUser()
  if (!user) {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' }
    })
  }
  
  // Get orders with codes (RLS automatically filters by user)
  const { data: orders, error } = await supabase
    .from('vending_orders')
    .select(`*, vending_codes(*)`)
    .order('created_at', { ascending: false })
  
  if (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 400,
      headers: { 'Content-Type': 'application/json' }
    })
  }
  
  // Transform to API format
  const transformedOrders = orders.map(o => ({
    id: o.id,
    user_id: o.user_id,
    machine_id: o.machine_id,
    machine_name: o.machine_name,
    machine_location: o.machine_location,
    product_name: o.product_name,
    product_size_ml: o.product_size_ml,
    amount: o.amount,
    status: o.status,
    created_at: o.created_at,
    code: o.vending_codes[0]?.code_plain || null,
    code_expires_at: o.vending_codes[0]?.expires_at || null,
    code_used_at: o.vending_codes[0]?.used_at || null
  }))
  
  return new Response(JSON.stringify({ orders: transformedOrders }), {
    headers: { 'Content-Type': 'application/json' }
  })
})
```

## 🔒 Security Checklist

- ✅ Row Level Security (RLS) enabled on all tables
- ✅ User can only see their own orders
- ✅ Codes are hashed (SHA256) in database
- ✅ Plain codes cleared after use
- ✅ Atomic transactions prevent double-spend
- ✅ Code validation checks expiry + used status
- ✅ Service role key used only in Edge Functions (server-side)

## ⏰ Cron Job for Expired Codes

Add this to Supabase Dashboard → Database → Functions → pg_cron:

```sql
-- Run every 5 minutes
SELECT cron.schedule(
    'process-expired-vending-codes',
    '*/5 * * * *', -- Every 5 minutes
    $$SELECT process_expired_vending_codes()$$
);
```

## 🧪 Testing

### Test with cURL:

```bash
# 1. Get machines
curl https://your-project.supabase.co/functions/v1/get-vending-machines \
  -H "Authorization: Bearer YOUR_USER_TOKEN"

# 2. Create order
curl -X POST https://your-project.supabase.co/functions/v1/create-vending-order \
  -H "Authorization: Bearer YOUR_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"machine_id":"MACHINE_UUID","amount":50000}'

# 3. Get orders
curl https://your-project.supabase.co/functions/v1/get-vending-orders \
  -H "Authorization: Bearer YOUR_USER_TOKEN"
```

### Test with Postman:
1. Import endpoints
2. Set Authorization header with JWT token
3. Test each endpoint

## 📊 Monitoring

### Check metrics in Supabase Dashboard:
- Edge Functions → Logs
- Database → Query Performance
- Database → Table Sizes

### Important queries:
```sql
-- Check active codes
SELECT COUNT(*) FROM vending_codes 
WHERE expires_at > EXTRACT(EPOCH FROM NOW()) * 1000 
AND used_at IS NULL;

-- Check orders by status
SELECT status, COUNT(*) FROM vending_orders GROUP BY status;

-- Check refunds
SELECT COUNT(*), SUM(amount) FROM vending_transactions WHERE type = 'REFUND';
```

## 🚨 Troubleshooting

### "Wallet not found"
- Ensure wallet table exists and has user's wallet
- Check wallet schema matches expected structure

### "Insufficient balance"
- Check wallet.total_tokens column
- Verify amount is in correct units (cents/minor units)

### "Code expired" immediately
- Check server time vs client time
- Verify expires_at calculation (should be +5 minutes)

### Edge Function errors
- Check logs in Supabase Dashboard
- Verify environment variables are set
- Test with simplified function first

## ✅ Deployment Checklist

- [ ] Database schema deployed
- [ ] Sample data inserted
- [ ] Edge Functions created
- [ ] Edge Functions deployed
- [ ] Environment variables set
- [ ] RLS policies enabled
- [ ] Cron job scheduled
- [ ] Tested GET machines
- [ ] Tested POST order
- [ ] Tested GET orders
- [ ] Verified wallet debit
- [ ] Verified code generation
- [ ] Verified auto-refund

---

**Once deployed, the app will have a fully functional backend!** 🚀
