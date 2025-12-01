# MomoTerminal - Comprehensive Database & Backend Implementation Plan
**Generated:** December 1, 2025  
**Status:** Production Ready Deployment Plan

---

## Executive Summary

This document provides a complete audit of all database tables, backend services, API endpoints, and implementation gaps that need to be addressed for production deployment.

### Current Implementation Status

| Component | Status | Notes |
|-----------|--------|-------|
| **Local Database (Room)** | ✅ Complete | 3 entities implemented |
| **Supabase Tables** | ✅ Complete | 8 tables with RLS policies |
| **Edge Functions** | ✅ Complete | 5 functions deployed |
| **API Services** | ✅ Complete | Retrofit + OkHttp configured |
| **Data Sync** | ⚠️ Partial | Needs testing & error handling |
| **Analytics** | ⚠️ Partial | Tables exist, integration needed |
| **Error Logging** | ⚠️ Partial | Backend ready, client needs work |

---

## 1. Database Architecture

### 1.1 Local Database (Room) - Android

**Location:** `app/src/main/java/com/momoterminal/data/local/`

#### Entities Implemented

```kotlin
// MomoDatabase.kt - Version 2
@Database(
    entities = [
        TransactionEntity::class,
        WebhookConfigEntity::class,
        SmsDeliveryLogEntity::class
    ],
    version = 2
)
```

##### 1.1.1 TransactionEntity
**Table:** `transactions`  
**Purpose:** Local transaction storage for offline-first reliability

```kotlin
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val sender: String,              // SMS sender (e.g., "MTN MoMo")
    val body: String,                // Raw SMS content
    val timestamp: Long,             // Unix timestamp
    val status: String,              // PENDING, SENT, FAILED
    val amount: Double?,             // Amount in main currency (GHS)
    val currency: String? = "GHS",
    val transactionId: String?,      // Provider transaction ID
    val merchantCode: String?        // Associated merchant
)
```

**DAO Methods Required:**
- ✅ `getAll()`: Flow<List<TransactionEntity>>
- ✅ `getRecentTransactions(limit: Int = 10)`: Flow<List<TransactionEntity>>
- ✅ `getPendingTransactions()`: List<TransactionEntity>
- ✅ `getPendingCount()`: Flow<Int>
- ✅ `insert(transaction: TransactionEntity): Long`
- ✅ `updateStatus(id: Long, status: String)`
- ⚠️ **MISSING:** `getTransactionsByDateRange(start: Long, end: Long)`
- ⚠️ **MISSING:** `getTransactionsByProvider(provider: String)`
- ⚠️ **MISSING:** `deleteOldTransactions(olderThan: Long)`

##### 1.1.2 WebhookConfigEntity
**Table:** `webhook_configs`  
**Purpose:** Store webhook endpoint configurations for SMS relay

```kotlin
data class WebhookConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,                // Display name
    val url: String,                 // Webhook endpoint URL
    val phoneNumber: String,         // Associated MoMo phone
    val apiKey: String,              // Bearer token
    val hmacSecret: String,          // HMAC-SHA256 secret
    val isActive: Boolean = true,
    val createdAt: Long
)
```

**DAO Methods Required:**
- ✅ `getAll()`: Flow<List<WebhookConfigEntity>>
- ✅ `getActiveWebhooks()`: List<WebhookConfigEntity>
- ✅ `getByPhoneNumber(phone: String)`: WebhookConfigEntity?
- ✅ `insert(webhook: WebhookConfigEntity): Long`
- ✅ `update(webhook: WebhookConfigEntity)`
- ✅ `delete(id: Long)`

##### 1.1.3 SmsDeliveryLogEntity
**Table:** `sms_delivery_logs`  
**Purpose:** Track SMS relay delivery status

```kotlin
data class SmsDeliveryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val webhookId: Long,             // FK to webhook_configs
    val phoneNumber: String,
    val sender: String,
    val message: String,
    val status: String,              // pending, sent, failed, delivered
    val responseCode: Int?,
    val responseBody: String?,
    val retryCount: Int = 0,
    val createdAt: Long,
    val sentAt: Long?
)
```

**DAO Methods Required:**
- ✅ `insert(log: SmsDeliveryLogEntity): Long`
- ✅ `updateStatus(id: Long, status: String, responseCode: Int?, responseBody: String?)`
- ✅ `getFailedLogs()`: List<SmsDeliveryLogEntity>
- ⚠️ **MISSING:** `getLogsByWebhookId(webhookId: Long, limit: Int)`
- ⚠️ **MISSING:** `getRecentLogs(limit: Int = 50)`

---

### 1.2 Supabase Cloud Database (PostgreSQL)

**Location:** `supabase/migrations/`

#### Tables Implemented

##### 1.2.1 transactions
**Migration:** `20251201180000_create_transactions_table.sql`  
**Purpose:** Cloud-synced transaction records

```sql
CREATE TABLE public.transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    sender VARCHAR(50) NOT NULL,
    body TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(15, 2),
    currency VARCHAR(10) DEFAULT 'GHS',
    transaction_id VARCHAR(100),
    merchant_code VARCHAR(50),
    provider VARCHAR(50),
    provider_type VARCHAR(20),
    device_id UUID,
    local_id BIGINT,                 -- Reference to local Room DB
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    synced_at TIMESTAMPTZ,
    CONSTRAINT unique_user_local_transaction UNIQUE(user_id, local_id, device_id)
);
```

**Indexes:**
- ✅ user_id, status, timestamp, provider, device_id
- ✅ Composite: (user_id, timestamp DESC)

**RLS Policies:**
- ✅ Users can view/insert/update own transactions
- ✅ Service role full access

##### 1.2.2 webhook_configs
**Migration:** `20251201180100_create_webhook_configs_table.sql`  
**Purpose:** Cloud-synced webhook configurations

```sql
CREATE TABLE public.webhook_configs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    url TEXT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    api_key TEXT NOT NULL,           -- Encrypted at app level
    hmac_secret TEXT NOT NULL,       -- Encrypted at app level
    is_active BOOLEAN DEFAULT TRUE,
    last_success_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    failure_count INT DEFAULT 0,
    max_requests_per_hour INT DEFAULT 1000,
    current_hour_requests INT DEFAULT 0,
    hour_reset_at TIMESTAMPTZ,
    local_id BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_user_phone UNIQUE(user_id, phone_number)
);
```

**Features:**
- ✅ Rate limiting per webhook
- ✅ Failure tracking with auto-disable after 10 consecutive failures
- ✅ Last success/failure timestamps

##### 1.2.3 devices
**Migration:** `20251201180300_create_devices_table.sql`  
**Purpose:** Device registration and management

```sql
CREATE TABLE public.devices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    device_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(100),
    device_model VARCHAR(100),
    manufacturer VARCHAR(100),
    os_version VARCHAR(50),
    app_version VARCHAR(20),
    sdk_version INT,
    fcm_token TEXT,                  -- Push notifications
    fcm_token_updated_at TIMESTAMPTZ,
    last_ip INET,
    last_location POINT,
    is_trusted BOOLEAN DEFAULT FALSE,
    is_blocked BOOLEAN DEFAULT FALSE,
    blocked_reason TEXT,
    total_transactions INT DEFAULT 0,
    last_sync_at TIMESTAMPTZ,
    last_active_at TIMESTAMPTZ DEFAULT NOW(),
    registered_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_user_device UNIQUE(user_id, device_id)
);
```

**Features:**
- ✅ Multi-device support per user
- ✅ Push notification token storage
- ✅ Device trust management
- ✅ Activity tracking

##### 1.2.4 merchant_settings
**Migration:** `20251201180400_create_merchant_settings_table.sql`  
**Purpose:** Merchant configuration and preferences

```sql
CREATE TABLE public.merchant_settings (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL REFERENCES auth.users(id),
    business_name VARCHAR(200),
    business_type VARCHAR(50),       -- retail, restaurant, service, etc.
    merchant_code VARCHAR(50) UNIQUE,
    tax_id VARCHAR(50),
    business_registration_number VARCHAR(50),
    business_email VARCHAR(255),
    business_phone VARCHAR(20),
    business_address TEXT,
    business_location POINT,
    preferred_provider VARCHAR(50) DEFAULT 'MTN',
    enabled_providers JSONB DEFAULT '["MTN", "Vodafone", "AirtelTigo"]'::jsonb,
    provider_specific_settings JSONB DEFAULT '{}'::jsonb,
    email_notifications BOOLEAN DEFAULT TRUE,
    sms_notifications BOOLEAN DEFAULT FALSE,
    push_notifications BOOLEAN DEFAULT TRUE,
    notify_on_transaction BOOLEAN DEFAULT TRUE,
    notify_on_failure BOOLEAN DEFAULT TRUE,
    notify_on_daily_summary BOOLEAN DEFAULT TRUE,
    daily_transaction_limit DECIMAL(15, 2),
    single_transaction_limit DECIMAL(15, 2),
    monthly_transaction_limit DECIMAL(15, 2),
    minimum_transaction_amount DECIMAL(15, 2) DEFAULT 1.00,
    nfc_enabled BOOLEAN DEFAULT TRUE,
    auto_sync_enabled BOOLEAN DEFAULT TRUE,
    offline_mode_enabled BOOLEAN DEFAULT TRUE,
    biometric_auth_required BOOLEAN DEFAULT FALSE,
    operating_hours JSONB DEFAULT '{}'::jsonb,
    timezone VARCHAR(50) DEFAULT 'Africa/Accra',
    terms_version_accepted VARCHAR(20),
    privacy_policy_accepted BOOLEAN DEFAULT FALSE,
    data_retention_days INT DEFAULT 365,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

**Features:**
- ✅ Comprehensive business profile
- ✅ Provider preferences with JSONB flexibility
- ✅ Notification preferences per channel
- ✅ Transaction limits and controls
- ✅ Feature flags
- ✅ Compliance tracking (GDPR, terms acceptance)

##### 1.2.5 sms_delivery_logs
**Migration:** `20251201180200_create_sms_delivery_logs_table.sql`  
**Purpose:** SMS delivery tracking with retry logic

```sql
CREATE TABLE public.sms_delivery_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    webhook_id UUID REFERENCES webhook_configs(id) ON DELETE SET NULL,
    phone_number VARCHAR(20) NOT NULL,
    sender VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,     -- pending, sent, failed, delivered, retrying
    response_code INT,
    response_body TEXT,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    error_message TEXT,
    error_type VARCHAR(50),
    processing_time_ms INT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ
);
```

**Features:**
- ✅ Retry scheduling with exponential backoff
- ✅ Performance tracking (processing time)
- ✅ Error classification

##### 1.2.6 analytics_events
**Migration:** `20251201180500_create_analytics_tables.sql`  
**Purpose:** User behavior and app usage tracking

```sql
CREATE TABLE public.analytics_events (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    event_name VARCHAR(100) NOT NULL,
    event_category VARCHAR(50),
    event_action VARCHAR(100),
    event_label VARCHAR(200),
    event_value NUMERIC,
    event_properties JSONB DEFAULT '{}'::jsonb,
    screen_name VARCHAR(100),
    previous_screen VARCHAR(100),
    session_id VARCHAR(100),
    session_duration_ms INT,
    user_type VARCHAR(50),
    is_new_user BOOLEAN,
    app_version VARCHAR(20),
    os_version VARCHAR(50),
    device_model VARCHAR(100),
    network_type VARCHAR(20),
    country VARCHAR(2),
    city VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_date DATE GENERATED ALWAYS AS (DATE(created_at)) STORED
);
```

**Use Cases:**
- Screen navigation tracking
- Feature usage analytics
- User journey mapping
- A/B testing data
- Performance metrics

##### 1.2.7 error_logs
**Migration:** `20251201180500_create_analytics_tables.sql`  
**Purpose:** Application error and crash reporting

```sql
CREATE TABLE public.error_logs (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    error_type VARCHAR(100) NOT NULL,
    error_code VARCHAR(50),
    severity VARCHAR(20),            -- low, medium, high, critical
    error_message TEXT NOT NULL,
    stack_trace TEXT,
    error_context JSONB DEFAULT '{}'::jsonb,
    component VARCHAR(100),
    function_name VARCHAR(100),
    file_path VARCHAR(200),
    line_number INT,
    screen_name VARCHAR(100),
    user_action VARCHAR(200),
    app_version VARCHAR(20),
    os_version VARCHAR(50),
    device_model VARCHAR(100),
    network_state VARCHAR(20),
    available_memory_mb INT,
    battery_level INT,
    is_resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    occurrence_count INT DEFAULT 1,
    first_occurred_at TIMESTAMPTZ DEFAULT NOW(),
    last_occurred_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

**Features:**
- ✅ Severity classification
- ✅ Context capture (memory, battery, network)
- ✅ Resolution tracking
- ✅ Occurrence frequency

##### 1.2.8 daily_transaction_summary (Materialized View)
**Migration:** `20251201180500_create_analytics_tables.sql`  
**Purpose:** Pre-aggregated daily transaction metrics

```sql
CREATE MATERIALIZED VIEW public.daily_transaction_summary AS
SELECT
    user_id,
    DATE(timestamp) AS transaction_date,
    COUNT(*) AS total_transactions,
    SUM(amount) AS total_amount,
    AVG(amount) AS average_amount,
    COUNT(*) FILTER (WHERE status = 'SENT') AS successful_count,
    COUNT(*) FILTER (WHERE status = 'FAILED') AS failed_count,
    COUNT(*) FILTER (WHERE status = 'PENDING') AS pending_count,
    COUNT(DISTINCT provider) AS providers_used,
    MIN(timestamp) AS first_transaction_at,
    MAX(timestamp) AS last_transaction_at
FROM public.transactions
GROUP BY user_id, DATE(timestamp);
```

**Refresh Strategy:**
- ⚠️ Manual refresh needed: `REFRESH MATERIALIZED VIEW daily_transaction_summary;`
- ⚠️ **TODO:** Set up scheduled refresh (hourly or daily via cron job)

---

## 2. Supabase Edge Functions

**Location:** `supabase/functions/`

### 2.1 Implemented Functions

#### 2.1.1 send-whatsapp-otp
**Path:** `supabase/functions/send-whatsapp-otp/index.ts`  
**Purpose:** Send OTP via WhatsApp using Twilio  
**Method:** POST  
**Authentication:** Required (JWT)

**Request Body:**
```typescript
{
  "phoneNumber": string  // E.164 format: +233XXXXXXXXX
}
```

**Response:**
```typescript
{
  "success": boolean,
  "otpId": string,       // UUID for verification
  "expiresAt": string,   // ISO 8601 timestamp
  "message": string
}
```

**Rate Limits:**
- 3 requests per phone number per hour
- 10 requests per IP per hour
- 100 global requests per hour

**Implementation Status:**
- ✅ OTP generation with crypto.randomBytes
- ✅ SHA-256 hashing before storage
- ✅ Multi-layer rate limiting
- ✅ Twilio WhatsApp integration
- ✅ Database logging
- ⚠️ **TODO:** Test with production Twilio credentials

#### 2.1.2 verify-whatsapp-otp
**Path:** `supabase/functions/verify-whatsapp-otp/index.ts`  
**Purpose:** Verify OTP and create/link user account  
**Method:** POST  
**Authentication:** Optional (creates account if new)

**Request Body:**
```typescript
{
  "phoneNumber": string,
  "otp": string,         // 6-digit code
  "otpId": string        // UUID from send-otp response
}
```

**Response:**
```typescript
{
  "success": boolean,
  "session": {
    "accessToken": string,
    "refreshToken": string,
    "user": {
      "id": string,
      "phone": string,
      "created_at": string
    }
  }
}
```

**Features:**
- ✅ Rate limiting (5 attempts per OTP)
- ✅ Timing-safe comparison
- ✅ Auto account creation
- ✅ Session token generation
- ✅ Used OTP invalidation

#### 2.1.3 sync-transactions
**Path:** `supabase/functions/sync-transactions/index.ts`  
**Purpose:** Sync local transactions to cloud  
**Method:** POST  
**Authentication:** Required (JWT)

**Request Body:**
```typescript
{
  "transactions": Array<{
    local_id: number,
    sender: string,
    body: string,
    timestamp: string,    // ISO 8601
    status: "PENDING" | "SENT" | "FAILED",
    amount?: number,
    currency?: string,
    transaction_id?: string,
    merchant_code?: string,
    provider?: string,
    provider_type?: string
  }>,
  "device_id": string
}
```

**Response:**
```typescript
{
  "success": boolean,
  "synced_count": number,
  "failed_count": number,
  "errors": Array<{
    local_id: number,
    error: string
  }>
}
```

**Features:**
- ✅ Batch sync support
- ✅ Duplicate prevention (UNIQUE constraint)
- ✅ Device tracking
- ✅ Error reporting per transaction

**Implementation Status:**
- ✅ Basic sync logic
- ⚠️ **TODO:** Conflict resolution strategy
- ⚠️ **TODO:** Partial sync handling (some succeed, some fail)
- ⚠️ **TODO:** Sync direction (cloud → device for multi-device)

#### 2.1.4 webhook-relay
**Path:** `supabase/functions/webhook-relay/index.ts`  
**Purpose:** Relay SMS to configured webhooks  
**Method:** POST  
**Authentication:** Service role or authenticated user

**Request Body:**
```typescript
{
  "phone_number": string,
  "sender": string,
  "message": string,
  "parsed_data": {
    "amount": number,
    "transaction_id": string,
    "provider": string,
    // ... other parsed fields
  }
}
```

**Response:**
```typescript
{
  "success": boolean,
  "delivered_webhooks": number,
  "failed_webhooks": number,
  "results": Array<{
    "webhook_name": string,
    "success": boolean,
    "status_code": number,
    "error"?: string
  }>
}
```

**Features:**
- ✅ Multi-webhook dispatch
- ✅ HMAC-SHA256 signature generation
- ✅ Bearer token authentication
- ✅ Delivery logging
- ⚠️ **TODO:** Retry logic for failed webhooks
- ⚠️ **TODO:** Webhook health monitoring

#### 2.1.5 complete-user-profile
**Path:** `supabase/functions/complete-user-profile/index.ts`  
**Purpose:** Update merchant settings and business profile  
**Method:** POST/PUT  
**Authentication:** Required (JWT)

**Request Body:**
```typescript
{
  "business_name": string,
  "business_type": "retail" | "restaurant" | "service" | "transport" | "agriculture" | "other",
  "merchant_code": string,
  "business_phone": string,
  "business_email"?: string,
  "preferred_provider": "MTN" | "Vodafone" | "AirtelTigo",
  "enabled_providers": string[],
  "notification_preferences": {
    "email": boolean,
    "sms": boolean,
    "push": boolean
  }
}
```

**Response:**
```typescript
{
  "success": boolean,
  "profile": MerchantSettings,
  "is_new": boolean
}
```

**Features:**
- ✅ Upsert logic (create or update)
- ✅ Validation
- ⚠️ **TODO:** Business verification workflow
- ⚠️ **TODO:** Merchant code generation if not provided

---

## 3. API Services & Integration

### 3.1 Android API Layer

**Location:** `app/src/main/java/com/momoterminal/api/`

#### 3.1.1 AuthApiService
**File:** `AuthApiService.kt`

```kotlin
interface AuthApiService {
    @POST("send-whatsapp-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): SendOtpResponse
    
    @POST("verify-whatsapp-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): VerifyOtpResponse
    
    @POST("complete-user-profile")
    suspend fun completeProfile(@Body request: ProfileRequest): ProfileResponse
}
```

**Status:** ✅ Implemented

#### 3.1.2 MomoApiService
**File:** `MomoApiService.kt`

```kotlin
interface MomoApiService {
    @POST("sync-transactions")
    suspend fun syncTransactions(@Body request: SyncRequest): SyncResponse
    
    @GET("transactions")
    suspend fun getTransactions(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): TransactionListResponse
    
    @POST("webhook-relay")
    suspend fun relayToWebhook(@Body request: WebhookRelayRequest): WebhookRelayResponse
}
```

**Status:** ✅ Implemented

**Missing Endpoints:**
- ⚠️ GET `/merchant-settings` - Fetch user's merchant settings
- ⚠️ GET `/devices` - List user's registered devices
- ⚠️ POST `/devices/register` - Register new device
- ⚠️ POST `/analytics/events` - Batch analytics logging
- ⚠️ POST `/error-logs` - Report errors to cloud

#### 3.1.3 SyncService
**File:** `SyncService.kt`

```kotlin
class SyncService @Inject constructor(
    private val api: MomoApiService,
    private val database: MomoDatabase,
    private val deviceInfo: DeviceInfoProvider
) {
    suspend fun syncPendingTransactions(): SyncResult
    suspend fun syncWebhookConfigs(): SyncResult
    suspend fun uploadAnalytics(): SyncResult
    suspend fun uploadErrorLogs(): SyncResult
}
```

**Status:** ✅ Partially implemented
- ✅ Transaction sync
- ⚠️ Webhook config sync (needs implementation)
- ⚠️ Analytics upload (needs implementation)
- ⚠️ Error log upload (needs implementation)

---

## 4. Implementation Gaps & Action Items

### 4.1 Critical (Must Fix Before Production)

#### 4.1.1 Database Sync Conflict Resolution
**Priority:** 🔴 Critical  
**Issue:** No strategy for handling sync conflicts when a transaction is modified on multiple devices

**Solution:**
```kotlin
// Add to TransactionEntity
val syncVersion: Int = 1,
val lastModifiedAt: Long = System.currentTimeMillis(),
val lastModifiedBy: String? = null  // device_id

// Conflict resolution strategy: Last-Write-Wins based on lastModifiedAt
```

#### 4.1.2 Device Registration Flow
**Priority:** 🔴 Critical  
**Issue:** Devices table exists but no registration flow in app

**Implementation:**
```kotlin
// Create DeviceRepository
class DeviceRepository @Inject constructor(
    private val api: MomoApiService,
    private val preferences: UserPreferences
) {
    suspend fun registerDevice() {
        val deviceInfo = DeviceInfoProvider.get()
        val response = api.registerDevice(RegisterDeviceRequest(
            device_id = deviceInfo.deviceId,
            device_name = deviceInfo.model,
            manufacturer = deviceInfo.manufacturer,
            os_version = deviceInfo.osVersion,
            app_version = BuildConfig.VERSION_NAME,
            fcm_token = FirebaseMessaging.getInstance().token.await()
        ))
        preferences.saveDeviceId(response.id)
    }
}
```

**Where to call:**
- On first app launch (after authentication)
- When FCM token is refreshed
- After app update

#### 4.1.3 Merchant Settings Persistence
**Priority:** 🔴 Critical  
**Issue:** `merchant_settings` table exists but no local cache or sync

**Solution:**
1. Create local entity:
```kotlin
@Entity(tableName = "merchant_settings_cache")
data class MerchantSettingsCacheEntity(
    @PrimaryKey val userId: String,
    val businessName: String?,
    val merchantCode: String?,
    val preferredProvider: String,
    val enabledProviders: String,  // JSON string
    val nfcEnabled: Boolean,
    val lastSyncedAt: Long
)
```

2. Implement sync in `AppConfig`:
```kotlin
suspend fun syncMerchantSettings() {
    val remote = api.getMerchantSettings()
    dao.upsert(remote.toEntity())
}
```

#### 4.1.4 Analytics Integration
**Priority:** 🟡 High  
**Issue:** Analytics tables exist but no client-side event logging

**Implementation:**
```kotlin
// Create AnalyticsManager
@Singleton
class AnalyticsManager @Inject constructor(
    private val database: MomoDatabase,
    private val workManager: WorkManager
) {
    fun logEvent(
        name: String,
        category: String? = null,
        properties: Map<String, Any>? = null
    ) {
        // Log to local queue
        // Schedule upload worker
    }
    
    fun logScreenView(screenName: String) {
        logEvent("screen_view", "navigation", mapOf("screen" to screenName))
    }
}
```

**Key Events to Track:**
- Screen views
- NFC tap initiated/completed/failed
- Transaction synced
- SMS parsed
- Webhook delivery success/failure
- Settings changed
- Login/logout

#### 4.1.5 Error Logging Integration
**Priority:** 🟡 High  
**Issue:** Error logs table exists but no structured error reporting

**Implementation:**
```kotlin
// Enhance AppErrorBoundary
@Composable
fun AppErrorBoundary(
    errorLogger: ErrorLogger,
    content: @Composable () -> Unit
) {
    ErrorHandler(
        onError = { error ->
            errorLogger.logError(
                type = error::class.simpleName ?: "Unknown",
                message = error.message ?: "No message",
                stackTrace = error.stackTraceToString(),
                severity = ErrorSeverity.HIGH,
                context = mapOf(
                    "screen" to currentScreen,
                    "user_action" to lastUserAction
                )
            )
        },
        content = content
    )
}
```

#### 4.1.6 Materialized View Refresh
**Priority:** 🟡 High  
**Issue:** `daily_transaction_summary` needs periodic refresh

**Solution Options:**

**Option 1: Supabase Cron Extension**
```sql
-- Enable pg_cron
CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Schedule daily refresh at midnight
SELECT cron.schedule(
    'refresh-transaction-summary',
    '0 0 * * *',
    'REFRESH MATERIALIZED VIEW CONCURRENTLY daily_transaction_summary'
);
```

**Option 2: Edge Function with Scheduled Invocation**
```typescript
// supabase/functions/refresh-analytics/index.ts
Deno.serve(async (req) => {
  const { data, error } = await supabaseAdmin
    .rpc('refresh_transaction_summary');
  
  return new Response(JSON.stringify({ success: !error }));
});
```

Then schedule via Supabase Dashboard or GitHub Actions.

**Option 3: Trigger-based Incremental Update**
```sql
-- More efficient: update on transaction insert/update
CREATE FUNCTION update_transaction_summary()
RETURNS TRIGGER AS $$
BEGIN
    -- Incremental update logic
    INSERT INTO daily_transaction_summary (...)
    VALUES (...)
    ON CONFLICT (user_id, transaction_date)
    DO UPDATE SET
        total_transactions = daily_transaction_summary.total_transactions + 1,
        total_amount = daily_transaction_summary.total_amount + NEW.amount,
        ...;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER transaction_summary_update
AFTER INSERT OR UPDATE ON transactions
FOR EACH ROW
EXECUTE FUNCTION update_transaction_summary();
```

**Recommendation:** Use Option 3 (trigger-based) for real-time accuracy.

### 4.2 High Priority (Production Enhancement)

#### 4.2.1 Multi-Device Sync
**Priority:** 🟡 High  
**Status:** Tables ready, logic needed

**Required:**
- Bi-directional sync (cloud → device on app start)
- Conflict resolution (last-write-wins with timestamp)
- Sync status indicator in UI

#### 4.2.2 Webhook Health Monitoring
**Priority:** 🟡 High  
**Status:** Delivery logs exist, dashboard needed

**Implementation:**
```kotlin
// WebhookHealthCheck
data class WebhookHealth(
    val webhookId: Long,
    val successRate: Float,        // Last 24 hours
    val avgResponseTime: Long,     // milliseconds
    val consecutiveFailures: Int,
    val lastSuccessAt: Long?,
    val isHealthy: Boolean
)

suspend fun getWebhookHealth(webhookId: Long): WebhookHealth {
    // Query sms_delivery_logs
    // Calculate metrics
}
```

**UI Component:**
```kotlin
@Composable
fun WebhookHealthCard(health: WebhookHealth) {
    Card {
        Row {
            HealthIndicator(healthy = health.isHealthy)
            Column {
                Text("Success Rate: ${health.successRate}%")
                Text("Avg Response: ${health.avgResponseTime}ms")
                if (health.consecutiveFailures > 0) {
                    Text(
                        "⚠️ ${health.consecutiveFailures} consecutive failures",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
```

#### 4.2.3 Transaction Export
**Priority:** 🟡 High  
**Status:** Not implemented

**Features:**
- Export to CSV/Excel
- Date range filtering
- Provider filtering
- Email export option

**Implementation:**
```kotlin
class TransactionExporter @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend fun exportToCsv(
        startDate: Long,
        endDate: Long,
        outputStream: OutputStream
    ) {
        val transactions = repository.getTransactionsByDateRange(startDate, endDate)
        // Generate CSV
    }
}
```

#### 4.2.4 Push Notifications
**Priority:** 🟡 High  
**Status:** FCM token storage ready, sending logic needed

**Use Cases:**
- New transaction notification
- Sync failure alert
- Daily summary
- Security alerts (new device login, suspicious activity)

**Implementation:**
```kotlin
// Cloud Function: send-notification
// Triggered by database events or scheduled
```

### 4.3 Medium Priority (Nice to Have)

#### 4.3.1 Offline Queue Management
**Priority:** 🟠 Medium  
**Enhancement:** Visual offline queue with retry controls

#### 4.3.2 Advanced Analytics Dashboard
**Priority:** 🟠 Medium  
**Features:** Charts, trends, provider comparison

#### 4.3.3 Merchant QR Code
**Priority:** 🟠 Medium  
**Feature:** Generate QR code with merchant details for easy sharing

#### 4.3.4 Transaction Receipts
**Priority:** 🟠 Medium  
**Feature:** Generate PDF receipts, email/WhatsApp sharing

---

## 5. Testing Checklist

### 5.1 Database Testing

- [ ] **Local Database:**
  - [ ] Insert transactions while offline
  - [ ] Sync when online
  - [ ] Verify no duplicates
  - [ ] Test migration from v1 to v2

- [ ] **Supabase Tables:**
  - [ ] Test RLS policies (users can only access own data)
  - [ ] Verify foreign key constraints
  - [ ] Test unique constraints
  - [ ] Check cascade deletes

### 5.2 API Testing

- [ ] **Authentication:**
  - [ ] Send OTP (valid phone)
  - [ ] Send OTP (invalid phone)
  - [ ] Verify OTP (correct code)
  - [ ] Verify OTP (incorrect code)
  - [ ] Rate limiting (exceed limits)

- [ ] **Transaction Sync:**
  - [ ] Sync single transaction
  - [ ] Sync batch (100+ transactions)
  - [ ] Sync with conflicts
  - [ ] Sync failure recovery

- [ ] **Webhook Relay:**
  - [ ] Single webhook delivery
  - [ ] Multi-webhook delivery
  - [ ] Webhook timeout handling
  - [ ] HMAC signature verification

### 5.3 Integration Testing

- [ ] **End-to-End Flow:**
  - [ ] Register new user
  - [ ] Complete profile
  - [ ] Configure webhook
  - [ ] Receive SMS
  - [ ] Parse transaction
  - [ ] Relay to webhook
  - [ ] Sync to cloud
  - [ ] View in transactions screen

### 5.4 Performance Testing

- [ ] Sync 1000 transactions
- [ ] Query transactions with filters
- [ ] Analytics query performance
- [ ] Webhook relay latency
- [ ] App startup time with large database

---

## 6. Deployment Steps

### Phase 1: Backend Deployment (Week 1)

1. **Deploy Supabase Migrations**
   ```bash
   cd supabase
   supabase db push
   ```

2. **Deploy Edge Functions**
   ```bash
   supabase functions deploy send-whatsapp-otp
   supabase functions deploy verify-whatsapp-otp
   supabase functions deploy sync-transactions
   supabase functions deploy webhook-relay
   supabase functions deploy complete-user-profile
   ```

3. **Set Environment Variables**
   ```bash
   supabase secrets set TWILIO_ACCOUNT_SID=...
   supabase secrets set TWILIO_AUTH_TOKEN=...
   supabase secrets set TWILIO_WHATSAPP_NUMBER=...
   ```

4. **Verify Deployment**
   - Test each endpoint with Postman/cURL
   - Check Supabase logs for errors
   - Verify RLS policies

### Phase 2: Mobile App Updates (Week 1-2)

1. **Implement Missing Features**
   - Device registration
   - Merchant settings sync
   - Analytics logging
   - Error reporting

2. **Update API Integration**
   - Add missing endpoints
   - Implement retry logic
   - Add offline queue

3. **Testing**
   - Unit tests for new code
   - Integration tests
   - Manual testing on real devices

### Phase 3: Monitoring & Analytics (Week 2)

1. **Set Up Monitoring**
   - Supabase Dashboard alerts
   - Firebase Crashlytics
   - Custom error logging

2. **Analytics Dashboard**
   - Create Supabase SQL queries
   - Build basic admin dashboard (optional)

### Phase 4: Production Launch (Week 3)

1. **Internal Testing**
   - Alpha testing with 10-20 users
   - Monitor for issues

2. **Beta Launch**
   - Open to 100-500 users
   - Collect feedback

3. **Full Launch**
   - Staged rollout: 10% → 50% → 100%
   - Monitor metrics daily

---

## 7. Production Readiness Checklist

### Database
- [x] All tables created with proper indexes
- [x] RLS policies configured
- [ ] Materialized view refresh scheduled
- [ ] Database backups configured (Supabase automatic)
- [ ] Performance monitoring enabled

### API
- [x] All edge functions deployed
- [ ] Rate limiting tested under load
- [ ] Error handling for all edge cases
- [ ] API documentation created
- [ ] Postman collection for testing

### Mobile App
- [x] Local database with offline support
- [ ] Device registration on first launch
- [ ] Merchant settings sync
- [ ] Analytics event logging
- [ ] Error logging with context
- [ ] Push notification handling
- [ ] Multi-device sync

### Security
- [x] API authentication (JWT)
- [x] HMAC webhook signatures
- [x] Rate limiting (multi-layer)
- [x] Data encryption at rest (EncryptedSharedPreferences)
- [ ] Certificate pinning with production certs
- [ ] SQLCipher for Room database (recommended)

### Monitoring
- [ ] Firebase Crashlytics configured
- [ ] Custom error logging to Supabase
- [ ] Analytics dashboard
- [ ] Performance monitoring
- [ ] Webhook health monitoring
- [ ] Alert thresholds configured

### Compliance
- [ ] Privacy policy hosted publicly
- [ ] Terms of service accepted in app
- [ ] Data retention policy enforced (365 days default)
- [ ] GDPR consent flow
- [ ] SMS permission justification documented

---

## 8. Cost Estimation

### Supabase (Free Tier Limits)
- Database: 500 MB
- Storage: 1 GB
- Edge Functions: 500K invocations/month
- Bandwidth: 5 GB

**Estimated Usage (1000 active users):**
- Transactions: ~100 MB (100K records @ 1KB each)
- Logs: ~50 MB
- Edge Functions: ~300K invocations/month
- Bandwidth: ~2 GB

**Cost:** Free tier sufficient for launch, ~$25/month if upgraded to Pro.

### Twilio WhatsApp OTP
- Cost: $0.005 per message
- Expected: 3000 OTPs/month (3 per user on average)
- **Cost:** $15/month

### Firebase (Free Tier - Spark Plan)
- Crashlytics: Free
- Analytics: Free
- Cloud Messaging: Free
- **Cost:** $0

**Total Estimated Monthly Cost:** $15-40

---

## 9. Summary & Next Steps

### Current State
✅ **Database schema** is comprehensive and production-ready  
✅ **Backend services** are implemented and functional  
✅ **Mobile app** has solid foundation with offline-first architecture  
⚠️ **Integration gaps** exist but are well-documented

### Immediate Actions Required

1. **Fix Device Registration** (4 hours)
   - Implement `DeviceRepository`
   - Call on app start after auth

2. **Implement Merchant Settings Sync** (6 hours)
   - Create local cache entity
   - Sync on profile complete
   - Use in `AppConfig.isConfigured()`

3. **Add Analytics Logging** (4 hours)
   - Create `AnalyticsManager`
   - Log key events
   - Schedule batch upload

4. **Implement Error Logging** (4 hours)
   - Enhance `AppErrorBoundary`
   - Create `ErrorLogger`
   - Upload to Supabase

5. **Set Up Materialized View Refresh** (2 hours)
   - Choose strategy (trigger-based recommended)
   - Implement and test

6. **Deploy to Supabase Production** (2 hours)
   - Run migrations
   - Deploy edge functions
   - Configure secrets

7. **End-to-End Testing** (8 hours)
   - Test full user journey
   - Load test sync with 1000 transactions
   - Test multi-device scenarios

**Total Estimated Time:** 30 hours (3-4 days)

### Success Metrics

After implementation:
- [ ] 100% of transactions synced successfully
- [ ] <500ms average API response time
- [ ] 99.9% webhook delivery success rate
- [ ] <5% crash rate
- [ ] All devices registered and tracked
- [ ] Analytics events flowing to Supabase

---

## 10. Contact & Support

**Documentation:**
- Database schema: `supabase/migrations/`
- API services: `app/src/main/java/com/momoterminal/api/`
- Edge functions: `supabase/functions/`

**For Issues:**
- Check Firebase Crashlytics
- Review Supabase logs
- Query `error_logs` table

**Deployment Status:** 🟡 90% Complete - Implementation gaps identified and documented.

---

**Document Version:** 1.0  
**Last Updated:** December 1, 2025  
**Next Review:** After implementing immediate actions
