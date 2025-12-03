package com.momoterminal.core.performance.monitoring

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import javax.inject.Inject
import javax.inject.Singleton

// 1. APP LOGGER (with debug toggle)

@Singleton
class AppLogger @Inject constructor() {
    private var isDebugEnabled = BuildConfig.DEBUG
    
    fun enableDebug(enabled: Boolean) {
        isDebugEnabled = enabled
    }
    
    fun d(tag: String = "App", message: String) {
        if (isDebugEnabled) {
            Log.d(tag, message)
        }
    }
    
    fun i(tag: String = "App", message: String) {
        Log.i(tag, message)
    }
    
    fun w(tag: String = "App", message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
    }
    
    fun e(tag: String = "App", message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
    
    // Structured logging
    fun logEvent(event: String, params: Map<String, Any> = emptyMap()) {
        if (isDebugEnabled) {
            val paramsStr = params.entries.joinToString { "${it.key}=${it.value}" }
            d("Event", "$event: $paramsStr")
        }
    }
}

// 2. CRASH REPORTER

@Singleton
class CrashReporter @Inject constructor() {
    private val crashlytics = FirebaseCrashlytics.getInstance()
    
    fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }
    
    fun log(message: String) {
        crashlytics.log(message)
    }
    
    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }
    
    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
    
    fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }
    
    fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }
}

// 3. PERFORMANCE MONITOR

@Singleton
class PerformanceMonitor @Inject constructor(
    private val logger: AppLogger
) {
    private val firebasePerf = FirebasePerformance.getInstance()
    private val activeTraces = mutableMapOf<String, Trace>()
    
    fun startTrace(name: String): Trace {
        val trace = firebasePerf.newTrace(name)
        trace.start()
        activeTraces[name] = trace
        logger.d("Perf", "Started trace: $name")
        return trace
    }
    
    fun stopTrace(name: String) {
        activeTraces.remove(name)?.let { trace ->
            trace.stop()
            logger.d("Perf", "Stopped trace: $name")
        }
    }
    
    fun addTraceAttribute(name: String, key: String, value: String) {
        activeTraces[name]?.putAttribute(key, value)
    }
    
    fun incrementMetric(name: String, metric: String, value: Long = 1) {
        activeTraces[name]?.incrementMetric(metric, value)
    }
    
    // Convenience method for tracing a block
    suspend fun <T> trace(name: String, block: suspend () -> T): T {
        val trace = startTrace(name)
        return try {
            block()
        } finally {
            trace.stop()
        }
    }
}

// 4. NETWORK MONITORING

@Singleton
class NetworkMonitor @Inject constructor(
    private val performanceMonitor: PerformanceMonitor,
    private val logger: AppLogger
) {
    
    fun logRequest(url: String, method: String) {
        logger.d("Network", "$method $url")
    }
    
    fun logResponse(url: String, code: Int, durationMs: Long) {
        logger.d("Network", "Response $code from $url in ${durationMs}ms")
        
        // Track slow requests
        if (durationMs > 3000) {
            logger.w("Network", "Slow request: $url took ${durationMs}ms")
        }
    }
    
    fun logError(url: String, error: Throwable) {
        logger.e("Network", "Request failed: $url", error)
    }
}

// 5. INSTRUMENTED REPOSITORY

abstract class InstrumentedRepository<T>(
    private val performanceMonitor: PerformanceMonitor,
    private val logger: AppLogger
) {
    
    protected suspend fun <R> traced(
        operation: String,
        block: suspend () -> R
    ): R {
        logger.d("Repository", "Starting: $operation")
        val startTime = System.currentTimeMillis()
        
        return try {
            performanceMonitor.trace(operation) {
                block()
            }.also {
                val duration = System.currentTimeMillis() - startTime
                logger.d("Repository", "Completed: $operation in ${duration}ms")
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.e("Repository", "Failed: $operation after ${duration}ms", e)
            throw e
        }
    }
}

// Example usage
@Singleton
class ItemRepository @Inject constructor(
    private val dao: ItemDao,
    private val api: ItemApiService,
    performanceMonitor: PerformanceMonitor,
    logger: AppLogger
) : InstrumentedRepository<Item>(performanceMonitor, logger) {
    
    suspend fun getItems(): List<Item> = traced("fetch_items") {
        // Fetch logic
        api.getItems()
    }
    
    suspend fun getItemById(id: String): Item = traced("fetch_item_$id") {
        api.getItem(id)
    }
}

// 6. COMPOSE PERFORMANCE TRACKING

@Composable
fun TrackScreenView(screenName: String) {
    val performanceMonitor = remember { PerformanceMonitor() }
    
    DisposableEffect(screenName) {
        val trace = performanceMonitor.startTrace("screen_$screenName")
        
        onDispose {
            trace.stop()
        }
    }
}

// Usage
@Composable
fun MyScreen() {
    TrackScreenView("my_screen")
    
    // Screen content
}

// 7. IN-APP DEBUG MENU

@Composable
fun DebugMenu(
    appLogger: AppLogger,
    onDismiss: () -> Unit
) {
    var debugEnabled by remember { mutableStateOf(BuildConfig.DEBUG) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Debug Settings") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Debug Logs")
                    Switch(
                        checked = debugEnabled,
                        onCheckedChange = {
                            debugEnabled = it
                            appLogger.enableDebug(it)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        // Trigger test crash
                        throw RuntimeException("Test crash")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test Crash")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        // Clear cache
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Cache")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// 8. ANALYTICS EVENTS (domain-agnostic)

@Singleton
class AnalyticsTracker @Inject constructor(
    private val logger: AppLogger
) {
    
    fun trackScreenView(screenName: String) {
        logEvent("screen_view", mapOf("screen_name" to screenName))
    }
    
    fun trackAction(action: String, params: Map<String, Any> = emptyMap()) {
        logEvent("user_action", mapOf("action" to action) + params)
    }
    
    fun trackError(error: AppError) {
        logEvent("error", mapOf(
            "error_type" to error::class.simpleName,
            "error_message" to error.toUserMessage()
        ))
    }
    
    fun trackPerformance(metric: String, value: Long) {
        logEvent("performance", mapOf(
            "metric" to metric,
            "value" to value
        ))
    }
    
    private fun logEvent(event: String, params: Map<String, Any>) {
        logger.logEvent(event, params)
        // Also send to Firebase Analytics, Mixpanel, etc.
    }
}

// 9. NETWORK LOGGING INTERCEPTOR

class LoggingInterceptor @Inject constructor(
    private val networkMonitor: NetworkMonitor
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        networkMonitor.logRequest(request.url.toString(), request.method)
        
        return try {
            val response = chain.proceed(request)
            val duration = System.currentTimeMillis() - startTime
            
            networkMonitor.logResponse(
                request.url.toString(),
                response.code,
                duration
            )
            
            response
        } catch (e: Exception) {
            networkMonitor.logError(request.url.toString(), e)
            throw e
        }
    }
}

// 10. STARTUP PERFORMANCE TRACKING

@Singleton
class StartupTracker @Inject constructor(
    private val performanceMonitor: PerformanceMonitor,
    private val analyticsTracker: AnalyticsTracker
) {
    private val startTime = System.currentTimeMillis()
    
    fun onFirstFrameRendered() {
        val duration = System.currentTimeMillis() - startTime
        analyticsTracker.trackPerformance("cold_start_ms", duration)
        performanceMonitor.incrementMetric("app_start", "duration_ms", duration)
    }
    
    fun onContentReady() {
        val duration = System.currentTimeMillis() - startTime
        analyticsTracker.trackPerformance("time_to_interactive_ms", duration)
    }
}
