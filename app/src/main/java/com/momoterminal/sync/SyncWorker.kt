package com.momoterminal.sync

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.momoterminal.ai.AiSmsParserService
import com.momoterminal.config.AppConfig
import com.momoterminal.data.local.dao.TransactionDao
import com.momoterminal.supabase.PaymentResult
import com.momoterminal.supabase.SupabasePaymentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that syncs pending transactions to Supabase payments table
 * and to the configured webhook URL as secondary notification.
 * Takes "PENDING" items from the local database and pushes them to Supabase,
 * then optionally sends to configured gateway webhook.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionDao: TransactionDao,
    private val paymentRepository: SupabasePaymentRepository,
    private val aiSmsParserService: AiSmsParserService
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SyncWorker"
        
        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    override suspend fun doWork(): Result {
        val appConfig = AppConfig(applicationContext)
        val merchantCode = appConfig.getMerchantPhone()
        val deviceId = Build.MODEL
        
        val pendingTransactions = transactionDao.getPendingTransactions()
        
        Log.d(TAG, "Found ${pendingTransactions.size} pending transactions to sync")
        
        var hasErrors = false
        
        for (txn in pendingTransactions) {
            try {
                // Parse transaction using AI for structured data
                val parsedData = try {
                    aiSmsParserService.parseSmartly(txn.sender, txn.body)
                } catch (e: Exception) {
                    Log.w(TAG, "AI parsing failed for transaction ${txn.id}", e)
                    null
                }
                
                // Sync to Supabase payments table first
                val supabaseResult = paymentRepository.syncTransaction(
                    transaction = txn,
                    parsedData = parsedData,
                    deviceId = deviceId,
                    merchantCode = merchantCode
                )
                
                when (supabaseResult) {
                    is PaymentResult.Success -> {
                        Log.d(TAG, "Transaction ${txn.id} synced to Supabase")
                        // Mark as sent in local DB
                        transactionDao.updateStatus(txn.id, "SENT")
                    }
                    is PaymentResult.Error -> {
                        Log.w(TAG, "Supabase sync failed for ${txn.id}: ${supabaseResult.message}")
                        // Try webhook fallback
                        val webhookSuccess = syncToWebhook(txn, appConfig)
                        if (!webhookSuccess) {
                            Log.e(TAG, "Both Supabase and webhook sync failed for ${txn.id}")
                            hasErrors = true
                        }
                    }
                }
                
            } catch (e: Exception) {
                // Leave as PENDING, will retry
                Log.e(TAG, "Sync failed for ${txn.id}", e)
                hasErrors = true
            }
        }
        
        return if (hasErrors) Result.retry() else Result.success()
    }
    
    /**
     * Fallback: Sync transaction to webhook URL if Supabase sync fails.
     * @return true if sync was successful, false otherwise
     */
    private suspend fun syncToWebhook(
        txn: com.momoterminal.data.local.entity.TransactionEntity,
        appConfig: AppConfig
    ): Boolean {
        val gatewayUrl = appConfig.getGatewayUrl()
        val apiSecret = appConfig.getApiSecret()
        
        // If not configured, skip webhook (not considered a failure)
        if (gatewayUrl.isBlank()) {
            Log.w(TAG, "Gateway URL not configured, skipping webhook sync")
            return false
        }
        
        return try {
            // Construct JSON payload (using amountInPesewas for precision)
            val json = JSONObject().apply {
                put("sender", txn.sender)
                put("text", txn.body)
                put("timestamp", txn.timestamp)
                put("device", Build.MODEL)
                put("merchant", appConfig.getMerchantPhone())
                txn.amountInPesewas?.let { put("amount_in_pesewas", it) }
                txn.currency?.let { put("currency", it) }
                txn.transactionId?.let { put("transactionId", it) }
            }
            
            // POST to gateway
            val request = Request.Builder()
                .url(gatewayUrl)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("X-Api-Key", apiSecret)
                .addHeader("Content-Type", "application/json")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    transactionDao.updateStatus(txn.id, "SENT")
                    Log.d(TAG, "Transaction ${txn.id} synced to webhook successfully")
                    true
                } else {
                    Log.w(TAG, "Webhook sync failed for ${txn.id}: ${response.code}")
                    false
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Webhook sync failed for ${txn.id}", e)
            false
        }
    }
}
