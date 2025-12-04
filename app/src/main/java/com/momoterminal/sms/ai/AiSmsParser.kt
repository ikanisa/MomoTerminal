package com.momoterminal.sms.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI-powered SMS parser using OpenAI GPT or Google Gemini.
 * Extracts structured transaction data from Mobile Money SMS.
 */
@Singleton
class AiSmsParser @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    
    data class ParsedSmsData(
        @SerializedName("payer_name") val payerName: String?,
        @SerializedName("payer_phone") val payerPhone: String?,
        @SerializedName("payer_phone_last3") val payerPhoneLast3: String?,
        @SerializedName("amount") val amount: Double?,
        @SerializedName("currency") val currency: String?,
        @SerializedName("transaction_id") val transactionId: String?,
        @SerializedName("timestamp") val timestamp: String?,
        @SerializedName("provider") val provider: String?, // mtn, vodafone, airteltigo
        @SerializedName("confidence") val confidence: Double = 0.0
    )
    
    private data class OpenAIRequest(
        val model: String = "gpt-3.5-turbo",
        val messages: List<Message>,
        val temperature: Double = 0.3,
        val response_format: ResponseFormat = ResponseFormat("json_object")
    )
    
    private data class Message(
        val role: String,
        val content: String
    )
    
    private data class ResponseFormat(
        val type: String
    )
    
    private data class OpenAIResponse(
        val choices: List<Choice>
    )
    
    private data class Choice(
        val message: Message
    )
    
    /**
     * Parse SMS using OpenAI GPT-3.5-turbo.
     */
    suspend fun parseWithOpenAI(
        rawSms: String,
        senderAddress: String,
        apiKey: String
    ): ParsedSmsData? = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are a Mobile Money SMS parser. Extract structured data from SMS messages.
                Return ONLY valid JSON with these fields:
                {
                  "payer_name": "Full name of sender",
                  "payer_phone": "Full phone number if available",
                  "payer_phone_last3": "Last 3 digits of phone",
                  "amount": 1000.50,
                  "currency": "RWF",
                  "transaction_id": "MP123456",
                  "timestamp": "ISO 8601 timestamp if available",
                  "provider": "mtn|vodafone|airteltigo",
                  "confidence": 0.95
                }
                If field not found, use null. For confidence, rate 0.0-1.0 based on data clarity.
            """.trimIndent()
            
            val userPrompt = """
                Parse this Mobile Money SMS:
                Sender: $senderAddress
                Message: $rawSms
            """.trimIndent()
            
            val request = OpenAIRequest(
                messages = listOf(
                    Message("system", systemPrompt),
                    Message("user", userPrompt)
                )
            )
            
            val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                Timber.e("OpenAI API error: ${response.code} - ${response.body?.string()}")
                return@withContext null
            }
            
            val responseBody = response.body?.string() ?: return@withContext null
            val openAIResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)
            val content = openAIResponse.choices.firstOrNull()?.message?.content ?: return@withContext null
            
            gson.fromJson(content, ParsedSmsData::class.java)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse SMS with OpenAI")
            null
        }
    }
    
    /**
     * Parse SMS using Google Gemini.
     */
    suspend fun parseWithGemini(
        rawSms: String,
        senderAddress: String,
        apiKey: String
    ): ParsedSmsData? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Parse this Mobile Money SMS and extract structured data.
                Return ONLY valid JSON with these fields:
                {
                  "payer_name": "Full name of sender",
                  "payer_phone": "Full phone number if available",
                  "payer_phone_last3": "Last 3 digits of phone",
                  "amount": 1000.50,
                  "currency": "RWF",
                  "transaction_id": "MP123456",
                  "timestamp": "ISO 8601 timestamp if available",
                  "provider": "mtn|vodafone|airteltigo",
                  "confidence": 0.95
                }
                
                SMS Sender: $senderAddress
                SMS Message: $rawSms
            """.trimIndent()
            
            val requestBody = """
                {
                  "contents": [{
                    "parts": [{
                      "text": ${gson.toJson(prompt)}
                    }]
                  }],
                  "generationConfig": {
                    "temperature": 0.3,
                    "responseMimeType": "application/json"
                  }
                }
            """.trimIndent().toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                Timber.e("Gemini API error: ${response.code} - ${response.body?.string()}")
                return@withContext null
            }
            
            val responseBody = response.body?.string() ?: return@withContext null
            
            // Extract JSON from Gemini response
            val jsonContent = extractJsonFromGeminiResponse(responseBody)
            gson.fromJson(jsonContent, ParsedSmsData::class.java)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse SMS with Gemini")
            null
        }
    }
    
    private fun extractJsonFromGeminiResponse(response: String): String {
        try {
            val jsonObject = gson.fromJson(response, com.google.gson.JsonObject::class.java)
            return jsonObject.getAsJsonArray("candidates")
                .get(0).asJsonObject
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).asJsonObject
                .get("text").asString
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract JSON from Gemini response")
            throw e
        }
    }
}
