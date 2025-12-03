package com.momoterminal.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.momoterminal.data.local.dao.SmsTransactionDao
import com.momoterminal.data.local.dao.WalletDao
import com.momoterminal.data.remote.api.WalletSyncApi
import com.momoterminal.data.remote.dto.SmsTransactionDto
import com.momoterminal.data.remote.dto.TokenTransactionDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for syncing wallet data to backend.
 * Handles: token transactions, SMS transactions, NFC tags.
 */
@HiltWorker
class WalletSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val walletDao: WalletDao,
    private val smsTransactionDao: SmsTransactionDao,
    private val walletSyncApi: WalletSyncApi
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "WalletSyncWorker"
        private const val WORK_NAME = "wallet_sync"

        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<WalletSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WalletSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting wallet sync")
        var hasErrors = false

        // Sync token transactions
        try {
            val unsyncedTxns = walletDao.getUnsyncedTransactions()
            if (unsyncedTxns.isNotEmpty()) {
                val walletId = unsyncedTxns.first().walletId
                val dtos = unsyncedTxns.map { TokenTransactionDto.fromEntity(it) }
                val response = walletSyncApi.syncTransactions(walletId, dtos)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val syncedIds = unsyncedTxns.map { it.id } - (response.body()?.failedIds?.toSet() ?: emptySet())
                    walletDao.markTransactionsSynced(syncedIds)
                    Log.d(TAG, "Synced ${syncedIds.size} token transactions")
                } else {
                    hasErrors = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token transaction sync failed", e)
            hasErrors = true
        }

        // Sync SMS transactions
        try {
            val unsyncedSms = smsTransactionDao.getUnsynced()
            if (unsyncedSms.isNotEmpty()) {
                val dtos = unsyncedSms.map { SmsTransactionDto.fromEntity(it) }
                val response = walletSyncApi.syncSmsTransactions(dtos)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val syncedIds = unsyncedSms.map { it.id } - (response.body()?.failedIds?.toSet() ?: emptySet())
                    smsTransactionDao.markSynced(syncedIds)
                    Log.d(TAG, "Synced ${syncedIds.size} SMS transactions")
                } else {
                    hasErrors = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMS transaction sync failed", e)
            hasErrors = true
        }

        return if (hasErrors) Result.retry() else Result.success()
    }
}
