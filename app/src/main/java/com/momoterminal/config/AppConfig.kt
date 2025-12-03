package com.momoterminal.config

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration manager for MomoTerminal app.
 * 
 * Supports two country contexts:
 * - Profile Country: The country of the WhatsApp number used for registration
 * - Mobile Money Country: The country for mobile money operations (can be different)
 */
class AppConfig(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Save merchant configuration with separate profile and mobile money countries.
     */
    fun saveMerchantConfig(
        merchantCode: String,
        mobileMoneyNumber: String,
        momoCountryCode: String
    ) {
        val currency = SupportedCountries.getByCode(momoCountryCode)?.currency ?: "RWF"
        prefs.edit().apply {
            putString(KEY_MERCHANT_CODE, merchantCode)
            putString(KEY_MOBILE_MONEY_NUMBER, mobileMoneyNumber)
            putString(KEY_MOMO_COUNTRY_CODE, momoCountryCode)
            putString(KEY_MOMO_CURRENCY, currency)
            putBoolean(KEY_IS_CONFIGURED, mobileMoneyNumber.isNotBlank())
            apply()
        }
    }

    /**
     * Set profile country from authenticated WhatsApp phone number.
     * This is the user's registration country and does not affect mobile money operations.
     */
    fun setProfileCountryFromPhone(phoneNumber: String) {
        val countryCode = CountryDetector.detectCountry(phoneNumber)
        prefs.edit().apply {
            putString(KEY_PROFILE_COUNTRY_CODE, countryCode)
            putString(KEY_AUTH_PHONE, phoneNumber)
            // Also set momo country as default if not already set
            if (getMomoCountryCode().isBlank()) {
                val currency = CountryDetector.getCurrency(countryCode)
                putString(KEY_MOMO_COUNTRY_CODE, countryCode)
                putString(KEY_MOMO_CURRENCY, currency)
            }
            apply()
        }
    }

    /**
     * Save mobile money country (can be different from profile country).
     */
    fun saveMomoCountryCode(countryCode: String) {
        val currency = SupportedCountries.getByCode(countryCode)?.currency ?: "RWF"
        prefs.edit().apply {
            putString(KEY_MOMO_COUNTRY_CODE, countryCode)
            putString(KEY_MOMO_CURRENCY, currency)
            apply()
        }
    }

    /**
     * Save NFC writer enabled state.
     */
    fun setNfcWriterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NFC_WRITER_ENABLED, enabled).apply()
    }

    /**
     * Check if NFC writer is enabled.
     */
    fun isNfcWriterEnabled(): Boolean = prefs.getBoolean(KEY_NFC_WRITER_ENABLED, true)

    // Profile country (from WhatsApp registration)
    fun getProfileCountryCode(): String = prefs.getString(KEY_PROFILE_COUNTRY_CODE, "RW") ?: "RW"
    
    // Mobile money country (for transactions - can be different from profile)
    fun getMomoCountryCode(): String = prefs.getString(KEY_MOMO_COUNTRY_CODE, "") ?: ""
    fun getMomoCurrency(): String = prefs.getString(KEY_MOMO_CURRENCY, "RWF") ?: "RWF"
    
    // Get provider for the mobile money country
    fun getMomoProvider(): String {
        val countryCode = getMomoCountryCode().ifBlank { getProfileCountryCode() }
        return SupportedCountries.getPrimaryProviderForCountry(countryCode)
    }

    fun getAuthPhone(): String = prefs.getString(KEY_AUTH_PHONE, "") ?: ""
    fun getMerchantCode(): String = prefs.getString(KEY_MERCHANT_CODE, "") ?: ""
    fun getMobileMoneyNumber(): String = prefs.getString(KEY_MOBILE_MONEY_NUMBER, "") ?: ""
    
    // Legacy: getCountryCode returns momo country for backward compatibility
    fun getCountryCode(): String = getMomoCountryCode().ifBlank { getProfileCountryCode() }
    fun getCurrency(): String = getMomoCurrency()
    
    fun isConfigured(): Boolean = prefs.getBoolean(KEY_IS_CONFIGURED, false)
    fun isMomoConfigured(): Boolean = getMobileMoneyNumber().isNotBlank() && getMomoCountryCode().isNotBlank()

    // Legacy methods for compatibility
    fun getMerchantPhone(): String = getMobileMoneyNumber().ifBlank { getMerchantCode() }
    fun getGatewayUrl(): String = prefs.getString(KEY_GATEWAY_URL, "") ?: ""
    fun getApiSecret(): String = prefs.getString(KEY_API_SECRET, "") ?: ""

    // Legacy: saveCountryCode now saves momo country
    fun saveCountryCode(countryCode: String) {
        saveMomoCountryCode(countryCode)
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
        private const val KEY_PROFILE_COUNTRY_CODE = "profile_country_code"
        private const val KEY_MOMO_COUNTRY_CODE = "momo_country_code"
        private const val KEY_MOMO_CURRENCY = "momo_currency"
        private const val KEY_IS_CONFIGURED = "is_configured"
        private const val KEY_AUTH_PHONE = "auth_phone"
        private const val KEY_GATEWAY_URL = "gateway_url"
        private const val KEY_API_SECRET = "api_secret"
        private const val KEY_NFC_WRITER_ENABLED = "nfc_writer_enabled"
    }
}
