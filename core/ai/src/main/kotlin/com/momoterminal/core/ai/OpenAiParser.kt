package com.momoterminal.core.ai

import com.momoterminal.core.database.entity.SmsTransactionEntity
import com.momoterminal.core.database.entity.SmsTransactionType
import com.momoterminal.core.database.entity.SyncStatus
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenAI GPT-3.5-turbo parser for SMS transactions.
 * PRIMARY parser with 95-97% accuracy.
 */
@Singleton
class OpenAiParser @Inject constructor() : AiParserInterface {
    
    companion object {
        private const val TAG = "OpenAiParser"
        private const val TIMEOUT_SECONDS = 30L
    }
    
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
    
    override suspend fun parse(sender: String, body: String): ParsedTransaction? {
        if (!AiConfig.isOpenAiEnabled) {
            Timber.d("$TAG: OpenAI is disabled")
            return null
        }
        
        return try {
            val response = callOpenAiApi(sender, body)
            parseJsonResponse(response, body)?.let { entity ->
                ParsedTransaction(
                    entity = entity,
                    parsedBy = "openai",
                    confidence = 0.96f  // OpenAI has 95-97% accuracy
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: OpenAI parsing failed")
            null
        }
    }
    
    private fun callOpenAiApi(sender: String, body: String): String {
        val prompt = """
${AiConfig.TRANSACTION_EXTRACTION_PROMPT}

SMS Sender: $sender
SMS Body: $body
""".trimIndent()
        
        val requestBody = JSONObject().apply {
            put("model", AiConfig.OPENAI_MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.1)
            put("max_tokens", 500)
        }
        
        val request = Request.Builder()
            .url(AiConfig.OPENAI_ENDPOINT)
            .addHeader("Authorization", "Bearer ${AiConfig.openAiApiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("OpenAI API call failed: ${response.code} ${response.message}")
            }
            
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from OpenAI")
            
            val json = JSONObject(responseBody)
            val choices = json.getJSONArray("choices")
            if (choices.length() == 0) {
                throw Exception("No choices in OpenAI response")
            }
            
            return choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
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
                Timber.w("$TAG: No valid amount found in OpenAI response")
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
                parsedBy = "openai",
                aiConfidence = 0.96f
            )
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to parse OpenAI response JSON: $jsonString")
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
