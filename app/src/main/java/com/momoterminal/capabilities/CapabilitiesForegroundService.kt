package com.momoterminal.capabilities

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.momoterminal.R
import com.momoterminal.presentation.ComposeMainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground Service demonstration.
 * 
 * A foreground service is a service that the user is actively aware of and which
 * the system will not kill even under memory pressure. It must display a persistent
 * notification while running.
 * 
 * Required permissions in AndroidManifest.xml:
 *   <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
 *   <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
 * 
 * The service must be declared with a foregroundServiceType:
 *   <service
 *       android:name=".capabilities.CapabilitiesForegroundService"
 *       android:foregroundServiceType="dataSync" />
 * 
 * On Android 13+, you also need POST_NOTIFICATIONS permission to show notifications.
 * 
 * Use cases for foreground services:
 * - Music playback
 * - Location tracking
 * - Data synchronization
 * - File uploads/downloads
 * - Any long-running operation the user should be aware of
 */
class CapabilitiesForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var updateJob: Job? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate() {
        super.onCreate()
        Timber.i("CapabilitiesForegroundService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("CapabilitiesForegroundService started")

        when (intent?.action) {
            ACTION_STOP -> {
                Timber.i("Stop action received - stopping service")
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // Start as a foreground service with a notification
        val notification = createNotification("Service started at ${getCurrentTime()}")
        startForeground(NOTIFICATION_ID, notification)

        // Update the shared state so UI can observe it
        _isRunning.value = true
        _lastUpdate.value = "Service started at ${getCurrentTime()}"

        // Start periodic updates
        startPeriodicUpdates()

        // Return STICKY so the service restarts if the system kills it
        return START_STICKY
    }

    /**
     * Starts a coroutine that periodically updates the notification and broadcasts
     * the timestamp to any observers.
     */
    private fun startPeriodicUpdates() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            var counter = 0
            while (true) {
                delay(UPDATE_INTERVAL_MS)
                counter++
                
                val message = "Running for ${counter * 5}s - Updated: ${getCurrentTime()}"
                
                // Update notification
                updateNotification(message)
                
                // Update shared state for UI observation
                _lastUpdate.value = message
                
                Timber.d("Foreground service update: $message")
            }
        }
    }

    /**
     * Creates the notification channel required for Android 8.0+.
     * Notification channels allow users to customize notification behavior per channel.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Capabilities Demo Service",
                NotificationManager.IMPORTANCE_LOW // Low importance = no sound
            ).apply {
                description = "Notification channel for the capabilities demo foreground service"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Timber.d("Notification channel created: $CHANNEL_ID")
        }
    }

    /**
     * Creates the persistent notification shown while the service is running.
     */
    private fun createNotification(contentText: String): Notification {
        // Intent to open the app when notification is tapped
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, ComposeMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to stop the service from the notification
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CapabilitiesForegroundService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Capabilities Demo Service")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification) // Make sure this icon exists
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopIntent
            )
            .setOngoing(true) // Cannot be swiped away
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Updates the notification with new content.
     */
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getCurrentTime(): String = dateFormat.format(Date())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("CapabilitiesForegroundService destroyed")
        
        // Cancel all coroutines
        updateJob?.cancel()
        serviceScope.cancel()
        
        // Update shared state
        _isRunning.value = false
        _lastUpdate.value = "Service stopped at ${getCurrentTime()}"
    }

    companion object {
        private const val CHANNEL_ID = "capabilities_foreground_service"
        private const val NOTIFICATION_ID = 1001
        private const val UPDATE_INTERVAL_MS = 5000L // 5 seconds
        private const val ACTION_STOP = "com.momoterminal.capabilities.STOP_SERVICE"

        // Shared state for observing service status from UI
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        private val _lastUpdate = MutableStateFlow("Service not started")
        val lastUpdate: StateFlow<String> = _lastUpdate

        /**
         * Starts the foreground service.
         * On Android 8.0+, must use startForegroundService().
         */
        fun start(context: Context) {
            val intent = Intent(context, CapabilitiesForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stops the foreground service.
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, CapabilitiesForegroundService::class.java))
        }
    }
}
