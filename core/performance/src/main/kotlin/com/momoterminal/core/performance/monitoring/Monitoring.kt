package com.momoterminal.core.performance.monitoring

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application logger with debug toggle.
 */
@Singleton
class AppLogger @Inject constructor() {
    private var isDebugEnabled = false
    
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
    
    fun w(tag: String = "App", message: String) {
        Log.w(tag, message)
    }
    
    fun e(tag: String = "App", message: String, error: Throwable? = null) {
        if (error != null) {
            Log.e(tag, message, error)
        } else {
            Log.e(tag, message)
        }
    }
}

/**
 * Performance monitor interface.
 */
interface PerformanceMonitor {
    fun startTrace(name: String)
    fun stopTrace(name: String)
    fun recordMetric(name: String, value: Long)
}

/**
 * Default implementation that logs to Logcat.
 */
@Singleton
class LogcatPerformanceMonitor @Inject constructor(
    private val logger: AppLogger
) : PerformanceMonitor {
    
    private val traces = mutableMapOf<String, Long>()
    
    override fun startTrace(name: String) {
        traces[name] = System.currentTimeMillis()
        logger.d("Performance", "Started trace: $name")
    }
    
    override fun stopTrace(name: String) {
        val startTime = traces.remove(name)
        if (startTime != null) {
            val duration = System.currentTimeMillis() - startTime
            logger.d("Performance", "Trace $name took ${duration}ms")
        }
    }
    
    override fun recordMetric(name: String, value: Long) {
        logger.d("Performance", "Metric $name = $value")
    }
}
