package com.momoterminal.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Country configuration from Supabase.
 * Each country has exactly one mobile money provider.
 */
@Serializable
data class CountryConfig(
    @SerialName("id") val id: String,
    @SerialName("code") val code: String,           // ISO 3166-1 alpha-2 (e.g., "RW")
    @SerialName("name") val name: String,           // English name
    @SerialName("name_local") val nameLocal: String, // Local name
    @SerialName("currency") val currency: String,    // ISO 4217 (e.g., "RWF")
    @SerialName("currency_symbol") val currencySymbol: String,
    @SerialName("phone_prefix") val phonePrefix: String, // e.g., "+250"
    @SerialName("phone_length") val phoneLength: Int = 9, // digits after prefix
    @SerialName("flag_emoji") val flagEmoji: String = "",
    @SerialName("provider_name") val providerName: String, // e.g., "MTN MoMo"
    @SerialName("provider_code") val providerCode: String, // e.g., "MTN"
    @SerialName("provider_color") val providerColor: String = "#FFCC00",
    @SerialName("ussd_template") val ussdTemplate: String, // e.g., "*182*8*1*{merchant}*{amount}#"
    @SerialName("is_active") val isActive: Boolean = true
) {
    fun formatPhoneNumber(number: String): String {
        val cleaned = number.replace(Regex("[^0-9]"), "")
        return "$phonePrefix $cleaned"
    }

    fun isValidPhoneLength(number: String): Boolean {
        val cleaned = number.replace(Regex("[^0-9]"), "")
        return cleaned.length == phoneLength
    }

    fun generateUssd(merchantCode: String, amount: String): String {
        return ussdTemplate
            .replace("{merchant}", merchantCode)
            .replace("{amount}", amount)
    }

    companion object {
        val DEFAULT = CountryConfig(
            id = "default",
            code = "RW",
            name = "Rwanda",
            nameLocal = "Rwanda",
            currency = "RWF",
            currencySymbol = "FRw",
            phonePrefix = "+250",
            phoneLength = 9,
            flagEmoji = "🇷🇼",
            providerName = "MTN MoMo",
            providerCode = "MTN",
            providerColor = "#FFCC00",
            ussdTemplate = "*182*8*1*{merchant}*{amount}#",
            isActive = true
        )
    }
}

/**
 * User's mobile money configuration.
 * WhatsApp country may differ from MoMo country.
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
