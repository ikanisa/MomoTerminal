package com.momoterminal.config

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration manager for MomoTerminal app.
 */
class AppConfig(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Save merchant configuration.
     */
    fun saveMerchantConfig(
        merchantCode: String,
        mobileMoneyNumber: String,
        countryCode: String
    ) {
        val currency = SupportedCountries.getByCode(countryCode)?.currency ?: "RWF"
        prefs.edit().apply {
            putString(KEY_MERCHANT_CODE, merchantCode)
            putString(KEY_MOBILE_MONEY_NUMBER, mobileMoneyNumber)
            putString(KEY_COUNTRY_CODE, countryCode)
            putString(KEY_CURRENCY, currency)
            putBoolean(KEY_IS_CONFIGURED, merchantCode.isNotBlank())
            apply()
        }
    }

    /**
     * Set country from authenticated phone number.
     */
    fun setCountryFromPhone(phoneNumber: String) {
        val countryCode = CountryDetector.detectCountry(phoneNumber)
        val currency = CountryDetector.getCurrency(countryCode)
        prefs.edit().apply {
            putString(KEY_COUNTRY_CODE, countryCode)
            putString(KEY_CURRENCY, currency)
            putString(KEY_AUTH_PHONE, phoneNumber)
            apply()
        }
    }

    fun getAuthPhone(): String = prefs.getString(KEY_AUTH_PHONE, "") ?: ""
    fun getMerchantCode(): String = prefs.getString(KEY_MERCHANT_CODE, "") ?: ""
    fun getMobileMoneyNumber(): String = prefs.getString(KEY_MOBILE_MONEY_NUMBER, "") ?: ""
    fun getCountryCode(): String = prefs.getString(KEY_COUNTRY_CODE, "RW") ?: "RW"
    fun getCurrency(): String = prefs.getString(KEY_CURRENCY, "RWF") ?: "RWF"
    fun isConfigured(): Boolean = prefs.getBoolean(KEY_IS_CONFIGURED, false)

    // Legacy methods for compatibility
    fun getMerchantPhone(): String = getMobileMoneyNumber().ifBlank { getMerchantCode() }
    fun getGatewayUrl(): String = prefs.getString(KEY_GATEWAY_URL, "") ?: ""
    fun getApiSecret(): String = prefs.getString(KEY_API_SECRET, "") ?: ""

    fun saveCountryCode(countryCode: String) {
        val currency = SupportedCountries.getByCode(countryCode)?.currency ?: "RWF"
        prefs.edit().apply {
            putString(KEY_COUNTRY_CODE, countryCode)
            putString(KEY_CURRENCY, currency)
            apply()
        }
    }

    fun clearConfig() {
        prefs.edit().clear().apply()
    }

    // Legacy saveConfig for backward compatibility
    fun saveConfig(url: String, secret: String, phone: String) {
        prefs.edit().apply {
            putString(KEY_GATEWAY_URL, url)
            putString(KEY_API_SECRET, secret)
            putString(KEY_MOBILE_MONEY_NUMBER, phone)
            putBoolean(KEY_IS_CONFIGURED, true)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "momo_terminal_config"
        private const val KEY_MERCHANT_CODE = "merchant_code"
        private const val KEY_MOBILE_MONEY_NUMBER = "mobile_money_number"
        private const val KEY_COUNTRY_CODE = "country_code"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_IS_CONFIGURED = "is_configured"
        private const val KEY_AUTH_PHONE = "auth_phone"
        private const val KEY_GATEWAY_URL = "gateway_url"
        private const val KEY_API_SECRET = "api_secret"
    }
}
