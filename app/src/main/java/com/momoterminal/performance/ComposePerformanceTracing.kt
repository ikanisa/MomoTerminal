package com.momoterminal.performance

import android.os.Trace
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import timber.log.Timber

/**
 * Compose performance tracing utilities.
 * 
 * These tools help identify:
 * - Excessive recompositions
 * - Slow composable functions
 * - Performance bottlenecks in UI code
 * 
 * Integration with Perfetto:
 * 1. Record a trace with Perfetto or Android Studio Profiler
 * 2. Look for traced sections in the timeline
 * 3. Analyze recomposition counts in logcat
 */

/**
 * Tracks recompositions of a composable function.
 * 
 * Usage:
 * ```
 * @Composable
 * fun MyScreen() {
 *     val tracker = rememberRecompositionTracker("MyScreen")
 *     // ... composable content
 * }
 * ```
 * 
 * @param name Name to identify this composable in logs
 * @param threshold Number of recompositions before logging a warning
 */
class RecompositionTracker(
    private val name: String,
    private val threshold: Int = 5
) {
    private var recompositionCount = 0
    private var lastLogTime = System.currentTimeMillis()
    
    /**
     * Call this in a SideEffect to track recompositions.
     */
    fun trackRecomposition() {
        recompositionCount++
        val currentTime = System.currentTimeMillis()
        
        // Log recomposition count periodically
        if (currentTime - lastLogTime > LOG_INTERVAL_MS) {
            if (recompositionCount > threshold) {
                Timber.w("Performance: '$name' recomposed $recompositionCount times in ${LOG_INTERVAL_MS}ms (threshold: $threshold)")
            } else {
                Timber.d("Performance: '$name' recomposed $recompositionCount times")
            }
            recompositionCount = 0
            lastLogTime = currentTime
        }
    }
    
    /**
     * Get current recomposition count.
     */
    fun getCount(): Int = recompositionCount
    
    /**
     * Reset the recomposition counter.
     */
    fun reset() {
        recompositionCount = 0
        lastLogTime = System.currentTimeMillis()
    }
    
    companion object {
        private const val LOG_INTERVAL_MS = 1000L
    }
}

/**
 * Remember a recomposition tracker for a composable.
 */
@Composable
fun rememberRecompositionTracker(name: String, threshold: Int = 5): RecompositionTracker {
    return remember(name) { RecompositionTracker(name, threshold) }
}

/**
 * Composable wrapper that tracks recompositions.
 * 
 * Usage:
 * ```
 * TrackRecomposition("MyComposable") {
 *     // Your composable content
 * }
 * ```
 */
@Composable
fun TrackRecomposition(
    name: String,
    threshold: Int = 5,
    content: @Composable () -> Unit
) {
    val tracker = rememberRecompositionTracker(name, threshold)
    SideEffect {
        tracker.trackRecomposition()
    }
    content()
}

/**
 * Composable that traces execution time for integration with Perfetto.
 * 
 * Usage:
 * ```
 * TracedComposable("ExpensiveOperation") {
 *     // Your composable content
 * }
 * ```
 */
@Composable
fun TracedComposable(
    traceName: String,
    content: @Composable () -> Unit
) {
    Trace.beginSection("Compose:$traceName")
    try {
        content()
    } finally {
        Trace.endSection()
    }
}

/**
 * Log recomposition timing for a composable.
 * Useful for identifying slow composables.
 * 
 * Usage:
 * ```
 * @Composable
 * fun MyScreen() {
 *     LogRecomposition("MyScreen")
 *     // ... composable content
 * }
 * ```
 */
@Composable
fun LogRecomposition(name: String) {
    val startTime = remember { System.nanoTime() }
    
    SideEffect {
        val duration = (System.nanoTime() - startTime) / 1_000_000.0
        if (duration > SLOW_COMPOSABLE_THRESHOLD_MS) {
            Timber.w("Slow recomposition: '$name' took ${String.format("%.2f", duration)}ms")
        } else {
            Timber.v("Recomposition: '$name' took ${String.format("%.2f", duration)}ms")
        }
    }
}

private const val SLOW_COMPOSABLE_THRESHOLD_MS = 16.0 // One frame at 60fps

/**
 * Extension function to trace a composable lambda with Perfetto.
 * 
 * Usage:
 * ```
 * val tracedContent = remember {
 *     { MyExpensiveComposable() }.traced("ExpensiveComposable")
 * }
 * ```
 */
inline fun <T> (() -> T).traced(name: String): () -> T = {
    Trace.beginSection(name)
    try {
        this()
    } finally {
        Trace.endSection()
    }
}

/**
 * Object for managing system tracing integration.
 */
object ComposeTracing {
    
    private var tracingEnabled = false
    
    /**
     * Enable tracing for all composables.
     * Call this in Application.onCreate() for debug builds.
     */
    fun enableTracing() {
        tracingEnabled = true
        Timber.d("Compose tracing enabled")
    }
    
    /**
     * Check if tracing is enabled.
     */
    fun isEnabled(): Boolean = tracingEnabled
    
    /**
     * Begin a trace section.
     * Use for non-composable performance-critical sections.
     */
    fun beginSection(name: String) {
        if (tracingEnabled) {
            Trace.beginSection(name)
        }
    }
    
    /**
     * End a trace section.
     */
    fun endSection() {
        if (tracingEnabled) {
            Trace.endSection()
        }
    }
    
    /**
     * Trace a block of code.
     */
    inline fun <T> trace(name: String, block: () -> T): T {
        if (tracingEnabled) {
            Trace.beginSection(name)
        }
        return try {
            block()
        } finally {
            if (tracingEnabled) {
                Trace.endSection()
            }
        }
    }
}
