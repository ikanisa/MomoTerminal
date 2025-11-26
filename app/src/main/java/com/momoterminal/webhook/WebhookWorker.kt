package com.momoterminal.webhook

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for processing webhook deliveries in the background.
 * Handles retry logic with exponential backoff and network connectivity requirements.
 */
@HiltWorker
class WebhookWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val webhookDispatcher: WebhookDispatcher
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "WebhookWorker"
        private const val WORK_NAME = "webhook_delivery_work"
        private const val PERIODIC_WORK_NAME = "webhook_periodic_retry"
        private const val MAX_RETRIES = 5
        private const val INITIAL_BACKOFF_SECONDS = 30L
        
        /**
         * Enqueue an immediate one-time webhook delivery work request.
         */
        fun enqueueNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<WebhookWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    INITIAL_BACKOFF_SECONDS,
                    TimeUnit.SECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            
            Log.d(TAG, "Enqueued immediate webhook delivery work")
        }
        
        /**
         * Schedule periodic retry work every 15 minutes.
         */
        fun schedulePeriodicRetry(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val periodicWork = PeriodicWorkRequestBuilder<WebhookWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    INITIAL_BACKOFF_SECONDS,
                    TimeUnit.SECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicWork
                )
            
            Log.d(TAG, "Scheduled periodic webhook retry work")
        }
        
        /**
         * Cancel all webhook work.
         */
        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).apply {
                cancelUniqueWork(WORK_NAME)
                cancelUniqueWork(PERIODIC_WORK_NAME)
            }
            Log.d(TAG, "Cancelled all webhook work")
        }
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting webhook delivery work")
        
        return try {
            val successCount = webhookDispatcher.retryPendingDeliveries(MAX_RETRIES)
            Log.d(TAG, "Webhook delivery work completed: $successCount deliveries successful")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Webhook delivery work failed", e)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
