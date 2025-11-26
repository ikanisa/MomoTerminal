package com.momoterminal

import android.app.Application
import android.content.SharedPreferences

/**
 * Main Application class for MomoTerminal.
 * Initializes global application state and provides shared resources.
 */
class MomoTerminalApp : Application() {

    lateinit var sharedPreferences: SharedPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

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
