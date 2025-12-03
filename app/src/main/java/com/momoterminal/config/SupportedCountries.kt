package com.momoterminal.config

import com.momoterminal.data.model.CountryConfig

/**
 * Fallback country configurations.
 * Primary data source is Supabase countries table.
 * This provides offline fallback only.
 */
@Deprecated("Use CountryRepository for country data from Supabase")
object SupportedCountries {
    
    // Fallback countries for offline use
    private val FALLBACK_COUNTRIES = listOf(
        CountryConfig(
            id = "rw", code = "RW", name = "Rwanda", nameLocal = "Rwanda",
            currency = "RWF", currencySymbol = "FRw", phonePrefix = "+250", phoneLength = 9,
            flagEmoji = "🇷🇼", providerName = "MTN MoMo", providerCode = "MTN",
            providerColor = "#FFCC00", ussdTemplate = "*182*8*1*{merchant}*{amount}#"
        ),
        CountryConfig(
            id = "cd", code = "CD", name = "DR Congo", nameLocal = "RD Congo",
            currency = "CDF", currencySymbol = "FC", phonePrefix = "+243", phoneLength = 9,
            flagEmoji = "🇨🇩", providerName = "Vodacom M-Pesa", providerCode = "VODACOM",
            providerColor = "#E60000", ussdTemplate = "*150*1*1*{merchant}*{amount}#"
        ),
        CountryConfig(
            id = "bi", code = "BI", name = "Burundi", nameLocal = "Burundi",
            currency = "BIF", currencySymbol = "FBu", phonePrefix = "+257", phoneLength = 8,
            flagEmoji = "🇧🇮", providerName = "Lumicash", providerCode = "LUMICASH",
            providerColor = "#00A651", ussdTemplate = "*150*1*{merchant}*{amount}#"
        ),
        CountryConfig(
            id = "tz", code = "TZ", name = "Tanzania", nameLocal = "Tanzania",
            currency = "TZS", currencySymbol = "TSh", phonePrefix = "+255", phoneLength = 9,
            flagEmoji = "🇹🇿", providerName = "Vodacom M-Pesa", providerCode = "VODACOM",
            providerColor = "#E60000", ussdTemplate = "*150*00#{merchant}*{amount}#"
        ),
        CountryConfig(
            id = "zm", code = "ZM", name = "Zambia", nameLocal = "Zambia",
            currency = "ZMW", currencySymbol = "ZK", phonePrefix = "+260", phoneLength = 9,
            flagEmoji = "🇿🇲", providerName = "MTN MoMo", providerCode = "MTN",
            providerColor = "#FFCC00", ussdTemplate = "*303*{merchant}*{amount}#"
        )
    )
    
    val PRIMARY_LAUNCH: List<CountryConfig> = FALLBACK_COUNTRIES
    
    fun getByCode(code: String): CountryConfig? = 
        FALLBACK_COUNTRIES.find { it.code.equals(code, ignoreCase = true) }
    
    fun getDefault(): CountryConfig = FALLBACK_COUNTRIES.first()
    
    fun getCurrencyForCountry(code: String): String = 
        getByCode(code)?.currency ?: "RWF"
}
