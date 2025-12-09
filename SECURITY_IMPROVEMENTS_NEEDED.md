# Security Improvements Needed

## CRITICAL-007: API Keys Exposed in APK

### Current State
- Supabase Anon Key is embedded in `BuildConfig` at compile time
- OpenAI API Key is embedded in `BuildConfig` at compile time
- Gemini API Key is embedded in `BuildConfig` at compile time
- These keys are visible in decompiled APK

### Security Risk
- **Severity:** HIGH
- **Impact:** API keys can be extracted from APK and used maliciously
- **CVSS:** 7.5 (High)

### Current Location
- File: `app/build.gradle.kts` lines 85-100+

### Recommended Solution

#### Option 1: Move to Supabase Edge Functions (Recommended)
```
Client (Android App)
  ↓
  No API keys needed
  ↓
Supabase Edge Functions (runs server-side)
  ↓
  Uses environment variables (OPENAI_API_KEY, etc.)
  ↓
OpenAI/Gemini APIs
```

**Implementation:**
1. Create Edge Function: `parse-sms-ai`
2. Move AI parsing logic to Edge Function
3. Client sends SMS text, receives parsed data
4. API keys stay on server

#### Option 2: Use ProGuard Obfuscation (Partial Protection)
```kotlin
// app/proguard-rules.pro
-keepclassmembers class com.momoterminal.BuildConfig {
    <fields>;
}
-keep class com.momoterminal.BuildConfig { *; }

// Additional obfuscation
-repackageclasses 'o'
-allowaccessmodification
```

**Limitations:**
- Obfuscation != Encryption
- Keys still extractable by determined attacker

#### Option 3: Fetch Keys at Runtime from Secure Backend
```kotlin
// On app startup
val apiKeys = secureBackend.getApiKeys(deviceId, userId)
// Store in memory only, never persist
```

**Requirements:**
- Backend service to dispense keys
- Device authentication
- Rate limiting

### Immediate Mitigation (Production)

For production builds, keys should NOT be embedded:

```kotlin
// app/build.gradle.kts
buildTypes {
    getByName("release") {
        // Remove keys from BuildConfig
        buildConfigField("String", "OPENAI_API_KEY", "\"\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"\"")
        
        // Supabase Anon Key is public but should be protected by RLS
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"\"")
    }
}
```

**Runtime Check:**
```kotlin
// In app initialization
if (BuildConfig.OPENAI_API_KEY.isEmpty()) {
    // Fetch from secure backend or use Edge Functions
    aiService = EdgeFunctionAiService()
} else {
    // Dev mode only
    aiService = DirectAiService(BuildConfig.OPENAI_API_KEY)
}
```

### Note on Supabase Anon Key
- Supabase Anon Key is **designed** to be public
- Security comes from Row Level Security (RLS) policies
- However, it's best practice to rotate it periodically
- Consider implementing rate limiting at Supabase level

### Action Items
- [ ] Create `parse-sms-ai` Edge Function
- [ ] Move AI parsing to Edge Function
- [ ] Remove API keys from BuildConfig in release builds
- [ ] Implement runtime key fetching OR use Edge Functions exclusively
- [ ] Add ProGuard rules for additional obfuscation
- [ ] Set up key rotation schedule
- [ ] Add rate limiting to Supabase Edge Functions

### Timeline
- **Immediate:** Document security concern ✅ (this file)
- **Phase 1 (Week 1):** Move AI parsing to Edge Functions
- **Phase 2 (Week 2):** Remove keys from release builds
- **Phase 3 (Week 3):** Implement key rotation

### Related Files
- `app/build.gradle.kts` - BuildConfig definition
- `core/network/src/main/kotlin/com/momoterminal/ai/` - AI parsing clients
- `supabase/functions/` - Edge Functions directory

---
**Document Created:** 2025-12-09
**Status:** AWAITING IMPLEMENTATION
**Priority:** HIGH
