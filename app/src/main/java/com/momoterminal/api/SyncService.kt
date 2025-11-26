package com.momoterminal.api

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Background service for syncing transactions to the PWA database.
 * Runs in the background to ensure reliable delivery of SMS data.
 */
class SyncService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SYNC_TRANSACTION -> {
                val transaction = getTransactionFromIntent(intent)
                transaction?.let { syncTransaction(it) }
            }
            ACTION_SYNC_ALL_PENDING -> {
                syncAllPending()
            }
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun getTransactionFromIntent(intent: Intent): PaymentTransaction? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_TRANSACTION, PaymentTransaction::class.java)
        } else {
            intent.getSerializableExtra(EXTRA_TRANSACTION) as? PaymentTransaction
        }
    }

    private fun syncTransaction(transaction: PaymentTransaction) {
        scope.launch {
            try {
                val apiEndpoint = getApiEndpoint()
                if (apiEndpoint.isNullOrEmpty()) {
                    Log.w(TAG, "API endpoint not configured, skipping sync")
                    return@launch
                }

                val response = ApiClient.getApiService(apiEndpoint).syncTransaction(transaction)
                if (response.isSuccessful) {
                    Log.d(TAG, "Transaction synced successfully: ${transaction.transactionId}")
                    broadcastSyncResult(transaction.transactionId, true)
                } else {
                    Log.e(TAG, "Failed to sync transaction: ${response.code()}")
                    broadcastSyncResult(transaction.transactionId, false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing transaction", e)
                broadcastSyncResult(transaction.transactionId, false)
            }
        }
    }

    private fun syncAllPending() {
        scope.launch {
            try {
                val apiEndpoint = getApiEndpoint()
                val merchantCode = getMerchantCode()
                
                if (apiEndpoint.isNullOrEmpty() || merchantCode.isNullOrEmpty()) {
                    Log.w(TAG, "API endpoint or merchant code not configured")
                    return@launch
                }

                val response = ApiClient.getApiService(apiEndpoint).getPendingTransactions(merchantCode)
                if (response.isSuccessful) {
                    response.body()?.forEach { transaction ->
                        // Process pending transactions
                        Log.d(TAG, "Found pending transaction: ${transaction.transactionId}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing pending transactions", e)
            }
        }
    }

    private fun getApiEndpoint(): String? {
        return getSharedPreferences("momo_terminal_prefs", MODE_PRIVATE)
            .getString("api_endpoint", null)
    }

    private fun getMerchantCode(): String? {
        return getSharedPreferences("momo_terminal_prefs", MODE_PRIVATE)
            .getString("merchant_code", null)
    }

    private fun broadcastSyncResult(transactionId: String, success: Boolean) {
        val intent = Intent(BROADCAST_SYNC_RESULT).apply {
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            putExtra(EXTRA_SYNC_SUCCESS, success)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        private const val TAG = "SyncService"
        const val ACTION_SYNC_TRANSACTION = "com.momoterminal.action.SYNC_TRANSACTION"
        const val ACTION_SYNC_ALL_PENDING = "com.momoterminal.action.SYNC_ALL_PENDING"
        const val EXTRA_TRANSACTION = "extra_transaction"
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
        const val EXTRA_SYNC_SUCCESS = "extra_sync_success"
        const val BROADCAST_SYNC_RESULT = "com.momoterminal.broadcast.SYNC_RESULT"
    }
}
