# MomoTerminal ↔ EasyMO Integration Action Plan

**Generated:** December 3, 2025  
**Based on:** Comprehensive Integration Review

---

## Executive Summary

| Category | Current | Target | Priority |
|----------|---------|--------|----------|
| Security | 75/100 | 95/100 | 🔴 P0 |
| Reliability | 55/100 | 95/100 | 🟡 P1 |
| Consistency | 45/100 | 95/100 | 🔴 P0 |
| Observability | 70/100 | 95/100 | 🟢 P2 |

---

## Phase 1: Critical Security & Consistency (Week 1)

### 1.1 🔴 Add Nonce for Replay Protection

**File:** `app/src/main/java/com/momoterminal/webhook/WebhookDispatcher.kt`

Add nonce generation to prevent replay attacks:

```kotlin
// Add to createPayload() method
private fun createPayload(log: SmsDeliveryLogEntity, timestamp: Long): String {
    val nonce = UUID.randomUUID().toString()
    val json = JSONObject().apply {
        put("source", PAYLOAD_SOURCE)
        put("version", PAYLOAD_VERSION)
        put("timestamp", isoDateFormat.format(Date(timestamp)))
        put("nonce", nonce)  // ADD THIS
        put("client_transaction_id", UUID.randomUUID().toString())  // ADD THIS
        put("phone_number", log.phoneNumber)
        put("sender", log.sender)
        put("message", log.message)
        put("device_id", getDeviceId())
    }
    return json.toString()
}
```

**EasyMO Side:** Add nonce tracking table and validation.

---

### 1.2 🔴 Unified Transaction ID

**File:** `app/src/main/java/com/momoterminal/data/local/entity/TransactionEntity.kt`

Add client-generated UUID:

```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "client_transaction_id")
    val clientTransactionId: String = UUID.randomUUID().toString(),  // ADD THIS
    
    // ... existing fields
)
```

---

### 1.3 🔴 Standardize Webhook Headers

**Current inconsistency:**
- MomoTerminal: `X-Momo-Signature`, `X-Momo-Timestamp`, `X-Momo-Device-Id`
- webhook-relay: `X-Webhook-Signature`, `X-Webhook-Timestamp`

**Decision:** Standardize on `X-Momo-*` prefix across all systems.

---

### 1.4 🟡 Consolidate Edge Functions

**Current State:**
```
MomoTerminal/supabase/functions/     EasyMO/supabase/functions/
├── send-whatsapp-otp/               ├── momo-sms-webhook/
├── verify-whatsapp-otp/             ├── momo-sms-hook/
├── sync-transactions/               ├── momo-webhook/
├── webhook-relay/                   └── momo-allocator/
├── register-device/
├── complete-user-profile/
├── send-otp/
├── upload-errors/
├── upload-analytics/
└── get-merchant-settings/
```

**Recommendation:** 
1. Keep auth functions (`send-whatsapp-otp`, `verify-whatsapp-otp`) in MomoTerminal project
2. Move SMS/transaction functions to EasyMO for centralized processing
3. Create shared types package

---

## Phase 2: Reliability Improvements (Week 2)

### 2.1 🟡 Add Idempotency Keys

**File:** `app/src/main/java/com/momoterminal/webhook/WebhookDispatcher.kt`

```kotlin
suspend fun dispatchWithIdempotency(
    endpoint: WebhookEndpoint,
    payload: MomoSmsPayload
): DeliveryResult {
    val idempotencyKey = "${payload.clientTransactionId}-${endpoint.id}"
    
    // Check if already delivered
    val existing = deliveryLogDao.findByIdempotencyKey(idempotencyKey)
    if (existing?.status == DeliveryStatus.DELIVERED) {
        return DeliveryResult.AlreadyDelivered(existing)
    }
    
    // Proceed with delivery...
}
```

---

### 2.2 🟡 Exponential Backoff Retry

**File:** `app/src/main/java/com/momoterminal/sync/SyncWorker.kt`

Current implementation uses WorkManager's default retry. Enhance with:

```kotlin
companion object {
    private val RETRY_DELAYS = listOf(1_000L, 5_000L, 30_000L, 120_000L, 300_000L)
}

private suspend fun syncWithRetry(txn: TransactionEntity): Boolean {
    var lastError: Exception? = null
    
    for ((attempt, delay) in RETRY_DELAYS.withIndex()) {
        try {
            val result = paymentRepository.syncTransaction(txn)
            if (result.isSuccess) return true
        } catch (e: Exception) {
            lastError = e
            if (attempt < RETRY_DELAYS.lastIndex) {
                delay(delay)
            }
        }
    }
    
    Log.e(TAG, "All retries failed for ${txn.id}", lastError)
    return false
}
```

---

### 2.3 🟡 Webhook Delivery Confirmation

Add acknowledgment tracking:

```kotlin
// In WebhookDispatcher
data class DeliveryAck(
    val webhookId: Long,
    val transactionId: String,
    val serverTransactionId: String?,  // Returned by EasyMO
    val acknowledgedAt: Long
)
```

---

## Phase 3: API Contract Alignment (Week 3)

### 3.1 Unified Payload Schema v2

```kotlin
// MomoTerminal: app/src/main/java/com/momoterminal/webhook/MomoSmsPayloadV2.kt

@Serializable
data class MomoSmsPayloadV2(
    // Header
    val version: String = "2.0",
    val source: String = "momoterminal",
    
    // Identifiers
    val clientTransactionId: String,
    val deviceId: String,
    val correlationId: String = UUID.randomUUID().toString(),
    
    // SMS Data
    val phoneNumber: String,
    val sender: String,
    val message: String,
    val receivedAt: String,  // ISO 8601
    
    // Parsed Data (optional)
    val parsed: ParsedSmsData? = null,
    
    // Metadata
    val metadata: DeviceMetadata? = null
)

@Serializable
data class ParsedSmsData(
    val amount: Double? = null,
    val currency: String? = null,
    val provider: String? = null,
    val transactionId: String? = null,
    val senderName: String? = null,
    val parsedBy: String  // "ai", "regex", "none"
)

@Serializable
data class DeviceMetadata(
    val appVersion: String,
    val osVersion: String,
    val networkType: String
)
```

---

### 3.2 EasyMO Database Migration

```sql
-- Add to EasyMO migrations
ALTER TABLE momo_transactions 
ADD COLUMN IF NOT EXISTS client_transaction_id UUID UNIQUE,
ADD COLUMN IF NOT EXISTS client_received_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS app_version TEXT,
ADD COLUMN IF NOT EXISTS correlation_id UUID,
ADD COLUMN IF NOT EXISTS sync_status TEXT DEFAULT 'pending';

CREATE INDEX IF NOT EXISTS idx_momo_txn_client_id 
ON momo_transactions(client_transaction_id);

-- Idempotency table
CREATE TABLE IF NOT EXISTS idempotency_keys (
    key TEXT PRIMARY KEY,
    result JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);
```

---

## Phase 4: Observability & UX (Week 4)

### 4.1 End-to-End Transaction Tracing

Add correlation ID flow:

```
MomoTerminal                    EasyMO
     │                            │
     │ correlation_id: abc123     │
     ├───────────────────────────►│
     │                            │ Log: correlation_id=abc123
     │                            │
     │◄───────────────────────────┤
     │ server_txn_id: xyz789      │
     │                            │
```

### 4.2 Transaction Status Polling

```kotlin
// MomoTerminal: TransactionStatusPoller.kt
@Singleton
class TransactionStatusPoller @Inject constructor(
    private val supabase: SupabaseClient,
    private val transactionDao: TransactionDao
) {
    suspend fun pollPendingTransactions() {
        val pendingIds = transactionDao.getPendingClientTransactionIds()
        if (pendingIds.isEmpty()) return
        
        val serverStatuses = supabase
            .from("momo_transactions")
            .select("client_transaction_id, status, matched_record_id")
            .inFilter("client_transaction_id", pendingIds)
            .execute()
        
        serverStatuses.forEach { status ->
            transactionDao.updateServerStatus(
                status.clientTransactionId,
                status.status,
                status.matchedRecordId
            )
        }
    }
}
```

---

## Implementation Checklist

### Week 1 (Critical)
- [ ] Add nonce to webhook payload
- [ ] Add `client_transaction_id` to TransactionEntity
- [ ] Standardize header names to `X-Momo-*`
- [ ] Document which functions stay in MomoTerminal vs move to EasyMO

### Week 2 (Reliability)
- [ ] Implement idempotency key tracking
- [ ] Add exponential backoff to SyncWorker
- [ ] Add delivery acknowledgment parsing

### Week 3 (Consistency)
- [ ] Create MomoSmsPayloadV2 data class
- [ ] Update WebhookDispatcher to use v2 payload
- [ ] Create EasyMO database migration
- [ ] Update EasyMO edge functions to accept v2

### Week 4 (Observability)
- [ ] Add correlation ID to all requests
- [ ] Implement TransactionStatusPoller
- [ ] Add offline queue visibility UI
- [ ] Standardize error codes

---

## Quick Wins (Can Do Today)

1. **Add nonce to payload** - 30 minutes
2. **Add client_transaction_id** - 1 hour  
3. **Document function ownership** - 1 hour

---

## Files to Modify

| File | Changes |
|------|---------|
| `WebhookDispatcher.kt` | Add nonce, client_transaction_id, v2 payload |
| `TransactionEntity.kt` | Add clientTransactionId field |
| `SyncWorker.kt` | Add exponential backoff |
| `webhook-relay/index.ts` | Standardize headers |
| `sync-transactions/index.ts` | Accept client_transaction_id |

---

## Success Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Duplicate transactions | Unknown | 0% |
| Failed webhook deliveries | Unknown | <1% |
| End-to-end trace coverage | 0% | 100% |
| Average sync latency | Unknown | <5s |

---

## References

- [CRITICAL_FIXES_SUMMARY.md](./CRITICAL_FIXES_SUMMARY.md) - Security fixes applied
- [PRODUCTION_CERTIFICATE_PINS.md](./PRODUCTION_CERTIFICATE_PINS.md) - Certificate management
- [DATABASE_BACKEND_IMPLEMENTATION.md](./DATABASE_BACKEND_IMPLEMENTATION.md) - Backend schema
