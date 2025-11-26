package com.momoterminal.webhook

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.momoterminal.data.local.dao.SmsDeliveryLogDao
import com.momoterminal.data.local.dao.WebhookConfigDao
import com.momoterminal.data.local.entity.SmsDeliveryLogEntity
import com.momoterminal.data.local.entity.WebhookConfigEntity
import com.momoterminal.util.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for dispatching SMS to configured webhooks.
 * Handles routing based on phone number, HMAC signing, and delivery tracking.
 */
@Singleton
class WebhookDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webhookConfigDao: WebhookConfigDao,
    private val smsDeliveryLogDao: SmsDeliveryLogDao,
    private val hmacSigner: HmacSigner,
    private val networkMonitor: NetworkMonitor
) {
    
    companion object {
        private const val TAG = "WebhookDispatcher"
        private const val PAYLOAD_VERSION = "1.0"
        private const val PAYLOAD_SOURCE = "momoterminal"
        private const val MAX_RESPONSE_BODY_LENGTH = 1000
        
        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    
    /**
     * Get the device ID for identifying the terminal.
     */
    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: Build.SERIAL
            ?: "unknown_device"
    }
    
    /**
     * Dispatch an SMS to all matching webhooks based on phone number.
     * Delivery to multiple webhooks is performed in parallel for better performance.
     *
     * @param phoneNumber The phone number the SMS was received on
     * @param sender The sender of the SMS
     * @param message The SMS message content
     * @return List of log IDs for the queued deliveries
     */
    suspend fun dispatchSms(
        phoneNumber: String,
        sender: String,
        message: String
    ): List<Long> = withContext(Dispatchers.IO) {
        val logIds = mutableListOf<Long>()
        
        try {
            // Find all active webhooks matching the phone number
            val matchingWebhooks = webhookConfigDao.getWebhooksByPhoneNumber(phoneNumber)
            
            val webhooksToUse = if (matchingWebhooks.isEmpty()) {
                Log.d(TAG, "No matching webhooks found for phone: $phoneNumber")
                // Fall back to wildcard/catch-all webhooks
                webhookConfigDao.getActiveWebhooksList().filter { 
                    it.phoneNumber.isBlank() || it.phoneNumber == "*" 
                }
            } else {
                matchingWebhooks
            }
            
            // Queue all deliveries
            for (webhook in webhooksToUse) {
                val logId = queueDelivery(webhook, phoneNumber, sender, message)
                logIds.add(logId)
            }
            
            // If online, attempt immediate delivery in parallel
            if (networkMonitor.isConnected && logIds.isNotEmpty()) {
                logIds.map { logId ->
                    async { deliverLog(logId) }
                }.awaitAll()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching SMS", e)
        }
        
        logIds
    }
    
    /**
     * Queue a delivery log for a webhook.
     */
    private suspend fun queueDelivery(
        webhook: WebhookConfigEntity,
        phoneNumber: String,
        sender: String,
        message: String
    ): Long {
        val log = SmsDeliveryLogEntity(
            webhookId = webhook.id,
            phoneNumber = phoneNumber,
            sender = sender,
            message = message,
            status = SmsDeliveryLogEntity.STATUS_PENDING
        )
        return smsDeliveryLogDao.insert(log)
    }
    
    /**
     * Attempt to deliver a pending log entry.
     *
     * @param logId The ID of the delivery log to process
     * @return True if delivery was successful
     */
    suspend fun deliverLog(logId: Long): Boolean = withContext(Dispatchers.IO) {
        val log = smsDeliveryLogDao.getById(logId) ?: return@withContext false
        val webhook = webhookConfigDao.getById(log.webhookId) ?: return@withContext false
        
        if (!webhook.isActive) {
            Log.d(TAG, "Webhook ${webhook.id} is inactive, skipping delivery")
            return@withContext false
        }
        
        try {
            val timestamp = System.currentTimeMillis()
            val payload = createPayload(log, timestamp)
            val signature = hmacSigner.signHex(payload, webhook.hmacSecret)
            
            val request = Request.Builder()
                .url(webhook.url)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Momo-Signature", signature)
                .addHeader("X-Momo-Timestamp", timestamp.toString())
                .addHeader("X-Momo-Device-Id", getDeviceId())
                .addHeader("Authorization", "Bearer ${webhook.apiKey}")
                .build()
            
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()?.take(MAX_RESPONSE_BODY_LENGTH)
                
                if (response.isSuccessful) {
                    smsDeliveryLogDao.updateDeliveryStatus(
                        id = logId,
                        status = SmsDeliveryLogEntity.STATUS_SENT,
                        responseCode = response.code,
                        responseBody = responseBody,
                        retryCount = log.retryCount,
                        sentAt = System.currentTimeMillis()
                    )
                    Log.d(TAG, "Successfully delivered log $logId to webhook ${webhook.id}")
                    return@withContext true
                } else {
                    smsDeliveryLogDao.updateDeliveryStatus(
                        id = logId,
                        status = SmsDeliveryLogEntity.STATUS_FAILED,
                        responseCode = response.code,
                        responseBody = responseBody,
                        retryCount = log.retryCount + 1,
                        sentAt = null
                    )
                    Log.w(TAG, "Failed to deliver log $logId: ${response.code}")
                    return@withContext false
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error delivering log $logId", e)
            smsDeliveryLogDao.updateDeliveryStatus(
                id = logId,
                status = SmsDeliveryLogEntity.STATUS_FAILED,
                responseCode = null,
                responseBody = e.message?.take(500),
                retryCount = log.retryCount + 1,
                sentAt = null
            )
            return@withContext false
        }
    }
    
    /**
     * Create the JSON payload for the webhook request.
     */
    private fun createPayload(log: SmsDeliveryLogEntity, timestamp: Long): String {
        val json = JSONObject().apply {
            put("source", PAYLOAD_SOURCE)
            put("version", PAYLOAD_VERSION)
            put("timestamp", isoDateFormat.format(Date(timestamp)))
            put("phone_number", log.phoneNumber)
            put("sender", log.sender)
            put("message", log.message)
            put("device_id", getDeviceId())
        }
        return json.toString()
    }
    
    /**
     * Retry all pending deliveries.
     *
     * @param maxRetries Maximum retry attempts before giving up
     * @return Number of successfully delivered items
     */
    suspend fun retryPendingDeliveries(maxRetries: Int = 5): Int = withContext(Dispatchers.IO) {
        if (!networkMonitor.isConnected) {
            Log.d(TAG, "No network connection, skipping retry")
            return@withContext 0
        }
        
        val pendingLogs = smsDeliveryLogDao.getPendingLogs(maxRetries)
        var successCount = 0
        
        for (log in pendingLogs) {
            if (deliverLog(log.id)) {
                successCount++
            }
        }
        
        Log.d(TAG, "Retry complete: $successCount/${pendingLogs.size} successful")
        successCount
    }
    
    /**
     * Send a test payload to verify webhook connectivity.
     *
     * @param webhook The webhook configuration to test
     * @return Pair of success status and response message
     */
    suspend fun testWebhook(webhook: WebhookConfigEntity): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val testPayload = JSONObject().apply {
                put("source", PAYLOAD_SOURCE)
                put("version", PAYLOAD_VERSION)
                put("timestamp", isoDateFormat.format(Date(timestamp)))
                put("test", true)
                put("message", "MomoTerminal webhook connectivity test")
                put("device_id", getDeviceId())
            }.toString()
            
            val signature = hmacSigner.signHex(testPayload, webhook.hmacSecret)
            
            val request = Request.Builder()
                .url(webhook.url)
                .post(testPayload.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Momo-Signature", signature)
                .addHeader("X-Momo-Timestamp", timestamp.toString())
                .addHeader("X-Momo-Device-Id", getDeviceId())
                .addHeader("Authorization", "Bearer ${webhook.apiKey}")
                .build()
            
            client.newCall(request).execute().use { response ->
                return@withContext if (response.isSuccessful) {
                    Pair(true, "Connection successful (${response.code})")
                } else {
                    Pair(false, "Server returned error: ${response.code}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Test webhook failed", e)
            return@withContext Pair(false, "Connection failed: ${e.message}")
        }
    }
}
