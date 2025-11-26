package com.momoterminal.config

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration manager for MomoTerminal app.
 * Uses SharedPreferences to store and retrieve app settings.
 * Allows each deployed phone to have its own webhook URL and merchant number.
 */
class AppConfig(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    /**
     * Save the gateway configuration.
     * @param url The project's webhook URL
     * @param secret Security key for headers (X-Api-Key)
     * @param phone Merchant phone number for the NFC payload
     */
    fun saveConfig(url: String, secret: String, phone: String) {
        prefs.edit().apply {
            putString(KEY_GATEWAY_URL, url)
            putString(KEY_API_SECRET, secret)
            putString(KEY_MERCHANT_PHONE, phone)
            putBoolean(KEY_IS_CONFIGURED, true)
            apply()
        }
    }
    
    /**
     * Get the configured gateway URL.
     */
    fun getGatewayUrl(): String {
        return prefs.getString(KEY_GATEWAY_URL, "") ?: ""
    }
    
    /**
     * Get the API secret key for authentication.
     */
    fun getApiSecret(): String {
        return prefs.getString(KEY_API_SECRET, "") ?: ""
    }
    
    /**
     * Get the merchant phone number.
     */
    fun getMerchantPhone(): String {
        return prefs.getString(KEY_MERCHANT_PHONE, "") ?: ""
    }
    
    /**
     * Check if the app has been configured.
     */
    fun isConfigured(): Boolean {
        return prefs.getBoolean(KEY_IS_CONFIGURED, false)
    }
    
    /**
     * Clear all configuration data.
     */
    fun clearConfig() {
        prefs.edit().clear().apply()
    }
    
    companion object {
        private const val PREFS_NAME = "momo_gateway_config"
        private const val KEY_GATEWAY_URL = "gateway_url"
        private const val KEY_API_SECRET = "api_secret"
        private const val KEY_MERCHANT_PHONE = "merchant_phone"
        private const val KEY_IS_CONFIGURED = "is_configured"
    }
}
