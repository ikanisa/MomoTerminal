package com.momoterminal.startup

import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

/**
 * Lazy initializer for heavy components.
 * Defers non-critical initialization until after first frame.
 * 
 * Usage:
 * ```
 * LazyInitializer.register("analytics") { initializeAnalytics() }
 * LazyInitializer.initializeAll() // Call after first frame
 * ```
 */
object LazyInitializer {
    
    private val initializers = ConcurrentHashMap<String, suspend () -> Unit>()
    private val initialized = ConcurrentHashMap<String, AtomicBoolean>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    /**
     * Register a lazy initializer.
     */
    fun register(name: String, initializer: suspend () -> Unit) {
        initializers[name] = initializer
        initialized[name] = AtomicBoolean(false)
    }
    
    /**
     * Initialize all registered components.
     * Call this after first frame is rendered.
     */
    fun initializeAll() {
        scope.launch {
            val totalTime = measureTimeMillis {
                initializers.entries.map { (name, init) ->
                    async {
                        initializeComponent(name, init)
                    }
                }.awaitAll()
            }
            Timber.i("All lazy components initialized in ${totalTime}ms")
        }
    }
    
    /**
     * Initialize a specific component immediately.
     */
    suspend fun initializeNow(name: String) {
        val init = initializers[name] ?: return
        initializeComponent(name, init)
    }
    
    private suspend fun initializeComponent(name: String, init: suspend () -> Unit) {
        val wasInitialized = initialized[name]?.getAndSet(true) ?: return
        if (wasInitialized) return
        
        try {
            val time = measureTimeMillis { init() }
            Timber.d("Lazy init '$name' completed in ${time}ms")
        } catch (e: Exception) {
            Timber.e(e, "Lazy init '$name' failed")
            initialized[name]?.set(false)
        }
    }
    
    /**
     * Check if a component is initialized.
     */
    fun isInitialized(name: String): Boolean {
        return initialized[name]?.get() ?: false
    }
    
    /**
     * Clear all initializers (for testing).
     */
    fun clear() {
        initializers.clear()
        initialized.clear()
    }
}
