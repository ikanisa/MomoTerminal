# 🎯 Complete SMS-to-Supabase Flow with AI

## Overview

This system automatically processes incoming Mobile Money SMS messages, uses AI to extract structured data, matches payments to registered vendors, and saves everything to Supabase.

---

## Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ 1. SMS Arrives                                               │
│    "You have received RWF 5,000 from Jean BOSCO             │
│     (250788767816). Transaction ID: MP123456"               │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. SmsReceiver (Broadcast Receiver)                         │
│    - Catches SMS                                             │
│    - Checks if Mobile Money transaction                      │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. AI Parser (OpenAI/Gemini)                                │
│    - Extracts: payer_name, payer_phone, amount, currency    │
│    - Extracts: transaction_id, timestamp, provider          │
│    - Returns confidence score (0.0 - 1.0)                    │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Vendor Matcher                                            │
│    - Queries sms_parsing_vendors table                       │
│    - Matches by payee_momo_number                           │
│    - Returns vendor_id if match found                        │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Save to Supabase                                          │
│    - Table: vendor_sms_transactions                          │
│    - Status: "matched" (if vendor found) or "parsed"         │
│    - Includes raw SMS, parsed data, AI confidence            │
└─────────────────────────────────────────────────────────────┘
```

---

## Database Tables

### 1. `sms_parsing_vendors`

Registered vendors who receive payments.

```sql
CREATE TABLE sms_parsing_vendors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_name TEXT NOT NULL,
    payee_momo_number TEXT NOT NULL UNIQUE,  -- Their MOMO number
    whatsapp_e164 TEXT NOT NULL,             -- Their WhatsApp login
    terminal_device_id TEXT,
    subscription_status TEXT NOT NULL DEFAULT 'pending',
    api_key TEXT NOT NULL,
    hmac_secret TEXT NOT NULL,
    webhook_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**How it works:**
- Each vendor registers their MOMO number (e.g., "250788767816")
- When a payment comes to that number, it's allocated to them
- They can view their transactions via API

### 2. `vendor_sms_transactions`

All processed Mobile Money SMS transactions.

```sql
CREATE TABLE vendor_sms_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id UUID REFERENCES sms_parsing_vendors(id),  -- Matched vendor
    raw_sms TEXT NOT NULL,                              -- Original SMS
    sender_address TEXT,                                -- SMS sender
    received_at TIMESTAMPTZ,
    
    -- AI Parsed Data
    payer_name TEXT,                                    -- From SMS body
    payer_phone TEXT,                                   -- Full phone if available
    amount NUMERIC(15,2),
    currency TEXT DEFAULT 'RWF',
    txn_id TEXT,
    txn_timestamp TIMESTAMPTZ,
    provider TEXT,                                       -- mtn, vodafone, etc.
    ai_confidence NUMERIC(3,2),                          -- 0.00 - 1.00
    parsed_json JSONB,                                   -- Full AI response
    
    status TEXT NOT NULL DEFAULT 'parsed',               -- raw, parsed, matched, error
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## Implementation

### Files Created

1. **`AiSmsParser.kt`** - AI parsing with OpenAI/Gemini
2. **`VendorSmsProcessor.kt`** - Vendor matching and Supabase sync
3. **`SmsReceiver.kt`** - Updated to use AI processor
4. **`ai.properties`** - API keys configuration

### Setup

#### 1. Get API Keys

**Option A: OpenAI**
```
1. Go to https://platform.openai.com/api-keys
2. Create new API key
3. Copy key (starts with sk-...)
```

**Option B: Google Gemini**
```
1. Go to https://aistudio.google.com/app/apikey
2. Create API key
3. Copy key
```

#### 2. Configure API Keys

Edit `ai.properties`:
```properties
# OpenAI
OPENAI_API_KEY=sk-your-actual-key-here

# OR Gemini
GEMINI_API_KEY=your-gemini-key-here

# Which to use
AI_PROVIDER=openai  # or gemini
```

#### 3. Register a Vendor

```sql
INSERT INTO sms_parsing_vendors (
    vendor_name,
    payee_momo_number,
    whatsapp_e164,
    subscription_status,
    api_key,
    hmac_secret
) VALUES (
    'Jean Shop',
    '250788767816',      -- Their MOMO number
    '+250788767816',     -- Their WhatsApp login
    'active',
    gen_random_uuid()::text,
    gen_random_uuid()::text
);
```

#### 4. Grant SMS Permission

```kotlin
// In app settings
Settings → Permissions & Controls → SMS Access → Enable
```

---

## Testing

### 1. Send Test SMS

Send yourself a test MoMo SMS:
```
You have received RWF 5,000 from Jean BOSCO (250788767816). 
Transaction ID: MP123456
```

### 2. Check Logs

```bash
adb logcat | grep -i "sms\|vendor\|ai"
```

Expected output:
```
SMS received from: MTN
Processing MoMo SMS from MTN
AI parsed: ParsedSmsData(payerName=Jean BOSCO, amount=5000.0, ...)
Matched vendor: vendor_id=abc-123
✅ SMS processed successfully: txn_id_xyz
```

### 3. Verify in Supabase

```sql
-- Check transactions
SELECT 
    v.vendor_name,
    t.payer_name,
    t.amount,
    t.currency,
    t.txn_id,
    t.status,
    t.ai_confidence,
    t.created_at
FROM vendor_sms_transactions t
LEFT JOIN sms_parsing_vendors v ON t.vendor_id = v.id
ORDER BY t.created_at DESC
LIMIT 10;
```

---

## How Vendor Matching Works

### Scenario 1: Matched Transaction
```
SMS: "You received RWF 5,000 from John (250788123456)"

1. AI extracts: payer_phone = "250788123456"
2. Query: SELECT * FROM sms_parsing_vendors 
         WHERE payee_momo_number = '250788123456'
3. Match found → vendor_id = abc-123
4. Save with status = 'matched'
```

### Scenario 2: Unmatched Transaction
```
SMS: "You received RWF 5,000 from Jane (250799999999)"

1. AI extracts: payer_phone = "250799999999"
2. Query: No vendor found
3. Save with vendor_id = null, status = 'parsed'
4. Admin can manually assign later
```

---

## AI Prompt Engineering

### What We Ask the AI

```
You are a Mobile Money SMS parser. Extract structured data from SMS.
Return ONLY valid JSON:
{
  "payer_name": "Full name of sender",
  "payer_phone": "Full phone number",
  "payer_phone_last3": "Last 3 digits",
  "amount": 1000.50,
  "currency": "RWF",
  "transaction_id": "MP123456",
  "timestamp": "ISO 8601 if available",
  "provider": "mtn|vodafone|airteltigo",
  "confidence": 0.95
}
```

### Why This Works

- **Structured output**: Forces JSON format
- **Clear fields**: AI knows exactly what to extract
- **Confidence score**: AI rates its own accuracy
- **Handles variations**: Works across different SMS formats

---

## Cost Estimate

### OpenAI GPT-3.5-turbo
- **Cost**: ~$0.002 per SMS (2,000 SMS = $4)
- **Speed**: ~2-3 seconds
- **Accuracy**: ~95%

### Google Gemini Flash
- **Cost**: ~$0.0001 per SMS (10,000 SMS = $1)
- **Speed**: ~1-2 seconds
- **Accuracy**: ~93%

**Recommendation**: Start with Gemini (cheaper), upgrade to OpenAI if accuracy issues.

---

## Security

### API Keys
- ✅ Stored in `ai.properties` (gitignored)
- ✅ Not committed to repository
- ✅ Can be environment variables in production

### RLS Policies
```sql
-- Vendors can only see their own transactions
CREATE POLICY "vendor_txns_own_read" ON vendor_sms_transactions
FOR SELECT TO authenticated
USING (
    vendor_id IN (
        SELECT id FROM sms_parsing_vendors 
        WHERE whatsapp_e164 = current_user_whatsapp()
    )
);
```

---

## Troubleshooting

### SMS Not Being Processed
1. Check SMS permission granted
2. Check AI API key is valid
3. Check logs for errors
4. Test AI parsing manually

### No Vendor Match
1. Verify vendor's MOMO number is registered
2. Check exact number format (with/without country code)
3. Query `sms_parsing_vendors` table

### Low AI Confidence
1. Check SMS format is readable
2. Try different AI provider
3. Adjust temperature parameter
4. Add more context to prompt

---

## Next Steps

1. **Deploy to Production**
   - Add API keys to secure environment variables
   - Monitor AI costs
   - Set up error alerts

2. **Add Webhook Notifications**
   - Notify vendors of new payments via webhook
   - Real-time payment notifications

3. **Build Vendor Dashboard**
   - Let vendors view their transactions
   - Download reports
   - Manage settings

4. **Optimize AI Costs**
   - Cache common SMS patterns
   - Use regex for simple cases
   - Only use AI for complex parsing

---

## Summary

✅ **SMS Received** → Broadcast Receiver  
✅ **AI Parsing** → OpenAI/Gemini extracts data  
✅ **Vendor Matching** → Match by MOMO number  
✅ **Save to Supabase** → vendor_sms_transactions table  
✅ **RLS Security** → Vendors see only their data  

**Status**: Ready to process real Mobile Money SMS! 🚀
