package com.momoterminal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * BroadcastReceiver that listens for incoming SMS messages
 * and relays Mobile Money related messages to a webhook.
 */
class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
        
        // Keywords to filter Mobile Money messages
        private val MOMO_KEYWORDS = listOf("MOMO", "MobileMoney", "MTN", "RWF", "received", "sent", "payment")
        
        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (smsMessage in messages) {
                val sender = smsMessage.displayOriginatingAddress ?: "Unknown"
                val body = smsMessage.messageBody ?: ""
                val timestamp = smsMessage.timestampMillis
                
                Log.d(TAG, "SMS received from: $sender")
                
                // Log all messages for testing, but only relay MOMO-related ones
                PaymentState.appendLog("SMS from $sender: ${body.take(50)}...")
                
                // Check if message is MOMO-related (case-insensitive)
                val isMomoMessage = MOMO_KEYWORDS.any { keyword ->
                    sender.contains(keyword, ignoreCase = true) ||
                    body.contains(keyword, ignoreCase = true)
                }
                
                if (isMomoMessage && PaymentState.webhookUrl.isNotBlank()) {
                    relayToWebhook(sender, body, timestamp, context)
                } else if (isMomoMessage) {
                    PaymentState.appendLog("Webhook not configured - skipping relay")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
            PaymentState.appendLog("SMS Error: ${e.message}")
        }
    }
    
    private fun relayToWebhook(sender: String, body: String, timestamp: Long, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("sender", sender)
                    put("body", body)
                    put("timestamp", timestamp)
                    put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                    put("app", "MomoTerminal")
                }
                
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = json.toString().toRequestBody(mediaType)
                
                val request = Request.Builder()
                    .url(PaymentState.webhookUrl)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "Webhook relay successful: ${response.code}")
                        PaymentState.appendLog("Webhook: Relayed successfully")
                    } else {
                        Log.w(TAG, "Webhook relay failed: ${response.code}")
                        PaymentState.appendLog("Webhook: Failed (${response.code})")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Webhook relay error", e)
                PaymentState.appendLog("Webhook Error: ${e.message}")
            }
        }
    }
}
