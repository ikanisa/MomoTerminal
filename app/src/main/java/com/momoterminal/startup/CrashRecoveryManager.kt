package com.momoterminal.startup

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Process
import androidx.core.content.edit
import com.momoterminal.presentation.ComposeMainActivity
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Crash recovery manager for handling app crashes and automatic restart.
 *
 * Features:
 * - Catches uncaught exceptions
 * - Logs crash details to Crashlytics
 * - Saves crash state for recovery
 * - Automatically restarts the app after crash (optional)
 * - Tracks consecutive crashes to prevent crash loops
 *
 * Usage:
 * ```kotlin
 * // Initialize in Application.onCreate()
 * CrashRecoveryManager.initialize(this)
 *
 * // Check for crash recovery on app start
 * if (CrashRecoveryManager.wasAppRestartedFromCrash(this)) {
 *     // Handle recovery
 * }
 * ```
 */
object CrashRecoveryManager {

    private const val PREFS_NAME = "crash_recovery"
    private const val KEY_CRASH_COUNT = "crash_count"
    private const val KEY_LAST_CRASH_TIME = "last_crash_time"
    private const val KEY_CRASH_STACK_TRACE = "crash_stack_trace"
    private const val KEY_WAS_CRASHED = "was_crashed"
    private const val KEY_CRASH_ACTIVITY = "crash_activity"
    
    /** Intent extra key indicating the app was restarted from a crash. */
    const val EXTRA_FROM_CRASH = "from_crash"

    // Maximum consecutive crashes before disabling auto-restart
    private const val MAX_CONSECUTIVE_CRASHES = 3
    
    // Time window for counting consecutive crashes (5 minutes)
    private const val CRASH_WINDOW_MS = 5 * 60 * 1000L

    private var isInitialized = false
    private var autoRestartEnabled = true
    private var currentActivity: Activity? = null

    /**
     * Initialize the crash recovery manager.
     * Should be called early in Application.onCreate().
     *
     * @param application The application instance
     * @param autoRestart Whether to automatically restart after crash (default: true)
     */
    fun initialize(application: Application, autoRestart: Boolean = true) {
        if (isInitialized) {
            Timber.w("CrashRecoveryManager already initialized")
            return
        }

        autoRestartEnabled = autoRestart
        isInitialized = true

        // Register activity lifecycle callbacks to track current activity
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
            }
            override fun onActivityPaused(activity: Activity) {
                if (currentActivity == activity) {
                    currentActivity = null
                }
            }
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        // Set up uncaught exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(application, thread, throwable, defaultHandler)
        }

        Timber.d("CrashRecoveryManager initialized")
    }

    /**
     * Handle an uncaught exception.
     */
    private fun handleCrash(
        context: Context,
        thread: Thread,
        throwable: Throwable,
        defaultHandler: Thread.UncaughtExceptionHandler?
    ) {
        Timber.e(throwable, "Uncaught exception on thread ${thread.name}")

        try {
            // Save crash information
            saveCrashInfo(context, throwable)

            // Log to Crashlytics
            logToCrashlytics(throwable)

            // Check if we should auto-restart
            if (autoRestartEnabled && !isInCrashLoop(context)) {
                Timber.i("Scheduling app restart...")
                scheduleRestart(context)
            } else {
                Timber.w("Auto-restart disabled or in crash loop")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in crash handler")
        }

        // Call the default handler
        defaultHandler?.uncaughtException(thread, throwable)
    }

    /**
     * Save crash information for recovery.
     */
    private fun saveCrashInfo(context: Context, throwable: Throwable) {
        val prefs = getPrefs(context)
        val currentTime = System.currentTimeMillis()
        val stackTrace = getStackTraceString(throwable)

        prefs.edit {
            putBoolean(KEY_WAS_CRASHED, true)
            putLong(KEY_LAST_CRASH_TIME, currentTime)
            putString(KEY_CRASH_STACK_TRACE, stackTrace)
            putString(KEY_CRASH_ACTIVITY, currentActivity?.javaClass?.name)
            
            // Increment crash count
            val lastCrashTime = prefs.getLong(KEY_LAST_CRASH_TIME, 0)
            val crashCount = if (currentTime - lastCrashTime < CRASH_WINDOW_MS) {
                prefs.getInt(KEY_CRASH_COUNT, 0) + 1
            } else {
                1
            }
            putInt(KEY_CRASH_COUNT, crashCount)
        }
    }

    /**
     * Log crash to Firebase Crashlytics.
     */
    private fun logToCrashlytics(throwable: Throwable) {
        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().apply {
                log("Crash caught by CrashRecoveryManager")
                currentActivity?.let { 
                    setCustomKey("crash_activity", it.javaClass.simpleName) 
                }
                recordException(throwable)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to log to Crashlytics")
        }
    }

    /**
     * Check if we're in a crash loop.
     */
    private fun isInCrashLoop(context: Context): Boolean {
        val prefs = getPrefs(context)
        val crashCount = prefs.getInt(KEY_CRASH_COUNT, 0)
        val lastCrashTime = prefs.getLong(KEY_LAST_CRASH_TIME, 0)
        val timeSinceCrash = System.currentTimeMillis() - lastCrashTime

        return crashCount >= MAX_CONSECUTIVE_CRASHES && timeSinceCrash < CRASH_WINDOW_MS
    }

    /**
     * Schedule app restart.
     */
    private fun scheduleRestart(context: Context) {
        try {
            val intent = Intent(context, ComposeMainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_FROM_CRASH, true)
            }
            context.startActivity(intent)
            
            // Kill the current process
            Process.killProcess(Process.myPid())
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule restart")
        }
    }

    /**
     * Check if the app was restarted from a crash.
     */
    fun wasAppRestartedFromCrash(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WAS_CRASHED, false)
    }

    /**
     * Get the last crash stack trace.
     */
    fun getLastCrashStackTrace(context: Context): String? {
        return getPrefs(context).getString(KEY_CRASH_STACK_TRACE, null)
    }

    /**
     * Get the activity that was active during the last crash.
     */
    fun getLastCrashActivity(context: Context): String? {
        return getPrefs(context).getString(KEY_CRASH_ACTIVITY, null)
    }

    /**
     * Get the number of consecutive crashes.
     */
    fun getConsecutiveCrashCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_CRASH_COUNT, 0)
    }

    /**
     * Clear crash recovery state.
     * Call this after successfully starting the app to reset crash tracking.
     */
    fun clearCrashState(context: Context) {
        getPrefs(context).edit {
            putBoolean(KEY_WAS_CRASHED, false)
            putInt(KEY_CRASH_COUNT, 0)
            remove(KEY_CRASH_STACK_TRACE)
            remove(KEY_CRASH_ACTIVITY)
        }
        Timber.d("Crash state cleared")
    }

    /**
     * Reset the crash counter.
     * Call this after a successful app session to reset consecutive crash counting.
     */
    fun resetCrashCounter(context: Context) {
        getPrefs(context).edit {
            putInt(KEY_CRASH_COUNT, 0)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString().take(4000) // Limit to 4000 chars
    }
}
