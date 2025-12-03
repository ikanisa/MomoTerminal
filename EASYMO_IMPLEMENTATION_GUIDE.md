# EasyMO Implementation Guide for MomoTerminal Integration

**Date:** December 3, 2025  
**Purpose:** Detailed implementation requirements for EasyMO Backend, Admin Panel, and Client Portal

---

## Overview

This guide details what needs to be implemented in each EasyMO component to complete the MomoTerminal integration.

```
┌─────────────────────────────────────────────────────────────────┐
│                     IMPLEMENTATION SCOPE                         │
├─────────────────────────────────────────────────────────────────┤
│  MomoTerminal (Android)  ──►  EasyMO Backend (Supabase)         │
│                               ├── Edge Functions                 │
│                               ├── Database                       │
│                               └── Realtime                       │
│                                      │                           │
│                          ┌───────────┴───────────┐              │
│                          ▼                       ▼              │
│                   EasyMO Admin            EasyMO Client         │
│                   (Internal)              (Merchants)           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Part 1: EasyMO Backend (Supabase Edge Functions)

### 1.1 🔴 P0: Nonce Validation for Replay Protection

**File:** `supabase/functions/momo-sms-webhook/index.ts`

```typescript
// Add nonce validation to prevent replay attacks

import { createClient } from '@supabase/supabase-js'

interface NonceValidationResult {
  valid: boolean
  error?: string
}

async function validateNonce(
  supabase: SupabaseClient,
  nonce: string,
  deviceId: string
): Promise<NonceValidationResult> {
  // Check if nonce already exists (replay attack)
  const { data: existing } = await supabase
    .from('webhook_nonces')
    .select('nonce')
    .eq('nonce', nonce)
    .single()

  if (existing) {
    return { valid: false, error: 'Nonce already used - possible replay attack' }
  }

  // Store nonce with 5-minute expiry
  const { error } = await supabase
    .from('webhook_nonces')
    .insert({
      nonce,
      device_id: deviceId,
      expires_at: new Date(Date.now() + 5 * 60 * 1000).toISOString()
    })

  if (error) {
    console.error('Failed to store nonce:', error)
    return { valid: false, error: 'Failed to validate nonce' }
  }

  return { valid: true }
}

// In main handler, add after signature verification:
const nonce = payload.nonce
if (!nonce) {
  return new Response(
    JSON.stringify({ error: 'Missing nonce' }),
    { status: 400 }
  )
}

const nonceResult = await validateNonce(supabase, nonce, payload.device_id)
if (!nonceResult.valid) {
  return new Response(
    JSON.stringify({ error: nonceResult.error }),
    { status: 409 } // Conflict
  )
}
```

---

### 1.2 🔴 P0: Standardize Headers to `X-Momo-*`

**File:** `supabase/functions/momo-sms-webhook/index.ts`

Update header extraction to use standardized names:

```typescript
// BEFORE (inconsistent)
const signature = req.headers.get('X-Webhook-Signature')
const timestamp = req.headers.get('X-Webhook-Timestamp')

// AFTER (standardized)
const signature = req.headers.get('X-Momo-Signature')
const timestamp = req.headers.get('X-Momo-Timestamp')
const deviceId = req.headers.get('X-Momo-Device-Id')
const nonce = req.headers.get('X-Momo-Nonce') // Optional: can also be in body
```

**Files to update:**
- `momo-sms-webhook/index.ts`
- `momo-sms-hook/index.ts`
- `webhook-relay/index.ts`

---

### 1.3 🟡 P1: Idempotency Key Validation

**File:** `supabase/functions/_shared/idempotency.ts` (new file)

```typescript
import { SupabaseClient } from '@supabase/supabase-js'

export interface IdempotencyResult<T> {
  cached: boolean
  result: T | null
}

export async function checkIdempotency<T>(
  supabase: SupabaseClient,
  key: string
): Promise<IdempotencyResult<T>> {
  const { data } = await supabase
    .from('idempotency_keys')
    .select('result')
    .eq('key', key)
    .gt('expires_at', new Date().toISOString())
    .single()

  if (data) {
    return { cached: true, result: data.result as T }
  }
  return { cached: false, result: null }
}

export async function storeIdempotencyResult(
  supabase: SupabaseClient,
  key: string,
  result: unknown,
  ttlSeconds: number = 86400
): Promise<void> {
  await supabase
    .from('idempotency_keys')
    .upsert({
      key,
      result,
      expires_at: new Date(Date.now() + ttlSeconds * 1000).toISOString()
    })
}
```

**Usage in webhook handler:**

```typescript
import { checkIdempotency, storeIdempotencyResult } from '../_shared/idempotency.ts'

// Generate idempotency key from client_transaction_id
const idempotencyKey = `sms-${payload.client_transaction_id}`

// Check for duplicate
const { cached, result } = await checkIdempotency(supabase, idempotencyKey)
if (cached) {
  return new Response(
    JSON.stringify({ ...result, cached: true }),
    { status: 200 }
  )
}

// Process transaction...
const processResult = await processTransaction(payload)

// Store result for future duplicate requests
await storeIdempotencyResult(supabase, idempotencyKey, processResult)
```

---

### 1.4 🟡 P1: Accept client_transaction_id in Payload

**File:** `supabase/functions/momo-sms-webhook/index.ts`

Update payload interface and insert:

```typescript
interface MomoSmsPayloadV2 {
  source: 'momoterminal'
  version: string
  timestamp: string
  nonce: string
  client_transaction_id: string  // NEW
  phone_number: string
  sender: string
  message: string
  device_id: string
  parsed?: {
    amount?: number
    currency?: string
    provider?: string
    transaction_id?: string
  }
}

// When inserting to database:
const { data, error } = await supabase
  .from('momo_transactions')
  .insert({
    client_transaction_id: payload.client_transaction_id,  // NEW
    phone_number: payload.phone_number,
    sender: payload.sender,
    message: payload.message,
    device_id: payload.device_id,
    amount: payload.parsed?.amount,
    currency: payload.parsed?.currency,
    provider: payload.parsed?.provider,
    provider_ref: payload.parsed?.transaction_id,
    status: 'pending'
  })
  .select('id, client_transaction_id')
  .single()

// Return server ID mapped to client ID
return new Response(
  JSON.stringify({
    success: true,
    server_transaction_id: data.id,
    client_transaction_id: data.client_transaction_id
  }),
  { status: 200 }
)
```

---

### 1.5 Database Cleanup Cron Job

**File:** `supabase/functions/cleanup-expired/index.ts` (new)

```typescript
import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from '@supabase/supabase-js'

serve(async () => {
  const supabase = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
  )

  // Cleanup expired nonces
  const { count: noncesDeleted } = await supabase
    .from('webhook_nonces')
    .delete()
    .lt('expires_at', new Date().toISOString())

  // Cleanup expired idempotency keys
  const { count: keysDeleted } = await supabase
    .from('idempotency_keys')
    .delete()
    .lt('expires_at', new Date().toISOString())

  return new Response(
    JSON.stringify({
      nonces_deleted: noncesDeleted,
      idempotency_keys_deleted: keysDeleted
    }),
    { status: 200 }
  )
})
```

**Schedule in Supabase Dashboard:** Run every hour via pg_cron or external scheduler.

---

## Part 2: EasyMO Admin Panel

### 2.1 Device Management Dashboard

**Features needed:**

```
┌─────────────────────────────────────────────────────────────────┐
│  ADMIN PANEL: MomoTerminal Devices                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  [Search devices...]                    [Export CSV] [Refresh]  │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ Device ID      │ Merchant    │ Last Seen  │ Status │ Actions││
│  ├─────────────────────────────────────────────────────────────┤│
│  │ abc123...      │ Shop A      │ 2 min ago  │ 🟢     │ [View] ││
│  │ def456...      │ Shop B      │ 1 hour ago │ 🟡     │ [View] ││
│  │ ghi789...      │ Shop C      │ 3 days ago │ 🔴     │ [View] ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
│  Total: 156 devices | Active: 89 | Inactive: 67                 │
└─────────────────────────────────────────────────────────────────┘
```

**API Endpoints needed:**

```typescript
// GET /api/admin/devices
interface DeviceListResponse {
  devices: {
    id: string
    device_id: string
    merchant_id: string
    merchant_name: string
    app_version: string
    last_seen_at: string
    status: 'active' | 'inactive' | 'blocked'
    transaction_count: number
  }[]
  total: number
  page: number
  per_page: number
}

// GET /api/admin/devices/:id
interface DeviceDetailResponse {
  device: DeviceInfo
  recent_transactions: Transaction[]
  webhook_delivery_stats: {
    total: number
    successful: number
    failed: number
  }
}

// POST /api/admin/devices/:id/block
// POST /api/admin/devices/:id/unblock
```

---

### 2.2 Transaction Monitoring

**Features needed:**

```
┌─────────────────────────────────────────────────────────────────┐
│  ADMIN PANEL: SMS Transactions                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Filters: [Date Range ▼] [Status ▼] [Provider ▼] [Merchant ▼]   │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ Time       │ Client ID   │ Amount  │ Provider │ Status     ││
│  ├─────────────────────────────────────────────────────────────┤│
│  │ 12:45:23   │ abc-123...  │ 50,000  │ MTN      │ ✅ Matched ││
│  │ 12:44:15   │ def-456...  │ 25,000  │ Airtel   │ ⏳ Pending ││
│  │ 12:43:02   │ ghi-789...  │ 10,000  │ MTN      │ ❌ Failed  ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
│  [View Details] shows: SMS body, parsed data, matching record   │
└─────────────────────────────────────────────────────────────────┘
```

**API Endpoints:**

```typescript
// GET /api/admin/transactions
interface TransactionListParams {
  page?: number
  per_page?: number
  status?: 'pending' | 'matched' | 'failed'
  provider?: string
  merchant_id?: string
  date_from?: string
  date_to?: string
  client_transaction_id?: string  // For tracing
}

// GET /api/admin/transactions/:id/trace
// Returns full trace: MomoTerminal → Webhook → Matcher → Final status
interface TransactionTraceResponse {
  client_transaction_id: string
  server_transaction_id: string
  timeline: {
    timestamp: string
    event: string
    details: Record<string, unknown>
  }[]
  matched_record?: {
    table: string
    id: string
    type: string
  }
}
```

---

### 2.3 Webhook Health Dashboard

**Features needed:**

```
┌─────────────────────────────────────────────────────────────────┐
│  ADMIN PANEL: Webhook Health                                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Last 24 Hours:                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Received: 1,234  │  Processed: 1,230  │  Failed: 4      │   │
│  │  Avg Latency: 245ms  │  Duplicates Blocked: 12           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  Recent Errors:                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ 12:45 │ Signature mismatch │ device: abc123 │ [Details]  │   │
│  │ 12:30 │ Replay attack blocked │ nonce: xyz │ [Details]   │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  [View Logs] [Configure Alerts] [Test Webhook]                  │
└─────────────────────────────────────────────────────────────────┘
```

**API Endpoints:**

```typescript
// GET /api/admin/webhooks/stats
interface WebhookStatsResponse {
  period: string
  received: number
  processed: number
  failed: number
  duplicates_blocked: number
  replay_attacks_blocked: number
  avg_latency_ms: number
  error_breakdown: {
    signature_mismatch: number
    invalid_payload: number
    timeout: number
    other: number
  }
}

// GET /api/admin/webhooks/errors
interface WebhookErrorsResponse {
  errors: {
    timestamp: string
    error_type: string
    device_id: string
    details: string
    request_id: string
  }[]
}
```

---

### 2.4 Security Audit Log

**Features needed:**

```
┌─────────────────────────────────────────────────────────────────┐
│  ADMIN PANEL: Security Audit Log                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  [Filter by event type ▼] [Date range] [Search...]              │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Time     │ Event                │ Device    │ IP        │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │ 12:45:23 │ 🔴 Replay blocked    │ abc123    │ 1.2.3.4   │   │
│  │ 12:44:15 │ 🟡 Signature retry   │ def456    │ 5.6.7.8   │   │
│  │ 12:43:02 │ 🟢 Device registered │ ghi789    │ 9.10.11.12│   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Part 3: EasyMO Client Portal (Merchant Dashboard)

### 3.1 Transaction History with Real-time Updates

**Features needed:**

```
┌─────────────────────────────────────────────────────────────────┐
│  MY TRANSACTIONS                                    [Export CSV] │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Today's Summary:                                                │
│  ┌────────────┬────────────┬────────────┬────────────┐          │
│  │ Received   │ Sent       │ Pending    │ Total      │          │
│  │ RWF 250K   │ RWF 50K    │ RWF 10K    │ RWF 310K   │          │
│  └────────────┴────────────┴────────────┴────────────┘          │
│                                                                  │
│  Recent Transactions:                              🔴 Live       │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ 🟢 12:45 │ +50,000 RWF │ From: 078... │ MTN MoMo       │   │
│  │ 🟢 12:30 │ +25,000 RWF │ From: 073... │ Airtel Money   │   │
│  │ ⏳ 12:15 │ +10,000 RWF │ Pending verification...        │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  [Load More]                                                     │
└─────────────────────────────────────────────────────────────────┘
```

**Implementation:**

```typescript
// React component with Supabase Realtime
import { useEffect, useState } from 'react'
import { supabase } from '@/lib/supabase'

function TransactionList({ merchantId }: { merchantId: string }) {
  const [transactions, setTransactions] = useState<Transaction[]>([])

  useEffect(() => {
    // Initial fetch
    fetchTransactions()

    // Subscribe to realtime updates
    const channel = supabase
      .channel('merchant-transactions')
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'momo_transactions',
          filter: `merchant_id=eq.${merchantId}`
        },
        (payload) => {
          if (payload.eventType === 'INSERT') {
            setTransactions(prev => [payload.new as Transaction, ...prev])
            // Show notification
            showNotification('New payment received!')
          } else if (payload.eventType === 'UPDATE') {
            setTransactions(prev =>
              prev.map(t => t.id === payload.new.id ? payload.new as Transaction : t)
            )
          }
        }
      )
      .subscribe()

    return () => {
      supabase.removeChannel(channel)
    }
  }, [merchantId])

  // ... render
}
```

---

### 3.2 Device Status Widget

**Features needed:**

```
┌─────────────────────────────────────────────────────────────────┐
│  MY DEVICES                                                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ 📱 Samsung Galaxy A52                                    │    │
│  │    Status: 🟢 Online (last seen: just now)              │    │
│  │    App Version: 2.0.1                                    │    │
│  │    Today: 45 transactions synced                         │    │
│  │    [View Details] [Rename]                               │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ 📱 Xiaomi Redmi Note 10                                  │    │
│  │    Status: 🟡 Offline (last seen: 2 hours ago)          │    │
│  │    App Version: 2.0.0 ⚠️ Update available               │    │
│  │    [View Details] [Rename]                               │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  [+ Add New Device]                                              │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.3 Webhook Configuration (for advanced users)

**Features needed:**

```
┌─────────────────────────────────────────────────────────────────┐
│  WEBHOOK SETTINGS                                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Forward SMS notifications to your system:                       │
│                                                                  │
│  Endpoint URL:                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ https://api.myshop.com/webhooks/momo                     │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  HMAC Secret: ●●●●●●●●●●●●●●●●  [Show] [Regenerate]             │
│                                                                  │
│  Events to forward:                                              │
│  ☑️ Payment received                                             │
│  ☑️ Payment sent                                                 │
│  ☐ Balance inquiry                                               │
│                                                                  │
│  [Test Webhook] [Save Changes]                                   │
│                                                                  │
│  Recent Deliveries:                                              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ 12:45 │ ✅ 200 OK │ 145ms │ payment_received            │   │
│  │ 12:30 │ ✅ 200 OK │ 132ms │ payment_received            │   │
│  │ 12:15 │ ❌ 500    │ 2.1s  │ payment_received [Retry]    │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.4 Transaction Search & Trace

**Features needed:**

```
┌─────────────────────────────────────────────────────────────────┐
│  FIND TRANSACTION                                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Search by:                                                      │
│  ○ Transaction ID   ○ Phone Number   ○ Amount   ○ Date Range    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Enter transaction ID or reference...                     │    │
│  └─────────────────────────────────────────────────────────┘    │
│  [Search]                                                        │
│                                                                  │
│  Result:                                                         │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Transaction Found ✅                                      │   │
│  │                                                           │   │
│  │ Amount: 50,000 RWF                                        │   │
│  │ From: 078XXXXXXX (Jean Doe)                               │   │
│  │ Provider: MTN MoMo                                        │   │
│  │ Reference: TXN123456789                                   │   │
│  │ Time: Dec 3, 2025 12:45:23                                │   │
│  │ Status: ✅ Confirmed                                      │   │
│  │                                                           │   │
│  │ Trace ID: abc-123-def-456                                 │   │
│  │ [View Full Trace] [Download Receipt]                      │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Part 4: Database Schema Updates (EasyMO)

### Required Tables/Columns

```sql
-- Already created in MomoTerminal migration, ensure exists in EasyMO:

-- 1. Add client_transaction_id to momo_transactions (if not exists)
ALTER TABLE momo_transactions 
ADD COLUMN IF NOT EXISTS client_transaction_id UUID UNIQUE;

-- 2. Webhook nonces table
CREATE TABLE IF NOT EXISTS webhook_nonces (
    nonce TEXT PRIMARY KEY,
    device_id TEXT NOT NULL,
    received_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

-- 3. Idempotency keys table
CREATE TABLE IF NOT EXISTS idempotency_keys (
    key TEXT PRIMARY KEY,
    result JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

-- 4. Security audit log
CREATE TABLE IF NOT EXISTS security_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type TEXT NOT NULL,
    device_id TEXT,
    ip_address INET,
    details JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Webhook delivery stats (for admin dashboard)
CREATE TABLE IF NOT EXISTS webhook_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    received INTEGER DEFAULT 0,
    processed INTEGER DEFAULT 0,
    failed INTEGER DEFAULT 0,
    duplicates_blocked INTEGER DEFAULT 0,
    replay_attacks_blocked INTEGER DEFAULT 0,
    avg_latency_ms INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for efficient queries
CREATE INDEX idx_audit_log_created ON security_audit_log(created_at DESC);
CREATE INDEX idx_audit_log_device ON security_audit_log(device_id);
CREATE INDEX idx_webhook_stats_period ON webhook_stats(period_start DESC);
```

---

## Part 5: Implementation Checklist

### Backend (Week 1-2)

- [ ] Add nonce validation to `momo-sms-webhook`
- [ ] Standardize headers to `X-Momo-*` in all functions
- [ ] Create `_shared/idempotency.ts` helper
- [ ] Update payload interface to include `client_transaction_id`
- [ ] Create `cleanup-expired` cron function
- [ ] Add security audit logging
- [ ] Create webhook stats aggregation

### Admin Panel (Week 2-3)

- [ ] Device management page
- [ ] Transaction monitoring with trace view
- [ ] Webhook health dashboard
- [ ] Security audit log viewer
- [ ] Alert configuration

### Client Portal (Week 3-4)

- [ ] Real-time transaction list with Supabase Realtime
- [ ] Device status widget
- [ ] Webhook configuration UI
- [ ] Transaction search & trace
- [ ] Export functionality (CSV/PDF)

---

## API Summary

| Endpoint | Method | Purpose | Panel |
|----------|--------|---------|-------|
| `/api/admin/devices` | GET | List all devices | Admin |
| `/api/admin/devices/:id` | GET | Device details | Admin |
| `/api/admin/devices/:id/block` | POST | Block device | Admin |
| `/api/admin/transactions` | GET | List transactions | Admin |
| `/api/admin/transactions/:id/trace` | GET | Full trace | Admin |
| `/api/admin/webhooks/stats` | GET | Webhook health | Admin |
| `/api/admin/webhooks/errors` | GET | Recent errors | Admin |
| `/api/merchant/transactions` | GET | Merchant's transactions | Client |
| `/api/merchant/devices` | GET | Merchant's devices | Client |
| `/api/merchant/webhooks` | GET/PUT | Webhook config | Client |
| `/api/merchant/webhooks/test` | POST | Test webhook | Client |

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Replay attacks blocked | 100% | Security audit log |
| Duplicate transactions | 0% | Idempotency key hits |
| Webhook delivery success | >99% | Webhook stats |
| End-to-end trace coverage | 100% | client_transaction_id presence |
| Admin response time | <500ms | API monitoring |
| Real-time update latency | <2s | Client portal metrics |
