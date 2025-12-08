package com.momoterminal.core.ai

/**
 * Unified AI configuration for SMS parsing.
 * 
 * IMPORTANT: OpenAI is PRIMARY, Gemini is FALLBACK
 * This ensures higher accuracy for critical financial transactions.
 */
object AiConfig {
    
    // PRIMARY: OpenAI GPT-3.5-turbo (higher accuracy)
    private var _openAiApiKey: String = ""
    val openAiApiKey: String
        get() = _openAiApiKey
    
    private var _isOpenAiEnabled: Boolean = false
    val isOpenAiEnabled: Boolean
        get() = _isOpenAiEnabled
    
    const val OPENAI_MODEL = "gpt-3.5-turbo"
    const val OPENAI_ENDPOINT = "https://api.openai.com/v1/chat/completions"
    
    // FALLBACK: Google Gemini 1.5 Flash (cost-effective backup)
    private var _geminiApiKey: String = ""
    val geminiApiKey: String
        get() = _geminiApiKey
    
    private var _isGeminiEnabled: Boolean = false
    val isGeminiEnabled: Boolean
        get() = _isGeminiEnabled
    
    const val GEMINI_MODEL = "gemini-1.5-flash"
    
    // Parser priority order
    enum class ParserPriority {
        OPENAI,   // PRIMARY - 95-97% accuracy, $0.002/SMS
        GEMINI,   // FALLBACK - 93-95% accuracy, $0.0001/SMS
        REGEX     // FINAL FALLBACK - 80-85% accuracy, free
    }
    
    val parserOrder = listOf(
        ParserPriority.OPENAI,
        ParserPriority.GEMINI,
        ParserPriority.REGEX
    )
    
    private var _initialized = false
    
    /**
     * Initialize AI configuration with API keys.
     * This should be called from Application.onCreate() with BuildConfig values.
     * Can only be called once.
     */
    fun initialize(
        openAiKey: String,
        geminiKey: String,
        aiParsingEnabled: Boolean
    ) {
        if (_initialized) {
            return // Already initialized, ignore subsequent calls
        }
        
        _openAiApiKey = openAiKey
        _geminiApiKey = geminiKey
        
        _isOpenAiEnabled = aiParsingEnabled && openAiKey.isNotBlank()
        _isGeminiEnabled = geminiKey.isNotBlank()
        
        _initialized = true
    }
    
    /**
     * System prompt for transaction extraction.
     * Used by both OpenAI and Gemini for consistent parsing.
     */
    const val TRANSACTION_EXTRACTION_PROMPT = """
You are a Mobile Money SMS parser for East and Central Africa (Rwanda, DR Congo, Tanzania, Burundi, Zambia, Ghana). Extract transaction information from the SMS message.

Return a JSON object with these fields (use null if not found):
- amount_in_pesewas: Long (amount in smallest currency unit: multiply by 100 for RWF, CDF, TZS, BIF, ZMW, GHS, USD)
- currency: String (RWF, CDF, TZS, BIF, ZMW, GHS, USD)
- sender_phone: String (phone number of sender for received transactions)
- recipient_phone: String (phone number of recipient for sent transactions)
- transaction_id: String (transaction reference/ID)
- transaction_type: String (one of: RECEIVED, SENT, PAYMENT, WITHDRAWAL, DEPOSIT, AIRTIME, CASH_OUT, UNKNOWN)
- provider: String (one of: MTN, VODAFONE, AIRTELTIGO, AIRTEL, TIGO, VODACOM, HALOTEL, LUMICASH, ECOCASH, ORANGE, MPESA, UNKNOWN)
- balance_in_pesewas: Long (account balance in smallest currency unit if mentioned)
- timestamp: String (ISO 8601 format if date/time mentioned, otherwise null)

Only return the JSON object, no other text or markdown formatting.
"""
}
