package com.momoterminal.sync

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.momoterminal.config.AppConfig
import com.momoterminal.data.AppDatabase
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that syncs pending transactions to the configured webhook URL.
 * Takes "PENDING" items from the local database and pushes them to the configured gateway.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
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
        val gatewayUrl = appConfig.getGatewayUrl()
        val apiSecret = appConfig.getApiSecret()
        
        // If not configured, fail
        if (gatewayUrl.isBlank()) {
            Log.w(TAG, "Gateway URL not configured, skipping sync")
            return Result.failure()
        }
        
        val database = AppDatabase.getDatabase(applicationContext)
        val pendingTransactions = database.transactionDao().getPendingTransactions()
        
        Log.d(TAG, "Found ${pendingTransactions.size} pending transactions to sync")
        
        for (txn in pendingTransactions) {
            try {
                // Construct JSON payload
                val json = JSONObject().apply {
                    put("sender", txn.sender)
                    put("text", txn.body)
                    put("timestamp", txn.timestamp)
                    put("device", Build.MODEL)
                    put("merchant", appConfig.getMerchantPhone())
                }
                
                // POST to gateway
                val request = Request.Builder()
                    .url(gatewayUrl)
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("X-Api-Key", apiSecret)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    database.transactionDao().updateStatus(txn.id, "SENT")
                    Log.d(TAG, "Transaction ${txn.id} synced successfully")
                } else {
                    Log.w(TAG, "Sync failed for ${txn.id}: ${response.code}")
                    // Leave as PENDING for retry
                }
                
                response.close()
                
            } catch (e: Exception) {
                // Leave as PENDING, will retry
                Log.e(TAG, "Sync failed for ${txn.id}", e)
            }
        }
        
        return Result.success()
    }
}
