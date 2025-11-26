package com.momoterminal

import android.app.Application
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
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

    lateinit var sharedPreferences: SharedPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
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
