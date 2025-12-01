# Complete Database & Backend Implementation Plan

**Date:** December 1, 2025  
**Status:** 📋 Implementation Guide

---

## 🗄️ DATABASE ARCHITECTURE

### Local Database (Room - Android)

**Purpose:** Offline-first architecture, data persistence

#### Tables:

1. **transactions** ✅ EXISTS
   - Stores SMS transaction data
   - Sync status tracking
   - Offline queue for webhook delivery

2. **webhook_configs** ✅ EXISTS
   - Webhook endpoint configurations
   - API keys and HMAC secrets
   - Phone number routing

3. **sms_delivery_logs** ✅ EXISTS
   - Delivery tracking
   - Retry management
   - Response logging

### Cloud Database (Supabase - PostgreSQL)

**Purpose:** Cloud sync, multi-device, analytics, backup

#### Tables Needed:

1. **otp_codes** ✅ EXISTS
   - WhatsApp OTP authentication
   - Rate limiting (5 per hour)
   - Auto-expiry (5 minutes)

2. **user_profiles** ✅ EXISTS  
   - Extends auth.users
   - Merchant information
   - Business details

3. **transactions** ⚠️ NEEDS CREATION
   - Cloud copy of local transactions
   - Analytics and reporting
   - Multi-device sync

4. **webhook_configs** ⚠️ NEEDS CREATION
   - Cloud-synced webhook configs
   - Multi-device consistency

5. **sms_delivery_logs** ⚠️ NEEDS CREATION
   - Cloud delivery tracking
   - Historical analytics

6. **devices** ⚠️ NEEDS CREATION
   - Device registration
   - Push notification tokens
   - Device-specific settings

7. **merchant_settings** ⚠️ NEEDS CREATION
   - Business configuration
   - Payment provider settings
   - Notification preferences

8. **analytics_events** ⚠️ NEEDS CREATION
   - User behavior tracking
   - Transaction metrics
   - Error reporting

---

## 📊 COMPLETE SUPABASE SCHEMA

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    SUPABASE POSTGRES                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  AUTH (Supabase Built-in)                                  │
│    └── auth.users                                          │
│                                                             │
│  USER MANAGEMENT                                           │
│    ├── user_profiles (extends auth.users)                 │
│    ├── devices                                             │
│    └── merchant_settings                                   │
│                                                             │
│  AUTHENTICATION                                            │
│    ├── otp_codes                                          │
│    └── otp_request_logs                                   │
│                                                             │
│  TRANSACTIONS (Core Business Logic)                        │
│    ├── transactions                                        │
│    ├── webhook_configs                                     │
│    └── sms_delivery_logs                                  │
│                                                             │
│  ANALYTICS & MONITORING                                    │
│    ├── analytics_events                                    │
│    └── error_logs                                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ IMPLEMENTATION PLAN

### Phase 1: Core Transaction Tables (CRITICAL)

Create tables that sync with local Room database:

#### 1. transactions table

```sql
-- Supabase transactions table
CREATE TABLE IF NOT EXISTS public.transactions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Transaction details
    sender VARCHAR(50) NOT NULL,
    body TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    
    -- Parsed data
    amount DECIMAL(15, 2),
    currency VARCHAR(10) DEFAULT 'GHS',
    transaction_id VARCHAR(100),
    merchant_code VARCHAR(50),
    
    -- Provider information
    provider VARCHAR(50),
    provider_type VARCHAR(20),
    
    -- Metadata
    device_id UUID REFERENCES devices(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    synced_at TIMESTAMPTZ,
    
    -- Local database reference
    local_id BIGINT
);

-- Indexes for performance
CREATE INDEX idx_transactions_user_id ON public.transactions(user_id);
CREATE INDEX idx_transactions_status ON public.transactions(status);
CREATE INDEX idx_transactions_timestamp ON public.transactions(timestamp DESC);
CREATE INDEX idx_transactions_provider ON public.transactions(provider);
CREATE INDEX idx_transactions_device ON public.transactions(device_id);

-- RLS Policies
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own transactions" ON public.transactions
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own transactions" ON public.transactions
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own transactions" ON public.transactions
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
```

#### 2. webhook_configs table

```sql
-- Webhook configurations (synced from device)
CREATE TABLE IF NOT EXISTS public.webhook_configs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Webhook details
    name VARCHAR(100) NOT NULL,
    url TEXT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    
    -- Security
    api_key TEXT NOT NULL, -- Encrypted
    hmac_secret TEXT NOT NULL, -- Encrypted
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    last_success_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    failure_count INT DEFAULT 0,
    
    -- Metadata
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Local reference
    local_id BIGINT
);

-- Indexes
CREATE INDEX idx_webhook_configs_user_id ON public.webhook_configs(user_id);
CREATE INDEX idx_webhook_configs_active ON public.webhook_configs(is_active);

-- RLS
ALTER TABLE public.webhook_configs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own webhooks" ON public.webhook_configs
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
```

#### 3. sms_delivery_logs table

```sql
-- SMS delivery tracking
CREATE TABLE IF NOT EXISTS public.sms_delivery_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    webhook_id UUID REFERENCES webhook_configs(id) ON DELETE CASCADE,
    
    -- SMS details
    phone_number VARCHAR(20) NOT NULL,
    sender VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    
    -- Delivery tracking
    status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'sent', 'failed', 'delivered')),
    response_code INT,
    response_body TEXT,
    retry_count INT DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW(),
    sent_at TIMESTAMPTZ,
    
    -- Local reference
    local_id BIGINT
);

-- Indexes
CREATE INDEX idx_sms_logs_user_id ON public.sms_delivery_logs(user_id);
CREATE INDEX idx_sms_logs_status ON public.sms_delivery_logs(status);
CREATE INDEX idx_sms_logs_created ON public.sms_delivery_logs(created_at DESC);

-- RLS
ALTER TABLE public.sms_delivery_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own logs" ON public.sms_delivery_logs
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);
```

### Phase 2: Device Management

#### 4. devices table

```sql
-- Device registration and management
CREATE TABLE IF NOT EXISTS public.devices (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Device info
    device_id VARCHAR(100) UNIQUE NOT NULL,
    device_name VARCHAR(100),
    device_model VARCHAR(100),
    os_version VARCHAR(50),
    app_version VARCHAR(20),
    
    -- Push notifications
    fcm_token TEXT,
    
    -- Security
    last_ip INET,
    last_location POINT,
    is_trusted BOOLEAN DEFAULT FALSE,
    
    -- Timestamps
    registered_at TIMESTAMPTZ DEFAULT NOW(),
    last_active_at TIMESTAMPTZ DEFAULT NOW(),
    
    CONSTRAINT unique_user_device UNIQUE(user_id, device_id)
);

-- Indexes
CREATE INDEX idx_devices_user_id ON public.devices(user_id);
CREATE INDEX idx_devices_last_active ON public.devices(last_active_at DESC);

-- RLS
ALTER TABLE public.devices ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own devices" ON public.devices
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can register devices" ON public.devices
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own devices" ON public.devices
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
```

### Phase 3: Merchant Configuration

#### 5. merchant_settings table

```sql
-- Merchant-specific settings and configuration
CREATE TABLE IF NOT EXISTS public.merchant_settings (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Business info
    business_name VARCHAR(200),
    business_type VARCHAR(50),
    merchant_code VARCHAR(50) UNIQUE,
    
    -- Payment providers
    preferred_provider VARCHAR(50),
    enabled_providers JSONB DEFAULT '["MTN", "Vodafone", "AirtelTigo"]'::jsonb,
    
    -- Notification settings
    email_notifications BOOLEAN DEFAULT TRUE,
    sms_notifications BOOLEAN DEFAULT FALSE,
    push_notifications BOOLEAN DEFAULT TRUE,
    
    -- Transaction limits
    daily_transaction_limit DECIMAL(15, 2),
    single_transaction_limit DECIMAL(15, 2),
    
    -- Features
    nfc_enabled BOOLEAN DEFAULT TRUE,
    auto_sync_enabled BOOLEAN DEFAULT TRUE,
    offline_mode_enabled BOOLEAN DEFAULT TRUE,
    
    -- Metadata
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS
ALTER TABLE public.merchant_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own settings" ON public.merchant_settings
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
```

### Phase 4: Analytics & Monitoring

#### 6. analytics_events table

```sql
-- Track user behavior and app usage
CREATE TABLE IF NOT EXISTS public.analytics_events (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    
    -- Event details
    event_name VARCHAR(100) NOT NULL,
    event_category VARCHAR(50),
    event_properties JSONB DEFAULT '{}'::jsonb,
    
    -- Context
    screen_name VARCHAR(100),
    session_id VARCHAR(100),
    
    -- Metadata
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for analytics queries
CREATE INDEX idx_analytics_event_name ON public.analytics_events(event_name);
CREATE INDEX idx_analytics_user_id ON public.analytics_events(user_id);
CREATE INDEX idx_analytics_created ON public.analytics_events(created_at DESC);
CREATE INDEX idx_analytics_category ON public.analytics_events(event_category);

-- RLS (only inserts allowed, service role for reads)
ALTER TABLE public.analytics_events ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can log own events" ON public.analytics_events
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Service role can read all events" ON public.analytics_events
    FOR SELECT
    TO service_role
    USING (true);
```

#### 7. error_logs table

```sql
-- Application error logging
CREATE TABLE IF NOT EXISTS public.error_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    
    -- Error details
    error_type VARCHAR(100) NOT NULL,
    error_message TEXT NOT NULL,
    stack_trace TEXT,
    error_code VARCHAR(50),
    
    -- Context
    screen_name VARCHAR(100),
    component VARCHAR(100),
    app_version VARCHAR(20),
    
    -- Environment
    os_version VARCHAR(50),
    network_state VARCHAR(20),
    
    -- Metadata
    created_at TIMESTAMPTZ DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    notes TEXT
);

-- Indexes
CREATE INDEX idx_error_logs_type ON public.error_logs(error_type);
CREATE INDEX idx_error_logs_created ON public.error_logs(created_at DESC);
CREATE INDEX idx_error_logs_unresolved ON public.error_logs(resolved_at) WHERE resolved_at IS NULL;

-- RLS
ALTER TABLE public.error_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can log own errors" ON public.error_logs
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Service role can read all errors" ON public.error_logs
    FOR SELECT
    TO service_role
    USING (true);
```

---

## 🔄 HELPER FUNCTIONS & TRIGGERS

### Auto-update timestamps

```sql
-- Trigger function to update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to all tables with updated_at
CREATE TRIGGER update_transactions_updated_at
    BEFORE UPDATE ON public.transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_webhook_configs_updated_at
    BEFORE UPDATE ON public.webhook_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_merchant_settings_updated_at
    BEFORE UPDATE ON public.merchant_settings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_profiles_updated_at
    BEFORE UPDATE ON public.user_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### Transaction statistics function

```sql
-- Get transaction statistics for a user
CREATE OR REPLACE FUNCTION get_transaction_stats(p_user_id UUID)
RETURNS TABLE (
    total_count BIGINT,
    total_amount DECIMAL,
    pending_count BIGINT,
    sent_count BIGINT,
    failed_count BIGINT,
    today_count BIGINT,
    today_amount DECIMAL,
    this_week_count BIGINT,
    this_month_count BIGINT
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(*)::BIGINT AS total_count,
        COALESCE(SUM(amount), 0) AS total_amount,
        COUNT(*) FILTER (WHERE status = 'PENDING')::BIGINT AS pending_count,
        COUNT(*) FILTER (WHERE status = 'SENT')::BIGINT AS sent_count,
        COUNT(*) FILTER (WHERE status = 'FAILED')::BIGINT AS failed_count,
        COUNT(*) FILTER (WHERE DATE(timestamp) = CURRENT_DATE)::BIGINT AS today_count,
        COALESCE(SUM(amount) FILTER (WHERE DATE(timestamp) = CURRENT_DATE), 0) AS today_amount,
        COUNT(*) FILTER (WHERE timestamp >= DATE_TRUNC('week', NOW()))::BIGINT AS this_week_count,
        COUNT(*) FILTER (WHERE timestamp >= DATE_TRUNC('month', NOW()))::BIGINT AS this_month_count
    FROM public.transactions
    WHERE user_id = p_user_id;
END;
$$;
```

### Device activity tracking

```sql
-- Update device last active timestamp
CREATE OR REPLACE FUNCTION update_device_activity(
    p_device_id VARCHAR,
    p_user_id UUID
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    UPDATE public.devices
    SET last_active_at = NOW()
    WHERE device_id = p_device_id AND user_id = p_user_id;
END;
$$;
```

---

## 📡 SUPABASE EDGE FUNCTIONS (Serverless)

### 1. send-otp (EXISTING)

**Purpose:** Send WhatsApp OTP codes  
**Status:** ✅ Already implemented

### 2. verify-otp (EXISTING)

**Purpose:** Verify OTP codes  
**Status:** ✅ Already implemented

### 3. sync-transactions (NEW)

**Purpose:** Batch sync transactions from device to cloud

```typescript
// supabase/functions/sync-transactions/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

interface Transaction {
  local_id: number
  sender: string
  body: string
  timestamp: string
  status: string
  amount?: number
  currency?: string
  transaction_id?: string
  merchant_code?: string
  provider?: string
  provider_type?: string
}

serve(async (req) => {
  try {
    // Get auth token
    const authHeader = req.headers.get('Authorization')!
    const token = authHeader.replace('Bearer ', '')
    
    // Create Supabase client
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      { global: { headers: { Authorization: authHeader } } }
    )
    
    // Get user
    const { data: { user }, error: authError } = await supabase.auth.getUser(token)
    if (authError) throw authError
    
    // Parse request body
    const { transactions, device_id }: { 
      transactions: Transaction[], 
      device_id: string 
    } = await req.json()
    
    // Insert transactions
    const transactionsToInsert = transactions.map(t => ({
      user_id: user.id,
      device_id: device_id,
      local_id: t.local_id,
      sender: t.sender,
      body: t.body,
      timestamp: t.timestamp,
      status: t.status,
      amount: t.amount,
      currency: t.currency || 'GHS',
      transaction_id: t.transaction_id,
      merchant_code: t.merchant_code,
      provider: t.provider,
      provider_type: t.provider_type,
      synced_at: new Date().toISOString()
    }))
    
    const { data, error } = await supabase
      .from('transactions')
      .upsert(transactionsToInsert, {
        onConflict: 'user_id,local_id',
        ignoreDuplicates: false
      })
      .select()
    
    if (error) throw error
    
    return new Response(
      JSON.stringify({ 
        success: true, 
        synced: data.length,
        message: `${data.length} transactions synced` 
      }),
      { headers: { "Content-Type": "application/json" }, status: 200 }
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ success: false, error: error.message }),
      { headers: { "Content-Type": "application/json" }, status: 400 }
    )
  }
})
```

### 4. webhook-relay (NEW)

**Purpose:** Relay SMS to configured webhooks with HMAC signing

```typescript
// supabase/functions/webhook-relay/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { createHmac } from "https://deno.land/std@0.160.0/node/crypto.ts"

serve(async (req) => {
  try {
    const authHeader = req.headers.get('Authorization')!
    const token = authHeader.replace('Bearer ', '')
    
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      { global: { headers: { Authorization: authHeader } } }
    )
    
    const { data: { user } } = await supabase.auth.getUser(token)
    if (!user) throw new Error('Unauthorized')
    
    const { webhook_id, sms_data } = await req.json()
    
    // Get webhook config
    const { data: webhook } = await supabase
      .from('webhook_configs')
      .select('*')
      .eq('id', webhook_id)
      .single()
    
    if (!webhook || !webhook.is_active) {
      throw new Error('Webhook not found or inactive')
    }
    
    // Prepare payload
    const payload = {
      phone_number: webhook.phone_number,
      sender: sms_data.sender,
      message: sms_data.message,
      timestamp: sms_data.timestamp,
      ...sms_data.parsed_data
    }
    
    // Generate HMAC signature
    const signature = createHmac('sha256', webhook.hmac_secret)
      .update(JSON.stringify(payload))
      .digest('hex')
    
    // Send to webhook
    const response = await fetch(webhook.url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Webhook-Signature': signature,
        'Authorization': `Bearer ${webhook.api_key}`
      },
      body: JSON.stringify(payload)
    })
    
    // Log delivery
    await supabase.from('sms_delivery_logs').insert({
      user_id: user.id,
      webhook_id: webhook.id,
      phone_number: webhook.phone_number,
      sender: sms_data.sender,
      message: sms_data.message,
      status: response.ok ? 'sent' : 'failed',
      response_code: response.status,
      response_body: await response.text(),
      sent_at: new Date().toISOString()
    })
    
    return new Response(
      JSON.stringify({ success: response.ok, status: response.status }),
      { headers: { "Content-Type": "application/json" } }
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ success: false, error: error.message }),
      { headers: { "Content-Type": "application/json" }, status: 400 }
    )
  }
})
```

---

## 📲 ANDROID APP INTEGRATION

### Update SupabaseClient Configuration

```kotlin
// app/src/main/java/com/momoterminal/di/SupabaseModule.kt

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    
    @Provides
    @Singleton
    fun provideSupabaseClient(
        @ApplicationContext context: Context
    ): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }
}
```

### Transaction Sync Service

```kotlin
// app/src/main/java/com/momoterminal/sync/TransactionSyncService.kt

@HiltWorker
class TransactionSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val supabaseClient: SupabaseClient,
    private val transactionDao: TransactionDao,
    private val deviceInfo: DeviceInfo
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Get pending transactions
            val pending = transactionDao.getPendingSync()
            
            if (pending.isEmpty()) {
                return Result.success()
            }
            
            // Sync to Supabase
            val response = supabaseClient.functions.invoke(
                "sync-transactions",
                body = mapOf(
                    "transactions" to pending.map { it.toSyncDto() },
                    "device_id" to deviceInfo.getDeviceId()
                )
            )
            
            if (response.status == HttpStatusCode.OK) {
                // Mark as synced
                pending.forEach { transaction ->
                    transactionDao.updateStatus(
                        transaction.id,
                        "SENT",
                        System.currentTimeMillis()
                    )
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("TransactionSync", "Sync failed", e)
            Result.retry()
        }
    }
}
```

---

## 📋 IMPLEMENTATION CHECKLIST

### Phase 1: Core Setup (2-3 hours)

```bash
# 1. Create migration files
cd /Users/jeanbosco/workspace/MomoTerminal/supabase/migrations

# 2. Create transactions table
touch 20251201180000_create_transactions_table.sql

# 3. Create webhook_configs table
touch 20251201180100_create_webhook_configs_table.sql

# 4. Create sms_delivery_logs table
touch 20251201180200_create_sms_delivery_logs_table.sql

# 5. Create devices table
touch 20251201180300_create_devices_table.sql

# 6. Create merchant_settings table
touch 20251201180400_create_merchant_settings_table.sql

# 7. Create analytics tables
touch 20251201180500_create_analytics_tables.sql

# 8. Create helper functions
touch 20251201180600_create_helper_functions.sql
```

### Phase 2: Edge Functions (2-3 hours)

```bash
# 1. Create sync function
cd supabase/functions
mkdir sync-transactions
touch sync-transactions/index.ts

# 2. Create webhook relay
mkdir webhook-relay
touch webhook-relay/index.ts

# 3. Deploy
supabase functions deploy sync-transactions
supabase functions deploy webhook-relay
```

### Phase 3: Android Integration (4-6 hours)

```kotlin
// 1. Update Supabase models
// 2. Implement sync service
// 3. Add background worker
// 4. Update repositories
// 5. Test offline/online sync
```

---

## 🚀 DEPLOYMENT COMMANDS

```bash
# 1. Link to Supabase project
supabase link --project-ref YOUR_PROJECT_REF

# 2. Apply migrations
supabase db push

# 3. Deploy Edge Functions
supabase functions deploy sync-transactions
supabase functions deploy webhook-relay

# 4. Verify deployment
supabase db remote status

# 5. Run tests
supabase test db
```

---

## 📊 MONITORING & MAINTENANCE

### Automated Jobs

```sql
-- Schedule cleanup job (every hour)
SELECT cron.schedule(
    'cleanup-expired-otps',
    '0 * * * *', -- Every hour
    $$SELECT cleanup_expired_otps()$$
);

-- Schedule analytics aggregation (daily at midnight)
SELECT cron.schedule(
    'aggregate-analytics',
    '0 0 * * *', -- Daily at midnight
    $$
    INSERT INTO analytics_daily_summary
    SELECT DATE(created_at), event_name, COUNT(*)
    FROM analytics_events
    WHERE DATE(created_at) = CURRENT_DATE - 1
    GROUP BY DATE(created_at), event_name
    $$
);
```

---

## ✅ VALIDATION CHECKLIST

After implementation:

- [ ] All tables created in Supabase
- [ ] RLS policies working correctly
- [ ] Edge Functions deployed
- [ ] Android app can sync transactions
- [ ] Webhooks working with HMAC signatures
- [ ] Device registration functional
- [ ] Analytics tracking operational
- [ ] Error logging working
- [ ] Automated cleanup jobs running
- [ ] Performance benchmarks met (< 200ms queries)

---

**Status:** Ready to implement  
**Estimated Time:** 8-12 hours  
**Priority:** HIGH for production readiness
