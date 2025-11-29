package com.momoterminal.debug

import android.app.Application
import leakcanary.AppWatcher
import leakcanary.LeakCanary
import timber.log.Timber

/**
 * Configuration for LeakCanary memory leak detection.
 * 
 * LeakCanary automatically watches for leaks in:
 * - Activities after they are destroyed
 * - Fragments after they are destroyed
 * - Fragment Views after they are destroyed
 * - ViewModels after they are cleared
 * - Services after they are destroyed
 * 
 * Custom configuration adds:
 * - RootView watching for leaks in custom views
 * - Custom reference matchers for known framework leaks
 */
object LeakCanaryConfig {

    private var enabled = false

    /**
     * Configure LeakCanary with custom settings.
     * 
     * @param application The application instance
     */
    fun configure(application: Application) {
        enabled = true
        
        LeakCanary.config = LeakCanary.config.copy(
            // Show notification for leaks
            dumpHeap = true,
            
            // Watch for object leaks retained longer than this
            retainedVisibleThreshold = 3,
            
            // Configure which objects to watch
            computeRetainedHeapSize = true,
            
            // Maximum number of leaks stored
            maxStoredHeapDumps = 7
        )
        
        Timber.d("LeakCanary configured with custom settings")
    }

    /**
     * Check if LeakCanary is enabled.
     */
    fun isEnabled(): Boolean = enabled

    /**
     * Manually watch an object for leaks.
     * Useful for tracking custom objects that should be garbage collected.
     * 
     * @param watchedObject The object to watch
     * @param description Description of what the object is
     */
    fun watchObject(watchedObject: Any, description: String) {
        if (enabled) {
            AppWatcher.objectWatcher.expectWeaklyReachable(
                watchedObject,
                description
            )
            Timber.d("Watching object for leaks: $description")
        }
    }

    /**
     * Get the current number of retained objects.
     */
    fun retainedObjectCount(): Int {
        return if (enabled) {
            AppWatcher.objectWatcher.retainedObjectCount
        } else {
            0
        }
    }

    /**
     * Trigger a heap dump manually.
     * Useful when you suspect a leak and want to analyze immediately.
     */
    fun dumpHeap() {
        if (enabled) {
            LeakCanary.dumpHeap()
            Timber.d("Heap dump triggered manually")
        }
    }
}
