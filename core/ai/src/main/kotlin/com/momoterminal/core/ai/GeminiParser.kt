package com.momoterminal.core.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.momoterminal.core.database.entity.SmsTransactionEntity
import com.momoterminal.core.database.entity.SmsTransactionType
import com.momoterminal.core.database.entity.SyncStatus
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Gemini 1.5 Flash parser for SMS transactions.
 * FALLBACK parser with 93-95% accuracy and 10x lower cost than OpenAI.
 */
@Singleton
class GeminiParser @Inject constructor() : AiParserInterface {
    
    companion object {
        private const val TAG = "GeminiParser"
    }
    
    private val model: GenerativeModel? by lazy {
        if (!AiConfig.isGeminiEnabled) {
            Timber.d("$TAG: Gemini is disabled or API key not configured")
            null
        } else {
            try {
                GenerativeModel(
                    modelName = AiConfig.GEMINI_MODEL,
                    apiKey = AiConfig.geminiApiKey,
                    generationConfig = generationConfig {
                        temperature = 0.1f
                        topK = 1
                        topP = 0.95f
                        maxOutputTokens = 1024
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Failed to initialize Gemini model")
                null
            }
        }
    }
    
    override suspend fun parse(sender: String, body: String): ParsedTransaction? {
        val geminiModel = this.model ?: return null
        
        return try {
            val prompt = """
${AiConfig.TRANSACTION_EXTRACTION_PROMPT}

SMS Sender: $sender
SMS Body: $body
""".trimIndent()
            
            val response = geminiModel.generateContent(
                content {
                    text(prompt)
                }
            )
            
            val responseText = response.text?.trim()
            if (responseText.isNullOrBlank()) {
                Timber.w("$TAG: Empty response from Gemini")
                return null
            }
            
            parseJsonResponse(responseText, body)?.let { entity ->
                ParsedTransaction(
                    entity = entity,
                    parsedBy = "gemini",
                    confidence = 0.94f  // Gemini has 93-95% accuracy
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Gemini parsing failed")
            null
        }
    }
    
    private fun parseJsonResponse(jsonString: String, rawMessage: String): SmsTransactionEntity? {
        return try {
            // Clean up the response (remove any markdown code blocks if present)
            val cleanJson = jsonString
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            val json = JSONObject(cleanJson)
            
            val amountInPesewas = json.optLong("amount_in_pesewas", -1)
            if (amountInPesewas < 0) {
                Timber.w("$TAG: No valid amount found in Gemini response")
                return null
            }
            
            val type = parseTransactionType(json.optString("transaction_type", "UNKNOWN"))
            val sender = when (type) {
                SmsTransactionType.RECEIVED -> json.optString("sender_phone").takeIf { it.isNotBlank() }
                else -> json.optString("recipient_phone").takeIf { it.isNotBlank() }
            } ?: ""
            
            SmsTransactionEntity(
                rawMessage = rawMessage,
                sender = sender,
                amount = amountInPesewas / 100.0,
                currency = json.optString("currency", "GHS"),
                type = type,
                balance = json.optLong("balance_in_pesewas", -1)
                    .takeIf { it >= 0 }
                    ?.let { it / 100.0 },
                reference = json.optString("transaction_id").takeIf { it.isNotBlank() },
                timestamp = System.currentTimeMillis(),
                synced = false,
                syncStatus = SyncStatus.PENDING,
                parsedBy = "gemini",
                aiConfidence = 0.94f
            )
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to parse Gemini response JSON: $jsonString")
            null
        }
    }
    
    private fun parseTransactionType(type: String): SmsTransactionType {
        return when (type.uppercase()) {
            "RECEIVED" -> SmsTransactionType.RECEIVED
            "SENT", "PAYMENT" -> SmsTransactionType.SENT
            "CASH_OUT", "WITHDRAWAL" -> SmsTransactionType.CASH_OUT
            "AIRTIME" -> SmsTransactionType.AIRTIME
            "DEPOSIT" -> SmsTransactionType.DEPOSIT
            else -> SmsTransactionType.UNKNOWN
        }
    }
}
