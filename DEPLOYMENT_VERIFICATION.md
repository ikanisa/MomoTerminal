# 🔍 Backend Deployment Verification

**Date:** December 1, 2025  
**Status:** ✅ Database Migrations Already Applied

---

## ✅ Database Migration Status

Our new migrations have **ALREADY BEEN APPLIED** to the remote database:

```
✅ 20251201180000_create_transactions_table.sql
✅ 20251201180100_create_webhook_configs_table.sql
✅ 20251201180200_create_sms_delivery_logs_table.sql
✅ 20251201180300_create_devices_table.sql
✅ 20251201180400_create_merchant_settings_table.sql
✅ 20251201180500_create_analytics_tables.sql
✅ 20251201180600_create_helper_functions.sql
```

**This means:**
- All 7 production tables are live
- All 8 helper functions are deployed
- All RLS policies are active
- All indexes are created
- All cron jobs are scheduled

---

## 📋 Deployment Verification Checklist

### 1. Verify Database Tables

Access your Supabase Dashboard:
https://app.supabase.com/project/lhbowpbcpwoiparwnwgt/editor

Check these tables exist:
- [ ] `transactions`
- [ ] `webhook_configs`
- [ ] `sms_delivery_logs`
- [ ] `devices`
- [ ] `merchant_settings`
- [ ] `analytics_events`
- [ ] `error_logs`

### 2. Verify Helper Functions

Go to Database > Functions and check:
- [ ] `get_transaction_stats`
- [ ] `update_device_activity`
- [ ] `check_webhook_health`
- [ ] `auto_disable_unhealthy_webhooks`
- [ ] `cleanup_old_data`
- [ ] `refresh_analytics_views`
- [ ] `get_recent_transactions`
- [ ] `upsert_merchant_settings`

### 3. Deploy Edge Functions (Manual Step Required)

```bash
# Deploy sync-transactions
supabase functions deploy sync-transactions --linked

# Deploy webhook-relay  
supabase functions deploy webhook-relay --linked

# Verify deployment
supabase functions list --linked
```

### 4. Verify Edge Functions

After deployment, check:
https://app.supabase.com/project/lhbowpbcpwoiparwnwgt/functions

Should show:
- [ ] `sync-transactions` (deployed)
- [ ] `webhook-relay` (deployed)

### 5. Test Edge Functions

```bash
# Get your auth token from Supabase Dashboard
# Settings > API > Project API keys > anon/public key

# Test sync-transactions
curl -X POST \
  https://lhbowpbcpwoiparwnwgt.supabase.co/functions/v1/sync-transactions \
  -H "Authorization: Bearer YOUR_ANON_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "device_id": "test-device-123",
    "transactions": [{
      "local_id": 1,
      "sender": "MTN",
      "body": "Test transaction",
      "timestamp": "2025-12-01T18:00:00Z",
      "status": "PENDING",
      "amount": 10.50,
      "currency": "GHS"
    }]
  }'

# Expected response:
# {"success": true, "synced": 1, "message": "Successfully synced 1 transactions"}
```

---

## 🔐 Verify RLS Policies

Test that Row Level Security is working:

1. Go to Table Editor
2. Try to view `transactions` table
3. Should be empty (users can only see their own data)
4. Try to insert a row - should fail without proper auth

---

## 📊 Verify Cron Jobs

Go to Database > Cron Jobs:

Should see:
- [ ] `cleanup-old-data` (every hour)
- [ ] `refresh-analytics` (every 5 minutes)
- [ ] `check-webhook-health` (every 30 minutes)
- [ ] `cleanup-expired-otps` (every 15 minutes)

---

## 🧪 Test Database Functions

Run these queries in SQL Editor:

```sql
-- Test get_transaction_stats (should return empty stats for new user)
SELECT * FROM get_transaction_stats(auth.uid());

-- Test materialized view exists
SELECT * FROM daily_transaction_summary LIMIT 1;

-- Test devices table
SELECT * FROM devices;

-- Test merchant_settings
SELECT * FROM merchant_settings;
```

---

## ✅ Production Readiness Checklist

After verification:

- [ ] All tables created and accessible
- [ ] RLS policies enforced
- [ ] Helper functions executable
- [ ] Edge Functions deployed
- [ ] Edge Functions tested successfully
- [ ] Cron jobs scheduled
- [ ] Materialized views created

---

## 🚀 Next Steps

### 1. Android App Integration (2-3 hours)

**Implement Transaction Sync Worker:**
```kotlin
// app/src/main/java/com/momoterminal/sync/TransactionSyncWorker.kt
@HiltWorker
class TransactionSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: SmsRepository,
    private val supabaseClient: SupabaseClient,
    private val deviceManager: DeviceManager
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Get unsynced transactions
            val transactions = repository.getUnsyncedTransactions()
            
            if (transactions.isEmpty()) {
                return Result.success()
            }
            
            // Call sync-transactions Edge Function
            val response = supabaseClient.functions
                .invoke("sync-transactions") {
                    body = Json.encodeToString(mapOf(
                        "device_id" to deviceManager.getDeviceId(),
                        "transactions" to transactions.map { it.toSyncDto() }
                    ))
                }
            
            if (response.status == HttpStatusCode.OK) {
                // Mark as synced
                repository.markAsSynced(transactions.map { it.id })
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Transaction sync failed")
            Result.retry()
        }
    }
}
```

**Schedule Periodic Sync:**
```kotlin
// In App.kt or SyncModule
val syncRequest = PeriodicWorkRequestBuilder<TransactionSyncWorker>(
    repeatInterval = 15,
    repeatIntervalTimeUnit = TimeUnit.MINUTES
)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()

WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(
        "transaction-sync",
        ExistingPeriodicWorkPolicy.KEEP,
        syncRequest
    )
```

### 2. Device Registration

**Register Device on App Startup:**
```kotlin
// In MainActivity onCreate or App class
lifecycleScope.launch {
    val deviceId = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ANDROID_ID
    )
    
    supabaseClient.postgrest
        .from("devices")
        .upsert(mapOf(
            "device_id" to deviceId,
            "device_name" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "device_type" to "android",
            "os_version" to Build.VERSION.RELEASE,
            "app_version" to BuildConfig.VERSION_NAME,
            "is_trusted" to true
        ))
}
```

### 3. Webhook Integration

**Call Webhook Relay on SMS Receipt:**
```kotlin
// In SmsReceiver after successful parsing
lifecycleScope.launch {
    webhookConfigs.forEach { webhook ->
        supabaseClient.functions
            .invoke("webhook-relay") {
                body = Json.encodeToString(mapOf(
                    "webhook_id" to webhook.id,
                    "sms_data" to mapOf(
                        "sender" to sender,
                        "message" to message,
                        "timestamp" to timestamp,
                        "phone_number" to phoneNumber,
                        "parsed_data" to parsedData
                    )
                ))
            }
    }
}
```

### 4. Analytics Integration

**Track Events:**
```kotlin
// Create AnalyticsTracker.kt
@Singleton
class AnalyticsTracker @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    suspend fun trackEvent(
        name: String,
        category: String,
        action: String? = null,
        properties: Map<String, Any>? = null
    ) {
        supabaseClient.postgrest
            .from("analytics_events")
            .insert(mapOf(
                "event_name" to name,
                "event_category" to category,
                "event_action" to action,
                "properties" to properties,
                "session_id" to sessionId,
                "screen_name" to currentScreen
            ))
    }
}

// Use in ViewModels
analyticsTracker.trackEvent(
    name = "nfc_payment_initiated",
    category = "payment",
    action = "tap",
    properties = mapOf(
        "provider" to "MTN",
        "amount" to amount
    )
)
```

---

## 📈 Monitoring Setup

### 1. Supabase Dashboard

Monitor these metrics:
- Database size
- API requests/hour
- Function invocations
- Error rates

### 2. Custom Queries for Monitoring

```sql
-- Daily transaction volume
SELECT 
    DATE(created_at) as date,
    COUNT(*) as total_transactions,
    SUM(amount) as total_amount
FROM transactions
WHERE created_at > NOW() - INTERVAL '7 days'
GROUP BY DATE(created_at)
ORDER BY date DESC;

-- Webhook health
SELECT 
    w.name,
    COUNT(l.id) as deliveries_24h,
    COUNT(l.id) FILTER (WHERE l.status = 'sent') as successful,
    ROUND(100.0 * COUNT(l.id) FILTER (WHERE l.status = 'sent') / COUNT(l.id), 2) as success_rate
FROM webhook_configs w
LEFT JOIN sms_delivery_logs l ON w.id = l.webhook_id
WHERE l.created_at > NOW() - INTERVAL '24 hours'
GROUP BY w.id, w.name;

-- Active devices
SELECT 
    COUNT(*) as total_devices,
    COUNT(*) FILTER (WHERE last_active_at > NOW() - INTERVAL '7 days') as active_7d,
    COUNT(*) FILTER (WHERE is_trusted = true) as trusted
FROM devices;

-- Error frequency
SELECT 
    error_type,
    COUNT(*) as occurrences,
    MAX(created_at) as last_seen
FROM error_logs
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY error_type
ORDER BY occurrences DESC;
```

---

## ✅ Completion Status

```
╔══════════════════════════════════════════════════════════════════╗
║                                                                  ║
║   ✅ DATABASE: FULLY DEPLOYED                                   ║
║   ⏳ EDGE FUNCTIONS: PENDING MANUAL DEPLOYMENT                  ║
║   📱 ANDROID APP: READY FOR INTEGRATION                         ║
║                                                                  ║
╚══════════════════════════════════════════════════════════════════╝
```

**Database:** 100% Complete ✅  
**Edge Functions:** Ready to deploy ⏳  
**Android Integration:** Awaiting implementation 📱  

**Estimated Time to Full Production:** 2-3 hours
