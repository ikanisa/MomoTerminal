package com.momoterminal.debug

import android.app.Application
import timber.log.Timber

/**
 * Debug-only Application class that configures development tools.
 * 
 * This class extends the main Application to add:
 * - LeakCanary for memory leak detection
 * - StrictMode for detecting policy violations
 * - Enhanced logging
 * 
 * Note: This is only included in debug builds via the debug source set.
 */
open class DebugApplication : Application() {

    override fun onCreate() {
        // Initialize StrictMode BEFORE super.onCreate() to catch violations in Application.onCreate()
        StrictModeConfig.enableStrictMode()
        
        super.onCreate()
        
        // Configure LeakCanary
        LeakCanaryConfig.configure(this)
        
        // Log debug initialization
        Timber.d("Debug tools initialized")
        Timber.d("LeakCanary: ${if (LeakCanaryConfig.isEnabled()) "ENABLED" else "DISABLED"}")
        Timber.d("StrictMode: ENABLED")
    }
}
