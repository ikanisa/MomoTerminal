package com.momoterminal

import android.app.Application
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.momoterminal.monitoring.AnalyticsHelper
import com.momoterminal.monitoring.CrashlyticsHelper
import com.momoterminal.monitoring.PerformanceHelper
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Main Application class for MomoTerminal.
 * Initializes global application state and provides shared resources.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class MomoTerminalApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var crashlyticsHelper: CrashlyticsHelper

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var performanceHelper: PerformanceHelper

    lateinit var sharedPreferences: SharedPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        initializeFirebase()
        initializeTimber()
    }

    /**
     * Initialize Firebase services.
     */
    private fun initializeFirebase() {
        FirebaseApp.initializeApp(this)

        // Configure Crashlytics based on build type
        if (BuildConfig.DEBUG) {
            // Disable Crashlytics in debug builds
            crashlyticsHelper.setCrashlyticsCollectionEnabled(false)
            analyticsHelper.setAnalyticsCollectionEnabled(false)
            performanceHelper.setPerformanceCollectionEnabled(false)
        } else {
            // Enable Crashlytics in release builds
            crashlyticsHelper.setCrashlyticsCollectionEnabled(true)
            analyticsHelper.setAnalyticsCollectionEnabled(true)
            performanceHelper.setPerformanceCollectionEnabled(true)
        }
    }

    /**
     * Initialize Timber for logging.
     */
    private fun initializeTimber() {
        if (BuildConfig.DEBUG) {
            // Plant a debug tree for development
            Timber.plant(Timber.DebugTree())
        } else {
            // Plant a Crashlytics tree for production
            Timber.plant(CrashlyticsTree(crashlyticsHelper))
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        private const val PREFS_NAME = "momo_terminal_prefs"
        const val KEY_MERCHANT_CODE = "merchant_code"
        const val KEY_API_ENDPOINT = "api_endpoint"
        const val KEY_LAST_AMOUNT = "last_amount"
        const val KEY_NFC_ACTIVE = "nfc_active"

        @Volatile
        private var instance: MomoTerminalApp? = null

        fun getInstance(): MomoTerminalApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}

/**
 * Timber tree that logs to Firebase Crashlytics.
 * Only logs warnings and errors in release builds.
 */
class CrashlyticsTree(
    private val crashlyticsHelper: CrashlyticsHelper
) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Only log warnings and above
        if (priority < android.util.Log.WARN) {
            return
        }

        // Log the message as a breadcrumb
        crashlyticsHelper.logBreadcrumb("${tag ?: "MomoTerminal"}: $message")

        // Log the exception if present
        t?.let { throwable ->
            crashlyticsHelper.logException(throwable, message)
        }
    }
}
