package com.momoterminal.sms

import com.google.gson.Gson
import com.momoterminal.sms.ai.AiSmsParser
import com.momoterminal.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processes incoming Mobile Money SMS:
 * 1. Parse with AI (OpenAI/Gemini)
 * 2. Match to registered vendor by MOMO number
 * 3. Save to Supabase vendor_sms_transactions table
 */
@Singleton
class VendorSmsProcessor @Inject constructor(
    private val aiParser: AiSmsParser,
    private val supabaseClient: SupabaseClient,
    private val gson: Gson
) {
    
    data class VendorMatch(
        val vendorId: String,
        val vendorName: String,
        val momoNumber: String,
        val whatsappNumber: String
    )
    
    /**
     * Process incoming SMS and save to Supabase.
     */
    suspend fun processSms(
        rawSms: String,
        senderAddress: String,
        receivedAt: Long,
        useOpenAI: Boolean = true,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Processing SMS from $senderAddress")
            
            // Step 1: Parse with AI
            val parsed = if (useOpenAI) {
                aiParser.parseWithOpenAI(rawSms, senderAddress, apiKey)
            } else {
                aiParser.parseWithGemini(rawSms, senderAddress, apiKey)
            }
            
            if (parsed == null) {
                Timber.w("Failed to parse SMS with AI")
                return@withContext Result.failure(Exception("AI parsing failed"))
            }
            
            Timber.d("AI parsed: $parsed")
            
            // Step 2: Match to vendor by payer phone (MOMO number)
            val vendor = matchVendorByMomoNumber(parsed.payerPhone ?: "")
            
            if (vendor == null) {
                Timber.w("No vendor found for MOMO number: ${parsed.payerPhone}")
                // Still save as unmatched transaction
            }
            
            // Step 3: Save to Supabase
            val transactionId = saveToSupabase(
                vendorId = vendor?.vendorId,
                rawSms = rawSms,
                senderAddress = senderAddress,
                receivedAt = receivedAt,
                parsed = parsed
            )
            
            Timber.i("Saved SMS transaction: $transactionId")
            Result.success(transactionId)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to process SMS")
            Result.failure(e)
        }
    }
    
    /**
     * Match vendor by their registered MOMO number.
     */
    private suspend fun matchVendorByMomoNumber(momoNumber: String): VendorMatch? {
        if (momoNumber.isBlank()) return null
        
        try {
            // Query Supabase for vendor with this MOMO number
            val response = supabaseClient.postgrest
                .from("sms_parsing_vendors")
                .select("id, vendor_name, payee_momo_number, whatsapp_e164")
                .eq("payee_momo_number", momoNumber)
                .single()
                .execute()
            
            if (response.data == null) {
                Timber.d("No vendor found for MOMO: $momoNumber")
                return null
            }
            
            val json = gson.fromJson(response.data, com.google.gson.JsonObject::class.java)
            
            return VendorMatch(
                vendorId = json.get("id").asString,
                vendorName = json.get("vendor_name").asString,
                momoNumber = json.get("payee_momo_number").asString,
                whatsappNumber = json.get("whatsapp_e164").asString
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Error matching vendor")
            return null
        }
    }
    
    /**
     * Save parsed SMS transaction to Supabase.
     */
    private suspend fun saveToSupabase(
        vendorId: String?,
        rawSms: String,
        senderAddress: String,
        receivedAt: Long,
        parsed: AiSmsParser.ParsedSmsData
    ): String {
        val transactionData = mapOf(
            "vendor_id" to vendorId,
            "raw_sms" to rawSms,
            "sender_address" to senderAddress,
            "received_at" to java.time.Instant.ofEpochMilli(receivedAt).toString(),
            "payer_name" to parsed.payerName,
            "payer_phone" to parsed.payerPhone,
            "amount" to parsed.amount,
            "currency" to parsed.currency,
            "txn_id" to parsed.transactionId,
            "txn_timestamp" to parsed.timestamp,
            "provider" to parsed.provider,
            "ai_confidence" to parsed.confidence,
            "parsed_json" to gson.toJson(parsed),
            "status" to if (vendorId != null) "matched" else "parsed"
        )
        
        val response = supabaseClient.postgrest
            .from("vendor_sms_transactions")
            .insert(transactionData)
            .select("id")
            .single()
            .execute()
        
        val json = gson.fromJson(response.data, com.google.gson.JsonObject::class.java)
        return json.get("id").asString
    }
}
