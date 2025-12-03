package com.momoterminal.core.common.model

/**
 * Complete country configuration from Supabase.
 * Each country has exactly ONE accepted mobile money provider.
 */
data class CountryConfig(
    val id: String,
    val code: String,
    val codeAlpha3: String? = null,
    val name: String,
    val nameLocal: String? = null,
    val nameFrench: String? = null,
    val currency: String,
    val currencySymbol: String,
    val currencyName: String? = null,
    val currencyDecimals: Int = 2,
    val phonePrefix: String,
    val phoneLength: Int = 9,
    val phoneFormat: String? = null,
    val flagEmoji: String = "",
    val primaryLanguage: String = "en",
    val supportedLanguages: List<String> = listOf("en"),
    val timezone: String? = null,
    val providerName: String,
    val providerCode: String,
    val providerColor: String = "#FFCC00",
    val providerLogoUrl: String? = null,
    val ussdBaseCode: String? = null,
    val ussdSendToPhone: String? = null,
    val ussdPayMerchant: String? = null,
    val ussdCheckBalance: String? = null,
    val ussdNotes: String? = null,
    val hasUssdSupport: Boolean = true,
    val hasAppSupport: Boolean = false,
    val hasQrSupport: Boolean = false,
    val requiresPinPrompt: Boolean = true,
    val isActive: Boolean = true,
    val isPrimaryMarket: Boolean = false,
    val launchPriority: Int = 99
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
data class CountryListItem(
    val code: String,
    val name: String,
    val flagEmoji: String,
    val providerName: String,
    val currency: String,
    val phonePrefix: String,
    val hasUssdSupport: Boolean
) {
    fun toDisplayString(): String = "$flagEmoji $name"
    fun toDetailedDisplayString(): String = "$flagEmoji $name ($providerName)"
}

/**
 * User's mobile money configuration.
 */
data class UserMomoConfig(
    val userId: String,
    val whatsappCountryCode: String,
    val momoCountryCode: String,
    val momoPhoneNumber: String,
    val merchantName: String = "",
    val isVerified: Boolean = false
)
