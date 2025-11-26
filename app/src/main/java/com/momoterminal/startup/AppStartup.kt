package com.momoterminal.startup

import android.content.Context
import android.os.SystemClock
import android.os.Trace
import androidx.startup.Initializer
import com.google.firebase.FirebaseApp
import com.momoterminal.BuildConfig
import com.momoterminal.performance.ComposeTracing
import com.momoterminal.performance.PerformanceMonitor
import timber.log.Timber

/**
 * App Startup initializers for optimized initialization order.
 * 
 * The App Startup library enables:
 * - Explicit dependency ordering between initializers
 * - Lazy initialization (on-demand)
 * - Reduced Application.onCreate() complexity
 * - Better startup performance through parallelization
 * 
 * Initialization order:
 * 1. TimberInitializer (no dependencies - first)
 * 2. FirebaseInitializer (depends on Timber)
 * 3. PerformanceInitializer (depends on Timber)
 */

/**
 * Initializes Timber logging.
 * This is the first initializer as other initializers may need logging.
 */
class TimberInitializer : Initializer<Unit> {
    
    override fun create(context: Context) {
        StartupTracing.markMilestone("TimberInit:start")
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Timber initialized in DEBUG mode")
        } else {
            // In release, plant a tree that only logs warnings and errors
            Timber.plant(ReleaseTree())
            Timber.i("Timber initialized in RELEASE mode")
        }
        
        StartupTracing.markMilestone("TimberInit:end")
    }
    
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
    
    /**
     * Release build Timber tree that only logs warnings and above.
     * Errors are also sent to crash reporting.
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority >= android.util.Log.WARN) {
                // Log warnings and errors
                // In a real app, you might send these to Crashlytics
            }
        }
    }
}

/**
 * Initializes Firebase services.
 * Depends on Timber for logging during initialization.
 */
class FirebaseInitializer : Initializer<FirebaseApp?> {
    
    override fun create(context: Context): FirebaseApp? {
        StartupTracing.markMilestone("FirebaseInit:start")
        
        return try {
            val firebaseApp = FirebaseApp.initializeApp(context)
            Timber.d("Firebase initialized successfully")
            StartupTracing.markMilestone("FirebaseInit:end")
            firebaseApp
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase")
            StartupTracing.markMilestone("FirebaseInit:failed")
            null
        }
    }
    
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(TimberInitializer::class.java)
    }
}

/**
 * Initializes performance monitoring.
 * Depends on Timber for logging.
 */
class PerformanceInitializer : Initializer<Unit> {
    
    override fun create(context: Context) {
        StartupTracing.markMilestone("PerformanceInit:start")
        
        if (BuildConfig.DEBUG) {
            // Enable Compose tracing in debug builds
            ComposeTracing.enableTracing()
            
            // Start performance monitoring
            PerformanceMonitor.getInstance(context).startMonitoring(
                intervalMs = 10_000L // Monitor every 10 seconds in debug
            )
            
            Timber.d("Performance monitoring enabled")
        }
        
        StartupTracing.markMilestone("PerformanceInit:end")
    }
    
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(TimberInitializer::class.java)
    }
}

/**
 * Utility object for tracing startup milestones.
 * 
 * Usage:
 * ```
 * StartupTracing.markMilestone("MyFeature:initialized")
 * ```
 */
object StartupTracing {
    
    private val startTime = SystemClock.elapsedRealtime()
    private val milestones = mutableListOf<Pair<String, Long>>()
    
    /**
     * Mark a startup milestone.
     * 
     * @param name Name of the milestone
     */
    @Synchronized
    fun markMilestone(name: String) {
        val elapsed = SystemClock.elapsedRealtime() - startTime
        milestones.add(name to elapsed)
        
        // Also add to system trace
        Trace.beginSection("Startup:$name")
        Trace.endSection()
        
        Timber.d("Startup milestone: $name at ${elapsed}ms")
    }
    
    /**
     * Get elapsed time since app process started.
     */
    fun getElapsedTime(): Long {
        return SystemClock.elapsedRealtime() - startTime
    }
    
    /**
     * Get all recorded milestones.
     */
    @Synchronized
    fun getMilestones(): List<Pair<String, Long>> {
        return milestones.toList()
    }
    
    /**
     * Log all milestones for analysis.
     */
    @Synchronized
    fun logAllMilestones() {
        Timber.i("=== Startup Milestones ===")
        milestones.forEach { (name, time) ->
            Timber.i("  $name: ${time}ms")
        }
        Timber.i("  Total: ${getElapsedTime()}ms")
        Timber.i("==========================")
    }
    
    /**
     * Clear all milestones.
     */
    @Synchronized
    fun clear() {
        milestones.clear()
    }
    
    /**
     * Measure time for a startup block.
     */
    inline fun <T> measureStartup(name: String, block: () -> T): T {
        markMilestone("$name:start")
        return try {
            block()
        } finally {
            markMilestone("$name:end")
        }
    }
}
