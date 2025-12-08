package com.momoterminal

import android.app.Application
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorkerFactory
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.momoterminal.core.ai.AiConfig
import com.momoterminal.monitoring.AnalyticsHelper
import com.momoterminal.monitoring.CrashlyticsHelper
import com.momoterminal.monitoring.PerformanceHelper
import com.momoterminal.offline.OfflineFirstManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Main Application class for MomoTerminal.
 * Initializes global application state and provides shared resources.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class MomoTerminalApp : MultiDexApplication(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var crashlyticsHelper: CrashlyticsHelper

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var performanceHelper: PerformanceHelper

    @Inject
    lateinit var offlineFirstManager: OfflineFirstManager

    lateinit var sharedPreferences: SharedPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        initializeCrashRecovery()
        initializeFirebase()
        initializeTimber()
        initializeAiConfig()
        initializeOfflineSync()
    }

    /**
     * Initialize crash recovery manager for automatic app restart after crashes.
     */
    private fun initializeCrashRecovery() {
        com.momoterminal.startup.CrashRecoveryManager.initialize(
            application = this,
            autoRestart = !BuildConfig.DEBUG // Only auto-restart in release builds
        )
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

    /**
     * Initialize AI configuration for SMS parsing.
     */
    private fun initializeAiConfig() {
        AiConfig.initialize(
            openAiKey = BuildConfig.OPENAI_API_KEY,
            geminiKey = BuildConfig.GEMINI_API_KEY,
            aiParsingEnabled = BuildConfig.AI_PARSING_ENABLED
        )
        
        Timber.d("AI Config initialized - OpenAI: ${AiConfig.isOpenAiEnabled}, Gemini: ${AiConfig.isGeminiEnabled}")
    }

    /**
     * Initialize offline-first sync strategy.
     */
    private fun initializeOfflineSync() {
        offlineFirstManager.schedulePeriodicSync()
        Timber.d("Offline-first sync initialized")
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
