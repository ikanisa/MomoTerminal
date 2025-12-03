package com.momoterminal.config

import com.momoterminal.data.model.CountryConfig

/**
 * Supported countries configuration for MomoTerminal.
 * 
 * Target markets: Sub-Saharan French and English speaking countries
 * Excluded: Uganda (UG), Kenya (KE), Nigeria (NG), South Africa (ZA)
 * 
 * Note: Each country has ONE authorized mobile money provider.
 * The primary provider from the list is used for transactions.
 * Fallback country configurations.
 * Primary data source is Supabase countries table.
 * This provides offline fallback only.
 */
@Deprecated("Use CountryRepository for country data from Supabase")
object SupportedCountries {
    
    data class Country(
        val code: String,        // ISO 3166-1 alpha-2
        val name: String,
        val nameLocal: String,
        val currency: String,    // ISO 4217
        val currencySymbol: String,
        val phonePrefix: String,
        val language: String,    // Primary language
        val providers: List<String>  // Legacy: list of providers, first is primary
    ) {
        /**
         * Get the primary (authorized) provider for this country.
         */
        val primaryProvider: String get() = providers.firstOrNull() ?: "MTN"
        
        /**
         * Get display name for the primary provider.
         */
        val providerDisplayName: String get() = getProviderDisplayName(primaryProvider)
    }
    
    /**
     * Get display name for a provider code.
     */
    fun getProviderDisplayName(providerCode: String): String {
        return when (providerCode.uppercase()) {
            "MTN" -> "MTN MoMo"
            "AIRTEL" -> "Airtel Money"
            "VODACOM" -> "M-Pesa"
            "VODAFONE" -> "Vodafone Cash"
            "ORANGE" -> "Orange Money"
            "TIGO" -> "Tigo Pesa"
            "WAVE" -> "Wave"
            "MOOV" -> "Moov Money"
            "ECOCASH" -> "EcoCash"
            "TMONEY" -> "T-Money"
            "MVOLA" -> "MVola"
            "LUMICASH" -> "LumiCash"
            "TOGOCEL" -> "Flooz"
            "FREE" -> "Free Money"
            "TNM" -> "TNM Mpamba"
            "MASCOM" -> "MyZaka"
            "MTC" -> "MTC MoMo"
            "ECONET" -> "EcoCash"
            "AFRICELL" -> "Africell Money"
            "QCELL" -> "QMoney"
            "HALOTEL" -> "Halopesa"
            "ZAMTEL" -> "Zamtel Kwacha"
            "MOVITEL" -> "M-Pesa"
            "TELECEL" -> "Telecel Money"
            "ONEMONEY" -> "OneMoney"
            "AIRTELTIGO" -> "AirtelTigo Money"
            else -> providerCode
        }
    }
    
    // Primary launch countries
    val RWANDA = Country(
        code = "RW", name = "Rwanda", nameLocal = "Rwanda",
        currency = "RWF", currencySymbol = "FRw", phonePrefix = "+250",
        language = "rw", providers = listOf("MTN", "AIRTEL")
    )
    
    val DR_CONGO = Country(
        code = "CD", name = "DR Congo", nameLocal = "RD Congo",
        currency = "CDF", currencySymbol = "FC", phonePrefix = "+243",
        language = "fr", providers = listOf("ORANGE", "VODACOM", "AIRTEL")
    )
    
    val BURUNDI = Country(
        code = "BI", name = "Burundi", nameLocal = "Burundi",
        currency = "BIF", currencySymbol = "FBu", phonePrefix = "+257",
        language = "fr", providers = listOf("ECOCASH", "LUMICASH")
    )
    
    val TANZANIA = Country(
        code = "TZ", name = "Tanzania", nameLocal = "Tanzania",
        currency = "TZS", currencySymbol = "TSh", phonePrefix = "+255",
        language = "sw", providers = listOf("VODACOM", "AIRTEL", "TIGO", "HALOTEL")
    )
    
    val ZAMBIA = Country(
        code = "ZM", name = "Zambia", nameLocal = "Zambia",
        currency = "ZMW", currencySymbol = "ZK", phonePrefix = "+260",
        language = "en", providers = listOf("MTN", "AIRTEL", "ZAMTEL")
    )
    
    // Expansion countries - French speaking
    val SENEGAL = Country(
        code = "SN", name = "Senegal", nameLocal = "Sénégal",
        currency = "XOF", currencySymbol = "CFA", phonePrefix = "+221",
        language = "fr", providers = listOf("ORANGE", "FREE", "WAVE")
    )
    
    val IVORY_COAST = Country(
        code = "CI", name = "Ivory Coast", nameLocal = "Côte d'Ivoire",
        currency = "XOF", currencySymbol = "CFA", phonePrefix = "+225",
        language = "fr", providers = listOf("ORANGE", "MTN", "MOOV", "WAVE")
    )
    
    val CAMEROON = Country(
        code = "CM", name = "Cameroon", nameLocal = "Cameroun",
        currency = "XAF", currencySymbol = "FCFA", phonePrefix = "+237",
        language = "fr", providers = listOf("MTN", "ORANGE")
    )
    
    val MALI = Country(
        code = "ML", name = "Mali", nameLocal = "Mali",
        currency = "XOF", currencySymbol = "CFA", phonePrefix = "+223",
        language = "fr", providers = listOf("ORANGE", "MOOV")
    )
    
    val BURKINA_FASO = Country(
        code = "BF", name = "Burkina Faso", nameLocal = "Burkina Faso",
        currency = "XOF", currencySymbol = "CFA", phonePrefix = "+226",
        language = "fr", providers = listOf("ORANGE", "MOOV")
    )
    
    val NIGER = Country(
        code = "NE", name = "Niger", nameLocal = "Niger",
        currency = "XOF", currencySymbol = "CFA", phonePrefix = "+227",
        language = "fr", providers = listOf("AIRTEL", "MOOV", "ORANGE")
    )
    
    val BENIN = Country(
        code = "BJ", name = "Benin", nameLocal = "Bénin",
        currency = "XOF", currencySymbol = "CFA", phonePrefix = "+229",
        language = "fr", providers = listOf("MTN", "MOOV")
    )
    
    val TOGO = Country(
        code = "TG", name = "Togo", nameLocal = "Togo",
        currency = "XOF", currencySymbol = "CFA", phonePrefix = "+228",
        language = "fr", providers = listOf("TMONEY", "MOOV")
    )
    
    val GUINEA = Country(
        code = "GN", name = "Guinea", nameLocal = "Guinée",
        currency = "GNF", currencySymbol = "FG", phonePrefix = "+224",
        language = "fr", providers = listOf("ORANGE", "MTN")
    )
    
    val CHAD = Country(
        code = "TD", name = "Chad", nameLocal = "Tchad",
        currency = "XAF", currencySymbol = "FCFA", phonePrefix = "+235",
        language = "fr", providers = listOf("AIRTEL", "TIGO")
    )
    
    val CENTRAL_AFRICAN_REPUBLIC = Country(
        code = "CF", name = "Central African Republic", nameLocal = "République Centrafricaine",
        currency = "XAF", currencySymbol = "FCFA", phonePrefix = "+236",
        language = "fr", providers = listOf("ORANGE", "TELECEL")
    )
    
    val GABON = Country(
        code = "GA", name = "Gabon", nameLocal = "Gabon",
        currency = "XAF", currencySymbol = "FCFA", phonePrefix = "+241",
        language = "fr", providers = listOf("AIRTEL", "MOOV")
    )
    
    val CONGO_BRAZZAVILLE = Country(
        code = "CG", name = "Congo", nameLocal = "Congo-Brazzaville",
        currency = "XAF", currencySymbol = "FCFA", phonePrefix = "+242",
        language = "fr", providers = listOf("MTN", "AIRTEL")
    )
    
    // Expansion countries - English speaking
    val GHANA = Country(
        code = "GH", name = "Ghana", nameLocal = "Ghana",
        currency = "GHS", currencySymbol = "GH₵", phonePrefix = "+233",
        language = "en", providers = listOf("MTN", "VODAFONE", "AIRTELTIGO")
    )
    
    val MALAWI = Country(
        code = "MW", name = "Malawi", nameLocal = "Malawi",
        currency = "MWK", currencySymbol = "MK", phonePrefix = "+265",
        language = "en", providers = listOf("AIRTEL", "TNM")
    )
    
    val ZIMBABWE = Country(
        code = "ZW", name = "Zimbabwe", nameLocal = "Zimbabwe",
        currency = "ZWL", currencySymbol = "Z$", phonePrefix = "+263",
        language = "en", providers = listOf("ECOCASH", "ONEMONEY")
    )
    
    val MOZAMBIQUE = Country(
        code = "MZ", name = "Mozambique", nameLocal = "Moçambique",
        currency = "MZN", currencySymbol = "MT", phonePrefix = "+258",
        language = "pt", providers = listOf("VODACOM", "MOVITEL")
    )
    
    val BOTSWANA = Country(
        code = "BW", name = "Botswana", nameLocal = "Botswana",
        currency = "BWP", currencySymbol = "P", phonePrefix = "+267",
        language = "en", providers = listOf("ORANGE", "MASCOM")
    )
    
    val NAMIBIA = Country(
        code = "NA", name = "Namibia", nameLocal = "Namibia",
        currency = "NAD", currencySymbol = "N$", phonePrefix = "+264",
        language = "en", providers = listOf("MTC")
    )
    
    val LESOTHO = Country(
        code = "LS", name = "Lesotho", nameLocal = "Lesotho",
        currency = "LSL", currencySymbol = "L", phonePrefix = "+266",
        language = "en", providers = listOf("VODACOM", "ECONET")
    )
    
    val ESWATINI = Country(
        code = "SZ", name = "Eswatini", nameLocal = "Eswatini",
        currency = "SZL", currencySymbol = "E", phonePrefix = "+268",
        language = "en", providers = listOf("MTN")
    )
    
    val LIBERIA = Country(
        code = "LR", name = "Liberia", nameLocal = "Liberia",
        currency = "LRD", currencySymbol = "L$", phonePrefix = "+231",
        language = "en", providers = listOf("ORANGE", "MTN")
    )
    
    val SIERRA_LEONE = Country(
        code = "SL", name = "Sierra Leone", nameLocal = "Sierra Leone",
        currency = "SLL", currencySymbol = "Le", phonePrefix = "+232",
        language = "en", providers = listOf("ORANGE", "AFRICELL")
    )
    
    val GAMBIA = Country(
        code = "GM", name = "Gambia", nameLocal = "Gambia",
        currency = "GMD", currencySymbol = "D", phonePrefix = "+220",
        language = "en", providers = listOf("AFRICELL", "QCELL")
    )
    
    // Excluded countries (for reference - DO NOT ADD TO SUPPORTED LIST)
    private val EXCLUDED_COUNTRIES = setOf("UG", "KE", "NG", "ZA")
    
    // All supported countries
    val ALL_SUPPORTED: List<Country> = listOf(
        // Primary launch
        RWANDA, DR_CONGO, BURUNDI, TANZANIA, ZAMBIA,
        // French speaking expansion
        SENEGAL, IVORY_COAST, CAMEROON, MALI, BURKINA_FASO,
        NIGER, BENIN, TOGO, GUINEA, CHAD, CENTRAL_AFRICAN_REPUBLIC,
        GABON, CONGO_BRAZZAVILLE,
        // English speaking expansion
        GHANA, MALAWI, ZIMBABWE, MOZAMBIQUE, BOTSWANA,
        NAMIBIA, LESOTHO, ESWATINI, LIBERIA, SIERRA_LEONE, GAMBIA
    )
    
    // Primary launch countries
    val PRIMARY_LAUNCH: List<Country> = listOf(RWANDA, DR_CONGO, BURUNDI, TANZANIA, ZAMBIA)
    
    // Get country by code
    fun getByCode(code: String): Country? = ALL_SUPPORTED.find { it.code == code.uppercase() }
    
    // Check if country is supported
    fun isSupported(code: String): Boolean {
        val upperCode = code.uppercase()
        return upperCode !in EXCLUDED_COUNTRIES && ALL_SUPPORTED.any { it.code == upperCode }
    }
    
    // Get countries by language
    fun getByLanguage(language: String): List<Country> = 
        ALL_SUPPORTED.filter { it.language == language.lowercase() }
    
    // Get default country (Rwanda)
    fun getDefault(): Country = RWANDA

    // Get currency for country code
    fun getCurrencyForCountry(code: String): String = getByCode(code)?.currency ?: "GHS"
    
    // Get primary provider for country code
    fun getPrimaryProviderForCountry(code: String): String = getByCode(code)?.primaryProvider ?: "MTN"
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
