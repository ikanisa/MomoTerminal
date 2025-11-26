package com.momoterminal.sync

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.momoterminal.util.NetworkMonitor
import com.momoterminal.util.NetworkState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced manager class for scheduling and managing sync work.
 * Provides automatic sync on network availability, manual sync, and periodic background sync.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor
) {
    
    companion object {
        private const val SYNC_WORK_NAME = "momo_sync_work"
        private const val PERIODIC_SYNC_WORK_NAME = "momo_periodic_sync"
        private const val INITIAL_BACKOFF_DELAY_SECONDS = 30L
    }
    
    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    init {
        // Observe network state and trigger sync when network becomes available
        observeNetworkState()
        // Observe work state
        observeWorkState()
    }
    
    /**
     * Observe network state changes and trigger sync when online.
     */
    private fun observeNetworkState() {
        scope.launch {
            networkMonitor.networkState.collect { state ->
                when (state) {
                    is NetworkState.Available -> {
                        // Network is available, trigger sync for pending items
                        if (_syncState.value is SyncState.Offline) {
                            _syncState.value = SyncState.Idle
                            enqueueSyncNow()
                        }
                    }
                    is NetworkState.Unavailable -> {
                        _syncState.value = SyncState.Offline
                    }
                }
            }
        }
    }
    
    /**
     * Observe WorkManager state for sync work.
     */
    private fun observeWorkState() {
        scope.launch {
            val workInfoLiveData: LiveData<List<WorkInfo>> = 
                workManager.getWorkInfosForUniqueWorkLiveData(SYNC_WORK_NAME)
            
            // Note: In production, you would observe this LiveData properly
            // This is a simplified version for demonstration
        }
    }
    
    /**
     * Enqueue an immediate sync request with exponential backoff.
     * Used when a new SMS arrives to trigger instant upload if online.
     */
    fun enqueueSyncNow() {
        if (!networkMonitor.isConnected) {
            _syncState.value = SyncState.Offline
            return
        }
        
        _syncState.value = SyncState.Syncing()
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                INITIAL_BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()
        
        workManager.enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
    
    /**
     * Trigger a manual sync.
     * @return true if sync was enqueued, false if offline
     */
    fun triggerManualSync(): Boolean {
        if (!networkMonitor.isConnected) {
            _syncState.value = SyncState.Offline
            return false
        }
        
        enqueueSyncNow()
        return true
    }
    
    /**
     * Schedule a periodic sync every 15 minutes.
     * Ensures offline transactions are eventually synced.
     */
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                INITIAL_BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }
    
    /**
     * Update sync state based on work result.
     * Called from SyncWorker when sync completes.
     */
    fun onSyncComplete(success: Boolean, syncedCount: Int, errorMessage: String? = null) {
        _syncState.value = if (success) {
            SyncState.Success(syncedCount)
        } else {
            SyncState.Error(
                message = errorMessage ?: "Sync failed",
                isRetryable = true
            )
        }
    }
    
    /**
     * Reset sync state to idle.
     */
    fun resetState() {
        if (networkMonitor.isConnected) {
            _syncState.value = SyncState.Idle
        } else {
            _syncState.value = SyncState.Offline
        }
    }
    
    /**
     * Cancel all pending sync work.
     */
    fun cancelAll() {
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        _syncState.value = SyncState.Idle
    }
    
    /**
     * Get the current network connectivity status.
     */
    fun isOnline(): Boolean = networkMonitor.isConnected
}
