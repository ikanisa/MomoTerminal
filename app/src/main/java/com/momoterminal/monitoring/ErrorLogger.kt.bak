package com.momoterminal.monitoring

import androidx.work.*
import com.momoterminal.data.preferences.UserPreferences
import com.momoterminal.data.remote.api.MomoApiService
import com.momoterminal.data.remote.dto.BatchErrorLogsRequest
import com.momoterminal.data.remote.dto.ErrorLogDto
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
 * Severity levels for error logging.
 */
enum class ErrorSeverity(val value: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CRITICAL("critical")
}

/**
 * Error logger for structured error reporting to backend.
 */
@Singleton
class ErrorLogger @Inject constructor(
    private val deviceInfoProvider: DeviceInfoProvider,
    private val userPreferences: UserPreferences,
    private val workManager: WorkManager
) {
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val errorQueue = mutableListOf<ErrorLogDto>()
    
    /**
     * Log an error with full context.
     */
    fun logError(
        type: String,
        message: String,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        stackTrace: String? = null,
        errorCode: String? = null,
        component: String? = null,
        functionName: String? = null,
        screenName: String? = null,
        userAction: String? = null,
        context: Map<String, Any>? = null
    ) {
        scope.launch {
            try {
                val deviceInfo = deviceInfoProvider.getDeviceInfo()
                val deviceUuid = userPreferences.getDeviceUuid() ?: "unknown"
                
                val errorLog = ErrorLogDto(
                    errorType = type,
                    errorCode = errorCode,
                    severity = severity.value,
                    errorMessage = message,
                    stackTrace = stackTrace,
                    errorContext = context,
                    component = component,
                    functionName = functionName,
                    screenName = screenName,
                    userAction = userAction,
                    deviceId = deviceUuid,
                    appVersion = deviceInfo.appVersion,
                    osVersion = deviceInfo.osVersion,
                    timestamp = Instant.now().toString()
                )
                
                synchronized(errorQueue) {
                    errorQueue.add(errorLog)
                    
                    // Upload immediately for critical errors
                    if (severity == ErrorSeverity.CRITICAL) {
                        scheduleUpload(expedited = true)
                    } else if (errorQueue.size >= 10) {
                        scheduleUpload(expedited = false)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to log error: $type")
            }
        }
    }
    
    /**
     * Log an exception with automatic stack trace extraction.
     */
    fun logException(
        exception: Throwable,
        severity: ErrorSeverity = ErrorSeverity.HIGH,
        component: String? = null,
        screenName: String? = null,
        userAction: String? = null,
        context: Map<String, Any>? = null
    ) {
        logError(
            type = exception::class.simpleName ?: "UnknownException",
            message = exception.message ?: "No message",
            severity = severity,
            stackTrace = exception.stackTraceToString(),
            component = component,
            screenName = screenName,
            userAction = userAction,
            context = context
        )
    }
    
    /**
     * Log network error.
     */
    fun logNetworkError(
        endpoint: String,
        statusCode: Int,
        errorMessage: String,
        context: Map<String, Any>? = null
    ) {
        logError(
            type = "NetworkError",
            message = errorMessage,
            severity = if (statusCode >= 500) ErrorSeverity.HIGH else ErrorSeverity.MEDIUM,
            errorCode = statusCode.toString(),
            component = "Network",
            context = (context ?: emptyMap()) + mapOf(
                "endpoint" to endpoint,
                "status_code" to statusCode
            )
        )
    }
    
    /**
     * Log NFC error.
     */
    fun logNfcError(
        errorCode: String,
        errorMessage: String,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        context: Map<String, Any>? = null
    ) {
        logError(
            type = "NfcError",
            message = errorMessage,
            severity = severity,
            errorCode = errorCode,
            component = "NFC",
            context = context
        )
    }
    
    /**
     * Log SMS parsing error.
     */
    fun logSmsParsingError(
        smsBody: String,
        sender: String,
        errorMessage: String,
        context: Map<String, Any>? = null
    ) {
        logError(
            type = "SmsParsingError",
            message = errorMessage,
            severity = ErrorSeverity.MEDIUM,
            component = "SMS",
            context = (context ?: emptyMap()) + mapOf(
                "sms_body" to smsBody,
                "sender" to sender
            )
        )
    }
    
    /**
     * Log database error.
     */
    fun logDatabaseError(
        operation: String,
        errorMessage: String,
        context: Map<String, Any>? = null
    ) {
        logError(
            type = "DatabaseError",
            message = errorMessage,
            severity = ErrorSeverity.HIGH,
            component = "Database",
            functionName = operation,
            context = context
        )
    }
    
    /**
     * Schedule upload of error logs.
     */
    private fun scheduleUpload(expedited: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val uploadWork = if (expedited) {
            OneTimeWorkRequestBuilder<ErrorLogUploadWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        } else {
            OneTimeWorkRequestBuilder<ErrorLogUploadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
        }
        
        workManager.enqueueUniqueWork(
            "error_log_upload",
            ExistingWorkPolicy.REPLACE,
            uploadWork
        )
    }
    
    /**
     * Get queued errors for upload.
     */
    internal fun getQueuedErrors(): List<ErrorLogDto> {
        return synchronized(errorQueue) {
            errorQueue.toList()
        }
    }
    
    /**
     * Clear queued errors after successful upload.
     */
    internal fun clearQueue() {
        synchronized(errorQueue) {
            errorQueue.clear()
        }
    }
}

/**
 * Worker to upload error logs in background.
 */
class ErrorLogUploadWorker @Inject constructor(
    context: android.content.Context,
    params: WorkerParameters,
    private val errorLogger: ErrorLogger,
    private val api: MomoApiService
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val errors = errorLogger.getQueuedErrors()
            if (errors.isEmpty()) {
                return Result.success()
            }
            
            val request = BatchErrorLogsRequest(errors = errors)
            val response = api.uploadErrorLogs(request)
            
            if (response.isSuccessful) {
                errorLogger.clearQueue()
                Timber.d("Error logs uploaded successfully: ${errors.size} errors")
                Result.success()
            } else {
                Timber.w("Error log upload failed: ${response.code()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error log upload exception")
            Result.retry()
        }
    }
}
