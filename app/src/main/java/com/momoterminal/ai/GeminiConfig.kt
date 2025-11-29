package com.momoterminal.ai

import com.momoterminal.BuildConfig

/**
 * Configuration for Google Gemini AI integration.
 */
object GeminiConfig {
    
    /**
     * Gemini API key loaded from BuildConfig.
     */
    val apiKey: String = BuildConfig.GEMINI_API_KEY
    
    /**
     * Check if Gemini AI parsing is enabled.
     * Requires both feature flag and valid API key.
     */
    val isEnabled: Boolean
        get() = BuildConfig.AI_PARSING_ENABLED && apiKey.isNotBlank()
    
    /**
     * Model name to use for SMS parsing.
     */
    const val MODEL_NAME = "gemini-1.5-flash"
    
    /**
     * System prompt for transaction extraction.
     */
    const val TRANSACTION_EXTRACTION_PROMPT = """
You are a Mobile Money SMS parser for Ghana. Extract transaction information from the SMS message.

Return a JSON object with these fields (use null if not found):
- amount_in_pesewas: Long (amount in pesewas, multiply GHS by 100)
- currency: String (default "GHS")
- sender_phone: String (phone number of sender for received transactions)
- recipient_phone: String (phone number of recipient for sent transactions)
- transaction_id: String (transaction reference/ID)
- transaction_type: String (one of: RECEIVED, SENT, PAYMENT, WITHDRAWAL, DEPOSIT, AIRTIME, UNKNOWN)
- provider: String (one of: MTN, VODAFONE, AIRTELTIGO, UNKNOWN)
- balance_in_pesewas: Long (account balance in pesewas if mentioned)
- timestamp: String (ISO 8601 format if date/time mentioned, otherwise null)

Only return the JSON object, no other text or markdown formatting.
"""
}
