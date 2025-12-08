package com.momoterminal.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.momoterminal.core.database.dao.SmsTransactionDao
import com.momoterminal.core.database.entity.SyncStatus
import com.momoterminal.supabase.SupabasePaymentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * WorkManager worker for syncing SMS transactions to Supabase.
 * - Fetches pending transactions from local Room database
 * - Syncs to Supabase vendor_sms_transactions table
 * - Updates local sync status on success/failure
 * - Implements retry logic for failures
 */
@HiltWorker
class SmsTransactionSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val smsTransactionDao: SmsTransactionDao,
    private val supabaseRepository: SupabasePaymentRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SmsTransactionSyncWorker"
        private const val MAX_RETRY_COUNT = 3
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.i("$TAG: Starting SMS transaction sync")
            
            // Fetch pending transactions
            val pendingTransactions = smsTransactionDao.getPendingSyncTransactions()
            
            if (pendingTransactions.isEmpty()) {
                Timber.d("$TAG: No pending transactions to sync")
                return Result.success()
            }
            
            Timber.i("$TAG: Found ${pendingTransactions.size} pending transactions")
            
            var successCount = 0
            var failureCount = 0
            
            // Sync each transaction
            for (transaction in pendingTransactions) {
                try {
                    // Update status to SYNCING
                    smsTransactionDao.updateSyncStatus(transaction.id, SyncStatus.SYNCING)
                    
                    // Sync to Supabase
                    val result = supabaseRepository.syncSmsTransaction(transaction)
                    
                    if (result.isSuccess) {
                        val supabaseId = result.getOrNull()
                        smsTransactionDao.updateSyncStatus(
                            transaction.id, 
                            SyncStatus.SYNCED, 
                            supabaseId
                        )
                        successCount++
                        Timber.d("$TAG: Successfully synced transaction ${transaction.id}")
                    } else {
                        handleSyncFailure(transaction)
                        failureCount++
                    }
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: Error syncing transaction ${transaction.id}")
                    handleSyncFailure(transaction)
                    failureCount++
                }
            }
            
            Timber.i("$TAG: Sync completed - Success: $successCount, Failed: $failureCount")
            
            // Return success if at least one transaction synced successfully
            if (successCount > 0) {
                Result.success()
            } else if (failureCount > 0 && runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.failure()
            }
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Worker failed")
            if (runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    private suspend fun handleSyncFailure(transaction: com.momoterminal.core.database.entity.SmsTransactionEntity) {
        val newRetryCount = transaction.retryCount + 1
        smsTransactionDao.updateRetryCount(transaction.id, newRetryCount)
        
        if (newRetryCount >= MAX_RETRY_COUNT) {
            smsTransactionDao.updateSyncStatus(transaction.id, SyncStatus.FAILED)
            Timber.w("$TAG: Transaction ${transaction.id} marked as FAILED after $newRetryCount retries")
        } else {
            smsTransactionDao.updateSyncStatus(transaction.id, SyncStatus.PENDING)
            Timber.d("$TAG: Transaction ${transaction.id} set back to PENDING for retry")
        }
    }
}
