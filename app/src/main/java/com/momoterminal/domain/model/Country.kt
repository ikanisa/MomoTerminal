package com.momoterminal.domain.model

/**
 * Domain model for a country with its mobile money provider.
 */
data class Country(
    val code: String,                 // ISO 3166-1 alpha-2
    val name: String,
    val nameLocal: String,
    val currency: String,             // ISO 4217
    val currencySymbol: String,
    val phonePrefix: String,
    val language: String,
    val providerName: String,         // Single authorized provider code
    val providerDisplayName: String,  // Human readable provider name
    val providerColor: String,        // Hex color for UI
    val isPrimaryMarket: Boolean = false
) {
    companion object {
        /**
         * Default country for fallback.
         */
        val DEFAULT = Country(
            code = "RW",
            name = "Rwanda",
            nameLocal = "Rwanda",
            currency = "RWF",
            currencySymbol = "FRw",
            phonePrefix = "+250",
            language = "rw",
            providerName = "MTN",
            providerDisplayName = "MTN MoMo",
            providerColor = "#FFCC00",
            isPrimaryMarket = true
        )
    }
}
