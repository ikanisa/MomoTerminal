package com.momoterminal.offline

import android.content.Context
import androidx.work.*
import com.momoterminal.core.logging.MomoLogger
import com.momoterminal.core.database.dao.TransactionDao
import com.momoterminal.data.local.dao.WalletDao
import com.momoterminal.sync.SyncWorker
import com.momoterminal.sync.WalletSyncWorker
import com.momoterminal.util.NetworkMonitor
import com.momoterminal.util.NetworkState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central coordinator for offline-first strategy.
 * 
 * Design principles:
 * - Local DB is always the source of truth
 * - Sync is best-effort, never blocks UI
 * - Idempotent operations via clientTransactionId
 * - Automatic retry with exponential backoff
 */
@Singleton
class OfflineFirstManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val transactionDao: TransactionDao,
    private val walletDao: WalletDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workManager = WorkManager.getInstance(context)
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()
    
    init {
        observeNetworkAndSync()
        observePendingTransactions()
    }
    
    private fun observeNetworkAndSync() {
        scope.launch {
            networkMonitor.networkState.collect { state ->
                when (state) {
                    NetworkState.Available -> {
                        MomoLogger.i(TAG, "Network available, triggering sync")
                        triggerImmediateSync()
                    }
                    NetworkState.Unavailable -> {
                        MomoLogger.i(TAG, "Network unavailable, sync paused")
                        _syncState.value = SyncState.Offline
                    }
                }
            }
        }
    }
    
    private fun observePendingTransactions() {
        scope.launch {
            transactionDao.getPendingCount().collect { count ->
                _pendingCount.value = count
            }
        }
    }
    
    /**
     * Schedule periodic sync with constraints.
     * Called once at app startup.
     */
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Transaction sync every 15 minutes
        val transactionSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // flex interval
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG_TRANSACTION_SYNC)
            .build()
        
        // Wallet sync every 30 minutes
        val walletSyncRequest = PeriodicWorkRequestBuilder<WalletSyncWorker>(
            30, TimeUnit.MINUTES,
            10, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG_WALLET_SYNC)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WORK_TRANSACTION_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            transactionSyncRequest
        )
        
        workManager.enqueueUniquePeriodicWork(
            WORK_WALLET_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            walletSyncRequest
        )
        
        MomoLogger.i(TAG, "Periodic sync scheduled")
    }
    
    /**
     * Trigger immediate sync when network becomes available.
     */
    fun triggerImmediateSync() {
        if (!networkMonitor.isConnected) {
            MomoLogger.d(TAG, "Skipping sync - no network")
            return
        }
        
        _syncState.value = SyncState.Syncing
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(TAG_IMMEDIATE_SYNC)
            .build()
        
        workManager.enqueueUniqueWork(
            WORK_IMMEDIATE_SYNC,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
        
        // Observe work completion
        scope.launch {
            workManager.getWorkInfoByIdFlow(syncRequest.id).collect { info ->
                when (info?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        _syncState.value = SyncState.Synced
                        MomoLogger.i(TAG, "Immediate sync completed")
                    }
                    WorkInfo.State.FAILED -> {
                        _syncState.value = SyncState.Error("Sync failed")
                        MomoLogger.w(TAG, "Immediate sync failed")
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * Force sync all pending data.
     */
    suspend fun forceSyncAll(): Result<Unit> {
        return try {
            _syncState.value = SyncState.Syncing
            
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            
            workManager.enqueue(syncRequest).await()
            
            _syncState.value = SyncState.Synced
            Result.success(Unit)
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            MomoLogger.e(TAG, "Force sync failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cancel all pending sync work.
     */
    fun cancelAllSync() {
        workManager.cancelAllWorkByTag(TAG_TRANSACTION_SYNC)
        workManager.cancelAllWorkByTag(TAG_WALLET_SYNC)
        workManager.cancelAllWorkByTag(TAG_IMMEDIATE_SYNC)
        _syncState.value = SyncState.Idle
    }
    
    companion object {
        private const val TAG = "OfflineFirst"
        private const val TAG_TRANSACTION_SYNC = "transaction_sync"
        private const val TAG_WALLET_SYNC = "wallet_sync"
        private const val TAG_IMMEDIATE_SYNC = "immediate_sync"
        private const val WORK_TRANSACTION_SYNC = "periodic_transaction_sync"
        private const val WORK_WALLET_SYNC = "periodic_wallet_sync"
        private const val WORK_IMMEDIATE_SYNC = "immediate_sync"
    }
}

sealed class SyncState {
    data object Idle : SyncState()
    data object Offline : SyncState()
    data object Syncing : SyncState()
    data object Synced : SyncState()
    data class Error(val message: String) : SyncState()
}
