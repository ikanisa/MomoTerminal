package com.momoterminal.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Complete country configuration from Supabase.
 * Each country has exactly ONE accepted mobile money provider.
 */
@Serializable
data class CountryConfig(
    @SerialName("id") val id: String,
    @SerialName("code") val code: String,
    @SerialName("code_alpha3") val codeAlpha3: String? = null,
    @SerialName("name") val name: String,
    @SerialName("name_local") val nameLocal: String? = null,
    @SerialName("name_french") val nameFrench: String? = null,
    @SerialName("currency") val currency: String,
    @SerialName("currency_symbol") val currencySymbol: String,
    @SerialName("currency_name") val currencyName: String? = null,
    @SerialName("currency_decimals") val currencyDecimals: Int = 2,
    @SerialName("phone_prefix") val phonePrefix: String,
    @SerialName("phone_length") val phoneLength: Int = 9,
    @SerialName("phone_format") val phoneFormat: String? = null,
    @SerialName("flag_emoji") val flagEmoji: String = "",
    @SerialName("primary_language") val primaryLanguage: String = "en",
    @SerialName("supported_languages") val supportedLanguages: List<String> = listOf("en"),
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("provider_name") val providerName: String,
    @SerialName("provider_code") val providerCode: String,
    @SerialName("provider_color") val providerColor: String = "#FFCC00",
    @SerialName("provider_logo_url") val providerLogoUrl: String? = null,
    @SerialName("ussd_base_code") val ussdBaseCode: String? = null,
    @SerialName("ussd_send_to_phone") val ussdSendToPhone: String? = null,
    @SerialName("ussd_pay_merchant") val ussdPayMerchant: String? = null,
    @SerialName("ussd_check_balance") val ussdCheckBalance: String? = null,
    @SerialName("ussd_notes") val ussdNotes: String? = null,
    @SerialName("has_ussd_support") val hasUssdSupport: Boolean = true,
    @SerialName("has_app_support") val hasAppSupport: Boolean = false,
    @SerialName("has_qr_support") val hasQrSupport: Boolean = false,
    @SerialName("requires_pin_prompt") val requiresPinPrompt: Boolean = true,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_primary_market") val isPrimaryMarket: Boolean = false,
    @SerialName("launch_priority") val launchPriority: Int = 99
) {
    fun formatPhoneNumber(number: String): String {
        val cleaned = number.replace(Regex("[^0-9]"), "")
        return "$phonePrefix $cleaned"
    }

    fun isValidPhoneLength(number: String): Boolean {
        val cleaned = number.replace(Regex("[^0-9]"), "")
        return cleaned.length == phoneLength
    }

    fun generateMerchantPaymentUssd(merchantCode: String, amount: String): String? {
        if (!hasUssdSupport || ussdPayMerchant.isNullOrBlank()) return null
        return ussdPayMerchant
            .replace("{merchant}", merchantCode)
            .replace("{amount}", amount)
    }

    fun generateSendMoneyUssd(phoneNumber: String, amount: String): String? {
        if (!hasUssdSupport || ussdSendToPhone.isNullOrBlank()) return null
        return ussdSendToPhone
            .replace("{phone}", phoneNumber)
            .replace("{amount}", amount)
    }

    fun getDisplayName(languageCode: String): String = when (languageCode.lowercase()) {
        "fr" -> nameFrench ?: name
        primaryLanguage -> nameLocal ?: name
        else -> name
    }

    fun formatAmount(amount: Double): String {
        val formatted = when (currencyDecimals) {
            0 -> "%.0f".format(amount)
            1 -> "%.1f".format(amount)
            else -> "%.2f".format(amount)
        }
        return "$currencySymbol $formatted"
    }

    val displayName: String get() = nameLocal ?: name

    companion object {
        val DEFAULT = CountryConfig(
            id = "default",
            code = "RW",
            name = "Rwanda",
            nameLocal = "Rwanda",
            nameFrench = "Rwanda",
            currency = "RWF",
            currencySymbol = "FRw",
            currencyDecimals = 0,
            phonePrefix = "+250",
            phoneLength = 9,
            flagEmoji = "🇷🇼",
            primaryLanguage = "rw",
            providerName = "MTN MoMo",
            providerCode = "MTN",
            providerColor = "#FFCC00",
            ussdBaseCode = "*182#",
            ussdSendToPhone = "*182*1*1*{phone}*{amount}#",
            ussdPayMerchant = "*182*8*1*{merchant}*{amount}#",
            hasUssdSupport = true,
            isPrimaryMarket = true,
            launchPriority = 1,
            isActive = true
        )
    }
}

/**
 * Simplified country info for selection lists.
 */
@Serializable
data class CountryListItem(
    @SerialName("code") val code: String,
    @SerialName("name") val name: String,
    @SerialName("flag_emoji") val flagEmoji: String,
    @SerialName("provider_name") val providerName: String,
    @SerialName("currency") val currency: String,
    @SerialName("phone_prefix") val phonePrefix: String,
    @SerialName("has_ussd_support") val hasUssdSupport: Boolean
) {
    fun toDisplayString(): String = "$flagEmoji $name"
    fun toDetailedDisplayString(): String = "$flagEmoji $name ($providerName)"
}

/**
 * User's mobile money configuration.
 */
@Serializable
data class UserMomoConfig(
    @SerialName("user_id") val userId: String,
    @SerialName("whatsapp_country_code") val whatsappCountryCode: String,
    @SerialName("momo_country_code") val momoCountryCode: String,
    @SerialName("momo_phone_number") val momoPhoneNumber: String,
    @SerialName("merchant_name") val merchantName: String = "",
    @SerialName("is_verified") val isVerified: Boolean = false
)
