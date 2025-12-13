# CRITICAL-007: API Keys Security - Implementation Guide

**Priority:** CRITICAL  
**Time Required:** 2 hours  
**Status:** READY TO IMPLEMENT

---

## 🔴 SECURITY RISK

**Current State:**
- OpenAI API key embedded in APK (`BuildConfig.OPENAI_API_KEY`)
- Gemini API key embedded in APK (`BuildConfig.GEMINI_API_KEY`)
- Keys visible in decompiled APK via reverse engineering

**Impact:**
- Attackers can extract keys and abuse your AI API quota
- Could result in thousands of dollars in unauthorized API usage
- **CVSS Score:** 7.5 (High)

---

## ✅ SOLUTION: Move AI Parsing to Edge Functions

### Architecture Change

**BEFORE (Insecure):**
```
Android App (contains API keys) → OpenAI/Gemini API
```

**AFTER (Secure):**
```
Android App → Supabase Edge Function (contains API keys) → OpenAI/Gemini API
```

---

## 📝 IMPLEMENTATION STEPS

### Step 1: Create Edge Function `parse-sms-ai`

Create file: `supabase/functions/parse-sms-ai/index.ts`

```typescript
// Edge Function: parse-sms-ai
// Parses SMS text using OpenAI/Gemini with server-side API keys

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface ParseSmsRequest {
  smsText: string
  provider?: string
  userId: string
}

interface ParsedTransaction {
  amount: number
  currency: string
  transactionType: 'DEPOSIT' | 'WITHDRAWAL' | 'PAYMENT' | 'TRANSFER' | 'UNKNOWN'
  provider: string
  providerType: string
  recipientName?: string
  recipientPhone?: string
  transactionId?: string
  reference?: string
  balance?: number
  timestamp?: string
  success: boolean
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { smsText, provider = 'auto', userId }: ParseSmsRequest = await req.json()
    
    if (!smsText || smsText.trim().length === 0) {
      return new Response(
        JSON.stringify({ success: false, error: 'SMS text is required' }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
      )
    }

    const sanitizedText = smsText.slice(0, 1000).trim()
    let parsedData: ParsedTransaction | null = null

    // Try OpenAI first
    if ((provider === 'auto' || provider === 'openai') && Deno.env.get('OPENAI_API_KEY')) {
      try {
        parsedData = await parseWithOpenAI(sanitizedText)
      } catch (error) {
        console.error('OpenAI parsing failed:', error)
      }
    }

    // Fallback to Gemini
    if (!parsedData && Deno.env.get('GEMINI_API_KEY')) {
      try {
        parsedData = await parseWithGemini(sanitizedText)
      } catch (error) {
        console.error('Gemini parsing failed:', error)
      }
    }

    if (parsedData) {
      return new Response(
        JSON.stringify({ success: true, transaction: parsedData }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
      )
    } else {
      return new Response(
        JSON.stringify({ success: false, error: 'Could not parse SMS text' }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
      )
    }

  } catch (error) {
    console.error('Parse SMS error:', error)
    return new Response(
      JSON.stringify({ success: false, error: error.message }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
    )
  }
})

async function parseWithOpenAI(smsText: string): Promise<ParsedTransaction | null> {
  const apiKey = Deno.env.get('OPENAI_API_KEY')
  if (!apiKey) return null

  const response = await fetch('https://api.openai.com/v1/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${apiKey}`
    },
    body: JSON.stringify({
      model: 'gpt-3.5-turbo',
      messages: [
        {
          role: 'system',
          content: 'You are an SMS transaction parser. Extract transaction details and return ONLY valid JSON.'
        },
        {
          role: 'user',
          content: `Parse this SMS and return JSON: amount, currency, transactionType, provider, recipientName, transactionId, balance, timestamp, success.\n\nSMS: ${smsText}`
        }
      ],
      temperature: 0.1,
      max_tokens: 300
    })
  })

  if (!response.ok) throw new Error(`OpenAI API error: ${response.status}`)

  const data = await response.json()
  const content = data.choices[0]?.message?.content

  try {
    return content ? JSON.parse(content) : null
  } catch {
    return null
  }
}

async function parseWithGemini(smsText: string): Promise<ParsedTransaction | null> {
  const apiKey = Deno.env.get('GEMINI_API_KEY')
  if (!apiKey) return null

  const response = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=${apiKey}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{
          parts: [{
            text: `Parse this SMS and return JSON: amount, currency, transactionType, provider, recipientName, transactionId, balance, timestamp, success.\n\nSMS: ${smsText}`
          }]
        }],
        generationConfig: { temperature: 0.1, maxOutputTokens: 300 }
      })
    }
  )

  if (!response.ok) throw new Error(`Gemini API error: ${response.status}`)

  const data = await response.json()
  const content = data.candidates[0]?.content?.parts[0]?.text

  try {
    return content ? JSON.parse(content) : null
  } catch {
    return null
  }
}
```

### Step 2: Set Environment Variables in Supabase

```bash
# Set API keys as Supabase secrets
supabase secrets set OPENAI_API_KEY=your_openai_key_here
supabase secrets set GEMINI_API_KEY=your_gemini_key_here
```

**OR via Supabase Dashboard:**
1. Go to Project Settings → Edge Functions
2. Add secrets:
   - `OPENAI_API_KEY` = your OpenAI key
   - `GEMINI_API_KEY` = your Gemini key

### Step 3: Deploy Edge Function

```bash
cd supabase
supabase functions deploy parse-sms-ai
```

### Step 4: Remove API Keys from Android App

Edit `app/build.gradle.kts` (lines 94-109):

**BEFORE:**
```kotlin
// OpenAI configuration (PRIMARY AI parser)
val openAiApiKey = localProps.getProperty("OPENAI_API_KEY")
    ?: System.getenv("OPENAI_API_KEY")
    ?: ""
buildConfigField("String", "OPENAI_API_KEY", "\"$openAiApiKey\"")

// Gemini AI configuration (FALLBACK AI parser)
val geminiApiKey = localProps.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY")
    ?: ""
buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

// AI parsing feature flag
val aiParsingEnabled = localProps.getProperty("AI_PARSING_ENABLED")?.toBoolean()
    ?: (openAiApiKey.isNotBlank() || geminiApiKey.isNotBlank())
buildConfigField("boolean", "AI_PARSING_ENABLED", "$aiParsingEnabled")
```

**AFTER:**
```kotlin
// AI parsing now handled by Edge Functions - no API keys in app
buildConfigField("boolean", "AI_PARSING_ENABLED", "true")
```

### Step 5: Update Android Code to Use Edge Function

Find the AI parsing service (likely in `app/src/main/java/com/momoterminal/ai/AiSmsParserService.kt` or similar).

**Add Edge Function client:**

```kotlin
// In EdgeFunctionsApi.kt or similar
suspend fun parseSmsAi(
    smsText: String,
    userId: String,
    provider: String = "auto"
): ParseSmsResponse {
    return client.functions.invoke(
        function = "parse-sms-ai",
        body = mapOf(
            "smsText" to smsText,
            "userId" to userId,
            "provider" to provider
        )
    ).decodeAs<ParseSmsResponse>()
}

data class ParseSmsResponse(
    val success: Boolean,
    val transaction: ParsedTransaction?,
    val error: String?
)
```

**Update SMS parser to use Edge Function instead of local AI:**

```kotlin
// BEFORE
class AiSmsParserService @Inject constructor(
    private val openAiParser: OpenAiParser,
    private val geminiParser: GeminiParser
) {
    suspend fun parseSms(smsText: String): ParsedTransaction? {
        // Direct API calls with embedded keys
        return openAiParser.parse(smsText) ?: geminiParser.parse(smsText)
    }
}

// AFTER
class AiSmsParserService @Inject constructor(
    private val edgeFunctionsApi: EdgeFunctionsApi,
    private val authRepository: AuthRepository
) {
    suspend fun parseSms(smsText: String): ParsedTransaction? {
        val userId = authRepository.currentUserId()
        val response = edgeFunctionsApi.parseSmsAi(
            smsText = smsText,
            userId = userId
        )
        return if (response.success) response.transaction else null
    }
}
```

---

## 🧪 TESTING

### Test Edge Function

```bash
# Test with curl
curl -X POST \
  https://lhbowpbcpwoiparwnwgt.supabase.co/functions/v1/parse-sms-ai \
  -H "Authorization: Bearer YOUR_SUPABASE_ANON_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "smsText": "You have received RWF 5,000 from John Doe. Your balance is RWF 15,000",
    "userId": "test-user-123",
    "provider": "auto"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "transaction": {
    "amount": 5000,
    "currency": "RWF",
    "transactionType": "DEPOSIT",
    "provider": "MTN",
    "balance": 15000,
    "recipientName": "John Doe",
    "success": true
  }
}
```

### Test in Android App

1. Build app without API keys
2. Send test SMS
3. Verify parsing works via Edge Function
4. Check Supabase logs for Edge Function calls

---

## ✅ VERIFICATION

### Security Checklist

- [ ] Edge Function `parse-sms-ai` created
- [ ] API keys set as Supabase secrets
- [ ] Edge Function deployed successfully
- [ ] API keys removed from `build.gradle.kts`
- [ ] Android code updated to use Edge Function
- [ ] Tested SMS parsing works
- [ ] Verified no API keys in APK (decompile to check)

### Verify No Keys in APK

```bash
# Build release APK
./gradlew assembleRelease

# Decompile and search for keys
unzip -p app/build/outputs/apk/release/app-release.apk classes.dex | strings | grep -i "api_key"

# Should return NOTHING
```

---

## 💰 COST SAVINGS

**Before:** API keys exposed → Risk of $$$$ in unauthorized usage  
**After:** Keys secure on server → Only your app can use them

**Additional Benefits:**
- Can rotate keys without rebuilding app
- Can monitor/rate-limit usage server-side
- Can add usage analytics
- Can implement cost controls

---

## 📊 COMPLETION CRITERIA

✅ Edge Function deployed and tested  
✅ API keys removed from Android build  
✅ Android code uses Edge Function  
✅ SMS parsing works end-to-end  
✅ No keys found in decompiled APK  

---

## 🚀 DEPLOYMENT

### Production Deployment

```bash
# 1. Create Edge Function directory
mkdir -p supabase/functions/parse-sms-ai

# 2. Copy Edge Function code (from above)
# Create supabase/functions/parse-sms-ai/index.ts

# 3. Set secrets
supabase secrets set OPENAI_API_KEY=sk-...
supabase secrets set GEMINI_API_KEY=AIza...

# 4. Deploy function
supabase functions deploy parse-sms-ai

# 5. Update Android code (remove keys from build.gradle.kts)

# 6. Build and test
./gradlew clean assembleDebug
./gradlew installDebug

# 7. Verify security
# Decompile APK and confirm no API keys present
```

---

## 📝 ROLLBACK PLAN

If Edge Function fails:

1. Revert build.gradle.kts changes
2. Rebuild app with embedded keys (temporary)
3. Debug Edge Function issues
4. Redeploy once fixed

**DO NOT** ship production app with embedded API keys!

---

**Status:** READY TO IMPLEMENT  
**Priority:** CRITICAL  
**Time:** 2 hours  
**Next Step:** Create Edge Function and deploy

---

**Once complete, the app will be 100% production-ready with no security vulnerabilities!** 🎉
