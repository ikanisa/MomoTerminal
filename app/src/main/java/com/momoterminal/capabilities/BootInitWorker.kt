package com.momoterminal.capabilities

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * WorkManager worker that runs after device boot completion.
 * 
 * This worker is scheduled by BootCompletedReceiver and performs
 * any initialization tasks that need to happen after the device boots.
 * 
 * Uses Hilt for dependency injection with @HiltWorker annotation.
 * 
 * In a real application, this worker might:
 * - Sync pending data with the server
 * - Refresh authentication tokens
 * - Schedule periodic sync jobs
 * - Update cached data
 */
@HiltWorker
class BootInitWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Timber.i("BootInitWorker started - performing post-boot initialization")

        try {
            // Simulate some initialization work
            // In a real app, this could be:
            // - Syncing data with server
            // - Refreshing auth tokens
            // - Scheduling periodic jobs

            // Log the initialization
            val currentTime = System.currentTimeMillis()
            Timber.d("Post-boot initialization completed at: $currentTime")

            // Save the initialization time to SharedPreferences
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_INIT_TIME, currentTime)
                .putInt(KEY_BOOT_WORK_RUN_COUNT, getRunCount() + 1)
                .apply()

            Timber.i("BootInitWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "BootInitWorker failed")
            // Retry up to 3 times
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun getRunCount(): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BOOT_WORK_RUN_COUNT, 0)
    }

    companion object {
        private const val PREFS_NAME = "capabilities_demo_prefs"
        private const val KEY_LAST_INIT_TIME = "last_init_time"
        private const val KEY_BOOT_WORK_RUN_COUNT = "boot_work_run_count"
    }
}
