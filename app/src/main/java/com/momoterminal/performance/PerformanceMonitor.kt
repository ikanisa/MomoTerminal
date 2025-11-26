package com.momoterminal.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import android.os.Trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.RandomAccessFile

/**
 * Runtime performance monitoring utilities.
 * 
 * Provides monitoring for:
 * - Memory usage (Java heap, native heap, total)
 * - CPU usage
 * - Performance snapshots over time
 * 
 * Usage:
 * ```
 * val monitor = PerformanceMonitor.getInstance(context)
 * monitor.startMonitoring(intervalMs = 5000L)
 * 
 * // Get current snapshot
 * val snapshot = monitor.getCurrentSnapshot()
 * 
 * // Stop monitoring
 * monitor.stopMonitoring()
 * ```
 */
class PerformanceMonitor private constructor(private val context: Context) {
    
    private var monitoringJob: Job? = null
    private var lastSnapshot: PerformanceSnapshot? = null
    private var lastCpuTime: Long = 0
    private var lastAppCpuTime: Long = 0
    
    private val activityManager: ActivityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }
    
    /**
     * Start periodic performance monitoring.
     * 
     * @param intervalMs Interval between measurements in milliseconds
     * @param scope Coroutine scope for monitoring (defaults to IO dispatcher)
     * @param onSnapshot Callback invoked with each performance snapshot
     */
    fun startMonitoring(
        intervalMs: Long = DEFAULT_INTERVAL_MS,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        onSnapshot: ((PerformanceSnapshot) -> Unit)? = null
    ) {
        stopMonitoring()
        
        monitoringJob = scope.launch {
            Timber.d("Performance monitoring started (interval: ${intervalMs}ms)")
            
            while (isActive) {
                val snapshot = captureSnapshot()
                lastSnapshot = snapshot
                
                onSnapshot?.invoke(snapshot)
                logSnapshot(snapshot)
                
                delay(intervalMs)
            }
        }
    }
    
    /**
     * Stop performance monitoring.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        Timber.d("Performance monitoring stopped")
    }
    
    /**
     * Get the current performance snapshot.
     */
    fun getCurrentSnapshot(): PerformanceSnapshot {
        return captureSnapshot()
    }
    
    /**
     * Get the last captured snapshot from monitoring.
     */
    fun getLastSnapshot(): PerformanceSnapshot? = lastSnapshot
    
    /**
     * Capture a performance snapshot.
     */
    private fun captureSnapshot(): PerformanceSnapshot {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        
        // Memory metrics
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = memoryInfo.availMem
        val totalSystemMemory = memoryInfo.totalMem
        val lowMemory = memoryInfo.lowMemory
        
        // Native heap metrics
        val nativeHeapSize = Debug.getNativeHeapSize()
        val nativeHeapAllocated = Debug.getNativeHeapAllocatedSize()
        val nativeHeapFree = Debug.getNativeHeapFreeSize()
        
        // CPU usage
        val cpuUsage = calculateCpuUsage()
        
        return PerformanceSnapshot(
            timestamp = System.currentTimeMillis(),
            usedMemoryBytes = usedMemory,
            maxMemoryBytes = maxMemory,
            availableMemoryBytes = availableMemory,
            totalSystemMemoryBytes = totalSystemMemory,
            lowMemory = lowMemory,
            nativeHeapSizeBytes = nativeHeapSize,
            nativeHeapAllocatedBytes = nativeHeapAllocated,
            nativeHeapFreeBytes = nativeHeapFree,
            cpuUsagePercent = cpuUsage
        )
    }
    
    /**
     * Calculate CPU usage percentage for this process.
     */
    private fun calculateCpuUsage(): Float {
        return try {
            val pid = Process.myPid()
            val statFile = RandomAccessFile("/proc/$pid/stat", "r")
            val stat = statFile.readLine()
            statFile.close()
            
            val fields = stat.split(" ")
            if (fields.size < 17) return 0f
            
            val utime = fields[13].toLongOrNull() ?: 0L
            val stime = fields[14].toLongOrNull() ?: 0L
            val appCpuTime = utime + stime
            
            val cpuFile = RandomAccessFile("/proc/stat", "r")
            val cpuStat = cpuFile.readLine()
            cpuFile.close()
            
            val cpuFields = cpuStat.split(" ").filter { it.isNotEmpty() }
            if (cpuFields.size < 8) return 0f
            
            val cpuTime = (1..7).sumOf { cpuFields[it].toLongOrNull() ?: 0L }
            
            val usage = if (lastCpuTime > 0 && cpuTime > lastCpuTime) {
                val cpuTimeDiff = cpuTime - lastCpuTime
                val appCpuTimeDiff = appCpuTime - lastAppCpuTime
                (appCpuTimeDiff.toFloat() / cpuTimeDiff.toFloat()) * 100f
            } else {
                0f
            }
            
            lastCpuTime = cpuTime
            lastAppCpuTime = appCpuTime
            
            usage.coerceIn(0f, 100f)
        } catch (e: Exception) {
            Timber.w(e, "Failed to calculate CPU usage")
            0f
        }
    }
    
    /**
     * Log performance snapshot.
     */
    private fun logSnapshot(snapshot: PerformanceSnapshot) {
        val usedMB = snapshot.usedMemoryBytes / MB
        val maxMB = snapshot.maxMemoryBytes / MB
        val nativeAllocMB = snapshot.nativeHeapAllocatedBytes / MB
        val memoryPercent = (snapshot.usedMemoryBytes.toFloat() / snapshot.maxMemoryBytes * 100).toInt()
        
        if (snapshot.lowMemory || memoryPercent > 80) {
            Timber.w("Performance: Memory ${usedMB}MB/${maxMB}MB ($memoryPercent%), Native: ${nativeAllocMB}MB, CPU: ${String.format("%.1f", snapshot.cpuUsagePercent)}%")
        } else {
            Timber.d("Performance: Memory ${usedMB}MB/${maxMB}MB ($memoryPercent%), Native: ${nativeAllocMB}MB, CPU: ${String.format("%.1f", snapshot.cpuUsagePercent)}%")
        }
    }
    
    companion object {
        private const val DEFAULT_INTERVAL_MS = 5000L
        private const val MB = 1024L * 1024L
        
        @Volatile
        private var instance: PerformanceMonitor? = null
        
        /**
         * Get singleton instance of PerformanceMonitor.
         */
        fun getInstance(context: Context): PerformanceMonitor {
            return instance ?: synchronized(this) {
                instance ?: PerformanceMonitor(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Snapshot of performance metrics at a point in time.
 */
data class PerformanceSnapshot(
    /** Timestamp when the snapshot was taken */
    val timestamp: Long,
    
    /** Used Java heap memory in bytes */
    val usedMemoryBytes: Long,
    
    /** Maximum Java heap memory in bytes */
    val maxMemoryBytes: Long,
    
    /** Available system memory in bytes */
    val availableMemoryBytes: Long,
    
    /** Total system memory in bytes */
    val totalSystemMemoryBytes: Long,
    
    /** Whether the system is in low memory state */
    val lowMemory: Boolean,
    
    /** Native heap size in bytes */
    val nativeHeapSizeBytes: Long,
    
    /** Native heap allocated in bytes */
    val nativeHeapAllocatedBytes: Long,
    
    /** Native heap free in bytes */
    val nativeHeapFreeBytes: Long,
    
    /** CPU usage percentage */
    val cpuUsagePercent: Float
) {
    /**
     * Get memory usage as a percentage.
     */
    fun memoryUsagePercent(): Float {
        return (usedMemoryBytes.toFloat() / maxMemoryBytes * 100)
    }
    
    /**
     * Get used memory in megabytes.
     */
    fun usedMemoryMB(): Long = usedMemoryBytes / (1024L * 1024L)
    
    /**
     * Get max memory in megabytes.
     */
    fun maxMemoryMB(): Long = maxMemoryBytes / (1024L * 1024L)
    
    /**
     * Get native heap allocated in megabytes.
     */
    fun nativeHeapAllocatedMB(): Long = nativeHeapAllocatedBytes / (1024L * 1024L)
}

/**
 * Inline function to trace performance of a code block.
 * 
 * Usage:
 * ```
 * val result = tracePerformance("LoadData") {
 *     repository.loadData()
 * }
 * ```
 */
inline fun <T> tracePerformance(name: String, block: () -> T): T {
    val startTime = System.nanoTime()
    Trace.beginSection(name)
    
    return try {
        block()
    } finally {
        Trace.endSection()
        val duration = (System.nanoTime() - startTime) / 1_000_000.0
        if (duration > 16.0) { // Longer than one frame
            Timber.w("Performance: '$name' took ${String.format("%.2f", duration)}ms")
        } else {
            Timber.v("Performance: '$name' took ${String.format("%.2f", duration)}ms")
        }
    }
}

/**
 * Inline function to measure memory impact of a code block.
 * 
 * Usage:
 * ```
 * val (result, memoryDelta) = measureMemoryImpact {
 *     createLargeObject()
 * }
 * ```
 */
inline fun <T> measureMemoryImpact(block: () -> T): Pair<T, Long> {
    val runtime = Runtime.getRuntime()
    
    // Force garbage collection to get accurate baseline
    System.gc()
    Thread.sleep(100)
    
    val beforeMemory = runtime.totalMemory() - runtime.freeMemory()
    val result = block()
    val afterMemory = runtime.totalMemory() - runtime.freeMemory()
    
    val memoryDelta = afterMemory - beforeMemory
    Timber.d("Memory impact: ${memoryDelta / 1024}KB")
    
    return Pair(result, memoryDelta)
}
