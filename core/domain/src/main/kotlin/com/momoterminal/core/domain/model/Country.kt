package com.momoterminal.core.domain.model

/**
 * Domain model for country data.
 */
data class Country(
    val code: String,
    val name: String,
    val nameLocal: String,
    val currency: String,
    val currencySymbol: String,
    val phonePrefix: String,
    val phoneLength: Int,
    val primaryLanguage: String,
    val providerCode: String,
    val providerName: String,
    val providerColor: String,
    val ussdPayMerchant: String?,
    val ussdSendToPhone: String?,
    val hasUssdSupport: Boolean,
    val hasAppSupport: Boolean,
    val isPrimaryMarket: Boolean,
    val launchPriority: Int
) {
    val displayName: String get() = nameLocal.ifBlank { name }

    fun generateMerchantUssd(merchantCode: String, amount: String): String? {
        if (!hasUssdSupport || ussdPayMerchant.isNullOrBlank()) return null
        return ussdPayMerchant
            .replace("{merchant}", merchantCode)
            .replace("{amount}", amount)
    }

    fun generateSendUssd(phone: String, amount: String): String? {
        if (!hasUssdSupport || ussdSendToPhone.isNullOrBlank()) return null
        return ussdSendToPhone
            .replace("{phone}", phone)
            .replace("{amount}", amount)
    }
}
