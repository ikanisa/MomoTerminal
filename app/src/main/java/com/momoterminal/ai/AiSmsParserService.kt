package com.momoterminal.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.momoterminal.sms.SmsParser
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI-powered SMS parser service using Google Gemini.
 * Provides intelligent parsing of Mobile Money SMS messages with fallback to regex-based parsing.
 */
@Singleton
class AiSmsParserService @Inject constructor() {
    
    companion object {
        private const val TAG = "AiSmsParserService"
    }
    
    private val model: GenerativeModel? by lazy {
        if (!GeminiConfig.isEnabled) {
            Timber.d("Gemini AI is disabled or API key not configured")
            null
        } else {
            try {
                GenerativeModel(
                    modelName = GeminiConfig.MODEL_NAME,
                    apiKey = GeminiConfig.apiKey,
                    generationConfig = generationConfig {
                        temperature = 0.1f
                        topK = 1
                        topP = 0.95f
                        maxOutputTokens = 1024
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize Gemini model")
                null
            }
        }
    }
    
    /**
     * Check if AI parsing is available.
     */
    fun isAvailable(): Boolean = model != null
    
    /**
     * Parse SMS message using AI with fallback to regex parser.
     * 
     * @param sender The SMS sender address
     * @param body The SMS body text
     * @return ParsedTransaction if successfully parsed, null otherwise
     */
    suspend fun parseSmartly(sender: String, body: String): AiParsedTransaction? {
        // Try AI parsing first
        if (model != null) {
            try {
                val result = parseWithAi(sender, body)
                if (result != null) {
                    Timber.d("SMS parsed successfully using Gemini AI")
                    return result
                }
            } catch (e: Exception) {
                Timber.w(e, "AI parsing failed, falling back to regex")
            }
        }
        
        // Fallback to regex parser
        return parseWithRegex(sender, body)
    }
    
    /**
     * Parse SMS using Gemini AI.
     */
    private suspend fun parseWithAi(sender: String, body: String): AiParsedTransaction? {
        val model = this.model ?: return null
        
        try {
            val prompt = """
${GeminiConfig.TRANSACTION_EXTRACTION_PROMPT}

SMS Sender: $sender
SMS Body: $body
""".trimIndent()
            
            val response = model.generateContent(
                content {
                    text(prompt)
                }
            )
            
            val responseText = response.text?.trim()
            if (responseText.isNullOrBlank()) {
                Timber.w("Empty response from Gemini")
                return null
            }
            
            // Clean up the response (remove any markdown code blocks if present)
            val cleanJson = responseText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            return parseJsonResponse(cleanJson, body)
            
        } catch (e: Exception) {
            Timber.e(e, "Error during AI parsing")
            throw e
        }
    }
    
    /**
     * Parse JSON response from AI into AiParsedTransaction.
     */
    private fun parseJsonResponse(jsonString: String, rawMessage: String): AiParsedTransaction? {
        return try {
            val json = JSONObject(jsonString)
            
            val amountInPesewas = json.optLong("amount_in_pesewas", 0)
            if (amountInPesewas <= 0) {
                Timber.w("No valid amount found in AI response")
                return null
            }
            
            AiParsedTransaction(
                amountInPesewas = amountInPesewas,
                currency = json.optString("currency", "GHS"),
                senderPhone = json.optString("sender_phone").takeIf { it.isNotBlank() },
                recipientPhone = json.optString("recipient_phone").takeIf { it.isNotBlank() },
                transactionId = json.optString("transaction_id").takeIf { it.isNotBlank() },
                transactionType = json.optString("transaction_type", "UNKNOWN"),
                provider = json.optString("provider", "UNKNOWN"),
                balanceInPesewas = json.optLong("balance_in_pesewas", 0).takeIf { it > 0 },
                rawMessage = rawMessage,
                parsedBy = "gemini"
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse AI response JSON: $jsonString")
            null
        }
    }
    
    /**
     * Fallback to regex-based parsing.
     */
    private fun parseWithRegex(sender: String, body: String): AiParsedTransaction? {
        val result = SmsParser.parseSms(sender, body) ?: return null
        
        return AiParsedTransaction(
            amountInPesewas = result.amountInPesewas,
            currency = result.currency,
            senderPhone = if (result.transactionType == SmsParser.TransactionType.RECEIVED) {
                result.senderOrRecipient
            } else null,
            recipientPhone = if (result.transactionType != SmsParser.TransactionType.RECEIVED) {
                result.senderOrRecipient
            } else null,
            transactionId = result.transactionId,
            transactionType = result.transactionType.name,
            provider = result.provider,
            balanceInPesewas = result.balanceInPesewas,
            rawMessage = result.rawMessage,
            parsedBy = "regex"
        )
    }
}

/**
 * Transaction data parsed from SMS using AI or regex.
 */
data class AiParsedTransaction(
    val amountInPesewas: Long,
    val currency: String = "GHS",
    val senderPhone: String? = null,
    val recipientPhone: String? = null,
    val transactionId: String? = null,
    val transactionType: String,
    val provider: String,
    val balanceInPesewas: Long? = null,
    val rawMessage: String,
    val parsedBy: String = "gemini" // "gemini" or "regex"
) {
    /**
     * Get display amount in main currency unit.
     */
    fun getDisplayAmount(): Double = amountInPesewas / 100.0
    
    /**
     * Get display balance in main currency unit.
     */
    fun getDisplayBalance(): Double? = balanceInPesewas?.let { it / 100.0 }
}
