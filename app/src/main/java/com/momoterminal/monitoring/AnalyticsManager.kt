package com.momoterminal.monitoring

import androidx.work.*
import com.momoterminal.data.preferences.UserPreferences
import com.momoterminal.data.remote.api.MomoApiService
import com.momoterminal.data.remote.dto.AnalyticsEventDto
import com.momoterminal.data.remote.dto.BatchAnalyticsRequest
import com.momoterminal.util.DeviceInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics manager for tracking user behavior and app usage.
 * Events are queued locally and uploaded in batches.
 */
@Singleton
class AnalyticsManager @Inject constructor(
    private val deviceInfoProvider: DeviceInfoProvider,
    private val userPreferences: UserPreferences,
    private val workManager: WorkManager
) {
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val eventQueue = mutableListOf<AnalyticsEventDto>()
    private var sessionId: String = generateSessionId()
    
    /**
     * Log a generic event.
     */
    fun logEvent(
        eventName: String,
        category: String? = null,
        action: String? = null,
        label: String? = null,
        value: Double? = null,
        properties: Map<String, Any>? = null,
        screenName: String? = null
    ) {
        scope.launch {
            try {
                val event = AnalyticsEventDto(
                    eventName = eventName,
                    eventCategory = category,
                    eventAction = action,
                    eventLabel = label,
                    eventValue = value,
                    eventProperties = properties,
                    screenName = screenName,
                    sessionId = sessionId,
                    timestamp = Instant.now().toString()
                )
                
                synchronized(eventQueue) {
                    eventQueue.add(event)
                    
                    // Upload when we have 20 events or more
                    if (eventQueue.size >= 20) {
                        scheduleUpload()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to log analytics event: $eventName")
            }
        }
    }
    
    /**
     * Log screen view event.
     */
    fun logScreenView(screenName: String) {
        logEvent(
            eventName = "screen_view",
            category = "navigation",
            action = "view",
            label = screenName,
            screenName = screenName
        )
    }
    
    /**
     * Log NFC event.
     */
    fun logNfcEvent(action: String, success: Boolean, errorCode: String? = null) {
        logEvent(
            eventName = "nfc_$action",
            category = "nfc",
            action = action,
            value = if (success) 1.0 else 0.0,
            properties = mapOf(
                "success" to success,
                "error_code" to (errorCode ?: "none")
            )
        )
    }
    
    /**
     * Log transaction event.
     */
    fun logTransaction(
        action: String,
        provider: String? = null,
        amount: Double? = null,
        success: Boolean = true
    ) {
        logEvent(
            eventName = "transaction_$action",
            category = "transaction",
            action = action,
            value = amount,
            properties = mapOf(
                "provider" to (provider ?: "unknown"),
                "success" to success
            )
        )
    }
    
    /**
     * Log sync event.
     */
    fun logSync(itemType: String, count: Int, success: Boolean) {
        logEvent(
            eventName = "sync_$itemType",
            category = "sync",
            action = "sync",
            label = itemType,
            value = count.toDouble(),
            properties = mapOf(
                "count" to count,
                "success" to success
            )
        )
    }
    
    /**
     * Log settings change.
     */
    fun logSettingsChange(setting: String, value: Any) {
        logEvent(
            eventName = "settings_changed",
            category = "settings",
            action = "change",
            label = setting,
            properties = mapOf(
                "setting" to setting,
                "value" to value
            )
        )
    }
    
    /**
     * Log authentication event.
     */
    fun logAuthEvent(action: String, method: String, success: Boolean) {
        logEvent(
            eventName = "auth_$action",
            category = "authentication",
            action = action,
            label = method,
            value = if (success) 1.0 else 0.0,
            properties = mapOf(
                "method" to method,
                "success" to success
            )
        )
    }
    
    /**
     * Start a new session (e.g., on app launch).
     */
    fun startSession() {
        sessionId = generateSessionId()
        logEvent(
            eventName = "session_start",
            category = "session",
            action = "start"
        )
    }
    
    /**
     * End current session (e.g., on app background).
     */
    fun endSession() {
        logEvent(
            eventName = "session_end",
            category = "session",
            action = "end"
        )
        scheduleUpload() // Upload remaining events
    }
    
    /**
     * Schedule immediate upload of queued events.
     */
    fun scheduleUpload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val uploadWork = OneTimeWorkRequestBuilder<AnalyticsUploadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniqueWork(
            "analytics_upload",
            ExistingWorkPolicy.REPLACE,
            uploadWork
        )
    }
    
    /**
     * Get queued events for upload.
     */
    internal fun getQueuedEvents(): List<AnalyticsEventDto> {
        return synchronized(eventQueue) {
            eventQueue.toList()
        }
    }
    
    /**
     * Clear queued events after successful upload.
     */
    internal fun clearQueue() {
        synchronized(eventQueue) {
            eventQueue.clear()
        }
    }
    
    private fun generateSessionId(): String {
        return "${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}

/**
 * Worker to upload analytics events in background.
 */
class AnalyticsUploadWorker @Inject constructor(
    context: android.content.Context,
    params: WorkerParameters,
    private val analyticsManager: AnalyticsManager,
    private val api: MomoApiService,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val userPreferences: UserPreferences
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val events = analyticsManager.getQueuedEvents()
            if (events.isEmpty()) {
                return Result.success()
            }
            
            val deviceUuid = userPreferences.getDeviceUuid() ?: "unknown"
            val request = BatchAnalyticsRequest(
                events = events,
                deviceId = deviceUuid
            )
            
            val response = api.uploadAnalytics(request)
            
            if (response.isSuccessful) {
                analyticsManager.clearQueue()
                Timber.d("Analytics uploaded successfully: ${events.size} events")
                Result.success()
            } else {
                Timber.w("Analytics upload failed: ${response.code()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Analytics upload error")
            Result.retry()
        }
    }
}
