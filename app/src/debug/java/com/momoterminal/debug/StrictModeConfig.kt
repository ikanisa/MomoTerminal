package com.momoterminal.debug

import android.os.Build
import android.os.StrictMode
import timber.log.Timber

/**
 * Configuration for StrictMode policy violations detection.
 * 
 * StrictMode helps detect:
 * - Disk reads/writes on main thread
 * - Network operations on main thread
 * - Resource leaks (closables, SQLite cursors)
 * - Activity leaks
 * - Cleartext network traffic
 * 
 * Violations are logged and can optionally flash the screen or crash.
 */
object StrictModeConfig {

    private var enabled = false

    /**
     * Enable StrictMode with comprehensive detection.
     * Call this early in Application.onCreate() before other initialization.
     */
    fun enableStrictMode() {
        enabled = true
        
        // Thread policy - detect main thread violations
        val threadPolicy = StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .detectCustomSlowCalls()
            .penaltyLog()
            .penaltyFlashScreen()
            .build()
        
        // VM policy - detect memory and resource leaks
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
            .detectLeakedClosableObjects()
            .detectLeakedSqlLiteObjects()
            .detectActivityLeaks()
            .penaltyLog()
        
        // Add API-specific detections
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vmPolicyBuilder.detectContentUriWithoutPermission()
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            vmPolicyBuilder.detectNonSdkApiUsage()
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vmPolicyBuilder.detectCredentialProtectedWhileLocked()
            vmPolicyBuilder.detectImplicitDirectBoot()
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vmPolicyBuilder.detectIncorrectContextUse()
            vmPolicyBuilder.detectUnsafeIntentLaunch()
        }
        
        StrictMode.setThreadPolicy(threadPolicy)
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
        
        Timber.d("StrictMode enabled with comprehensive detection")
    }

    /**
     * Check if StrictMode is enabled.
     */
    fun isEnabled(): Boolean = enabled

    /**
     * Temporarily permit disk reads on the current thread.
     * Use this sparingly for known-safe operations.
     * 
     * @param block The code block to execute with disk reads permitted
     * @return The result of the block
     */
    inline fun <T> permitDiskReads(block: () -> T): T {
        val oldPolicy = StrictMode.allowThreadDiskReads()
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
    }

    /**
     * Temporarily permit disk writes on the current thread.
     * Use this sparingly for known-safe operations.
     * 
     * @param block The code block to execute with disk writes permitted
     * @return The result of the block
     */
    inline fun <T> permitDiskWrites(block: () -> T): T {
        val oldPolicy = StrictMode.allowThreadDiskWrites()
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
    }

    /**
     * Temporarily permit all disk operations on the current thread.
     * Use this sparingly for known-safe operations.
     * 
     * @param block The code block to execute with disk operations permitted
     * @return The result of the block
     */
    inline fun <T> permitDiskOperations(block: () -> T): T {
        val oldPolicy = StrictMode.allowThreadDiskReads()
        StrictMode.allowThreadDiskWrites()
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
    }

    /**
     * Note a slow call for StrictMode detection.
     * Call this in methods that are unexpectedly slow to help identify issues.
     * 
     * @param name Name of the slow call for logging
     */
    fun noteSlowCall(name: String) {
        if (enabled) {
            StrictMode.noteSlowCall(name)
        }
    }
}
