package com.momoterminal.capabilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Broadcast receiver that listens for device boot completion.
 * 
 * This receiver is triggered when the device finishes booting (BOOT_COMPLETED).
 * It can be used to:
 * - Schedule periodic background tasks using WorkManager
 * - Start a foreground service if needed
 * - Initialize app state that needs to persist across device restarts
 * 
 * Required permission in AndroidManifest.xml:
 *   <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
 * 
 * The receiver must also be declared in the manifest with:
 *   <receiver android:name=".capabilities.BootCompletedReceiver" android:exported="true">
 *       <intent-filter>
 *           <action android:name="android.intent.action.BOOT_COMPLETED" />
 *       </intent-filter>
 *   </receiver>
 * 
 * IMPORTANT: Keep the work done in onReceive() minimal and fast.
 * For long-running tasks, delegate to WorkManager or a foreground service.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Verify this is the boot completed action
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Timber.i("Boot completed broadcast received - scheduling startup tasks")

            // Schedule a WorkManager job for post-boot initialization
            // This is the recommended approach for API 26+ instead of starting a service directly
            schedulePostBootWork(context)

            // Log boot event timestamp for debugging
            val bootTime = System.currentTimeMillis()
            Timber.d("Device booted at: $bootTime")

            // Save the boot time to SharedPreferences so it can be displayed in the UI
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_BOOT_TIME, bootTime)
                .putBoolean(KEY_BOOT_RECEIVER_TRIGGERED, true)
                .apply()
        }
    }

    /**
     * Schedules a WorkManager job to run after boot.
     * 
     * WorkManager is the recommended way to perform background work that:
     * - Needs to be guaranteed to execute
     * - Can be deferred
     * - Doesn't need to run immediately
     * 
     * For this demo, we schedule a simple one-time work request that will
     * execute shortly after boot. In a real app, this could:
     * - Sync data with server
     * - Refresh cached content
     * - Schedule periodic sync jobs
     */
    private fun schedulePostBootWork(context: Context) {
        // Create a one-time work request with a small initial delay
        // to allow the system to stabilize after boot
        val bootWorkRequest = OneTimeWorkRequestBuilder<BootInitWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS) // Wait 10 seconds after boot
            .addTag(WORK_TAG_BOOT_INIT)
            .build()

        // Enqueue the work with KEEP policy to avoid duplicate work if somehow
        // the boot broadcast is received multiple times
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            bootWorkRequest
        )

        Timber.d("Post-boot WorkManager job scheduled")
    }

    companion object {
        private const val PREFS_NAME = "capabilities_demo_prefs"
        private const val KEY_LAST_BOOT_TIME = "last_boot_time"
        private const val KEY_BOOT_RECEIVER_TRIGGERED = "boot_receiver_triggered"
        private const val WORK_TAG_BOOT_INIT = "boot_init_work"
        private const val UNIQUE_WORK_NAME = "post_boot_initialization"
    }
}
