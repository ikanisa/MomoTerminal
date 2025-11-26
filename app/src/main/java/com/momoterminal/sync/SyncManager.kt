package com.momoterminal.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Manager class for scheduling and managing sync work.
 * Provides methods to enqueue immediate sync, schedule periodic sync, and cancel work.
 */
class SyncManager(private val context: Context) {
    
    companion object {
        private const val SYNC_WORK_NAME = "momo_sync_work"
        private const val PERIODIC_SYNC_WORK_NAME = "momo_periodic_sync"
    }
    
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Enqueue an immediate sync request.
     * Used when a new SMS arrives to trigger instant upload if online.
     */
    fun enqueueSyncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
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
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }
    
    /**
     * Cancel all pending sync work.
     */
    fun cancelAll() {
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
    }
}
