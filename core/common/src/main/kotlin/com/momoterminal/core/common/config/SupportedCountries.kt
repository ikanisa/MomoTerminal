package com.momoterminal.core.common.config

import com.momoterminal.core.common.model.CountryConfig

/**
 * Fallback country configurations for offline use.
 * Primary data source is Supabase countries table.
 */
object SupportedCountries {

    private val FALLBACK_COUNTRIES = listOf(
        // Primary Markets (launchPriority 1-8)
        CountryConfig(
            id = "rw", code = "RW", name = "Rwanda", nameLocal = "Rwanda", nameFrench = "Rwanda",
            currency = "RWF", currencySymbol = "FRw", currencyDecimals = 0,
            phonePrefix = "+250", phoneLength = 9, flagEmoji = "🇷🇼",
            primaryLanguage = "rw", providerName = "MTN MoMo", providerCode = "MTN",
            providerColor = "#FFCC00", ussdBaseCode = "*182#",
            ussdSendToPhone = "*182*1*1*{phone}*{amount}#",
            ussdPayMerchant = "*182*8*1*{merchant}*{amount}#",
            hasUssdSupport = true, isPrimaryMarket = true, launchPriority = 1
        ),
        CountryConfig(
            id = "bi", code = "BI", name = "Burundi", nameLocal = "Burundi", nameFrench = "Burundi",
            currency = "BIF", currencySymbol = "FBu", currencyDecimals = 0,
            phonePrefix = "+257", phoneLength = 8, flagEmoji = "🇧🇮",
            primaryLanguage = "fr", providerName = "Econet EcoCash", providerCode = "ECOCASH",
            providerColor = "#00A651", ussdBaseCode = "*151#",
            ussdSendToPhone = "*151*1*1*{phone}*{amount}#",
            ussdPayMerchant = "*151*1*2*{phone}*{amount}#",
            ussdNotes = "EcoCash distinguishes registered vs. unregistered users",
            hasUssdSupport = true, isPrimaryMarket = true, launchPriority = 2
        ),
        CountryConfig(
            id = "cm", code = "CM", name = "Cameroon", nameLocal = "Cameroun", nameFrench = "Cameroun",
            currency = "XAF", currencySymbol = "FCFA", currencyDecimals = 0,
            phonePrefix = "+237", phoneLength = 9, flagEmoji = "🇨🇲",
            primaryLanguage = "fr", providerName = "MTN Mobile Money", providerCode = "MTN",
            providerColor = "#FFCC00", ussdBaseCode = "*126#",
            ussdSendToPhone = "*126*2*{phone}*{amount}#",
            ussdPayMerchant = "*126*4*{merchant}*{amount}#",
            ussdNotes = "Option 2 for Transfer Money, Option 4 for bills/merchants",
            hasUssdSupport = true, isPrimaryMarket = true, launchPriority = 3
        ),
        CountryConfig(
            id = "mg", code = "MG", name = "Madagascar", nameLocal = "Madagasikara", nameFrench = "Madagascar",
            currency = "MGA", currencySymbol = "Ar", currencyDecimals = 0,
            phonePrefix = "+261", phoneLength = 9, flagEmoji = "🇲🇬",
            primaryLanguage = "fr", providerName = "Telma MVola", providerCode = "MVOLA",
            providerColor = "#E31E24", ussdBaseCode = "#111#",
            ussdSendToPhone = "#111*2*{phone}*{amount}#",
            ussdPayMerchant = "#111*4*{merchant}*{amount}#",
            ussdNotes = "MVola uses # prefix. Option 2 for send, Option 4 for pay",
            hasUssdSupport = true, hasAppSupport = true, isPrimaryMarket = true, launchPriority = 4
        ),
        CountryConfig(
            id = "mu", code = "MU", name = "Mauritius", nameLocal = "Mauritius", nameFrench = "Maurice",
            currency = "MUR", currencySymbol = "₨", currencyDecimals = 2,
            phonePrefix = "+230", phoneLength = 8, flagEmoji = "🇲🇺",
            primaryLanguage = "en", providerName = "my.t money", providerCode = "MYT",
            providerColor = "#E4002B",
            ussdNotes = "App-based only. No USSD. QR code payments supported.",
            hasUssdSupport = false, hasAppSupport = true, hasQrSupport = true,
            isPrimaryMarket = true, launchPriority = 5
        ),
        CountryConfig(
            id = "tz", code = "TZ", name = "Tanzania", nameLocal = "Tanzania", nameFrench = "Tanzanie",
            currency = "TZS", currencySymbol = "TSh", currencyDecimals = 0,
            phonePrefix = "+255", phoneLength = 9, flagEmoji = "🇹🇿",
            primaryLanguage = "sw", providerName = "Vodacom M-Pesa", providerCode = "VODACOM",
            providerColor = "#E60000", ussdBaseCode = "*150*00#",
            ussdSendToPhone = "*150*00*{phone}*{amount}#",
            ussdPayMerchant = "*150*00*{merchant}*{amount}#",
            ussdNotes = "M-Pesa prompts for PIN after dialing",
            hasUssdSupport = true, hasAppSupport = true, isPrimaryMarket = true, launchPriority = 6
        ),
        CountryConfig(
            id = "zm", code = "ZM", name = "Zambia", nameLocal = "Zambia", nameFrench = "Zambie",
            currency = "ZMW", currencySymbol = "ZK", currencyDecimals = 2,
            phonePrefix = "+260", phoneLength = 9, flagEmoji = "🇿🇲",
            primaryLanguage = "en", providerName = "MTN MoMo", providerCode = "MTN",
            providerColor = "#FFCC00", ussdBaseCode = "*115#",
            ussdSendToPhone = "*115*2*{phone}*{amount}#",
            ussdPayMerchant = "*115*5*{merchant}*{amount}#",
            ussdNotes = "Option 2 for Send Money, Option 5 for Merchant Pay",
            hasUssdSupport = true, isPrimaryMarket = true, launchPriority = 7
        ),
        CountryConfig(
            id = "cd", code = "CD", name = "DR Congo", nameLocal = "RD Congo", nameFrench = "République Démocratique du Congo",
            currency = "CDF", currencySymbol = "FC", currencyDecimals = 2,
            phonePrefix = "+243", phoneLength = 9, flagEmoji = "🇨🇩",
            primaryLanguage = "fr", providerName = "Orange Money", providerCode = "ORANGE",
            providerColor = "#FF6600", ussdBaseCode = "*144#",
            ussdSendToPhone = "*144*1*{phone}*{amount}#",
            ussdPayMerchant = "*144*4*{merchant}*{amount}#",
            ussdNotes = "Option 4 for bill payments",
            hasUssdSupport = true, hasAppSupport = true, isPrimaryMarket = true, launchPriority = 8
        ),
        // Secondary Markets (launchPriority 10+)
        CountryConfig(
            id = "zw", code = "ZW", name = "Zimbabwe", nameLocal = "Zimbabwe", nameFrench = "Zimbabwe",
            currency = "ZWL", currencySymbol = "Z$", currencyDecimals = 2,
            phonePrefix = "+263", phoneLength = 9, flagEmoji = "🇿🇼",
            primaryLanguage = "en", providerName = "Econet EcoCash", providerCode = "ECOCASH",
            providerColor = "#00A651", ussdBaseCode = "*151#",
            ussdSendToPhone = "*151*1*1*{phone}*{amount}#",
            ussdPayMerchant = "*151*2*{merchant}*{amount}#",
            ussdNotes = "PIN-protected after USSD dial",
            hasUssdSupport = true, launchPriority = 10
        ),
        CountryConfig(
            id = "mw", code = "MW", name = "Malawi", nameLocal = "Malawi", nameFrench = "Malawi",
            currency = "MWK", currencySymbol = "MK", currencyDecimals = 2,
            phonePrefix = "+265", phoneLength = 9, flagEmoji = "🇲🇼",
            primaryLanguage = "en", providerName = "Airtel Money", providerCode = "AIRTEL",
            providerColor = "#ED1C24", ussdBaseCode = "*211#",
            ussdSendToPhone = "*211*{phone}*{amount}#",
            ussdPayMerchant = "*211*{merchant}*{amount}#",
            hasUssdSupport = true, launchPriority = 11
        ),
        CountryConfig(
            id = "na", code = "NA", name = "Namibia", nameLocal = "Namibia", nameFrench = "Namibie",
            currency = "NAD", currencySymbol = "N$", currencyDecimals = 2,
            phonePrefix = "+264", phoneLength = 9, flagEmoji = "🇳🇦",
            primaryLanguage = "en", providerName = "MTC Money (Maris)", providerCode = "MTC",
            providerColor = "#0066B3", ussdBaseCode = "*140*682#",
            ussdSendToPhone = "*140*682*{phone}*{amount}#",
            ussdPayMerchant = "*140*682*{merchant}*{amount}#",
            hasUssdSupport = true, launchPriority = 12
        ),
        CountryConfig(
            id = "sc", code = "SC", name = "Seychelles", nameLocal = "Seychelles", nameFrench = "Seychelles",
            currency = "SCR", currencySymbol = "₨", currencyDecimals = 2,
            phonePrefix = "+248", phoneLength = 7, flagEmoji = "🇸🇨",
            primaryLanguage = "en", providerName = "Airtel Money", providerCode = "AIRTEL",
            providerColor = "#ED1C24", ussdBaseCode = "*202#",
            ussdSendToPhone = "*202*{phone}*{amount}#",
            ussdPayMerchant = "*202*{merchant}*{amount}#",
            hasUssdSupport = true, launchPriority = 13
        ),
        CountryConfig(
            id = "gh", code = "GH", name = "Ghana", nameLocal = "Ghana", nameFrench = "Ghana",
            currency = "GHS", currencySymbol = "GH₵", currencyDecimals = 2,
            phonePrefix = "+233", phoneLength = 9, flagEmoji = "🇬🇭",
            primaryLanguage = "en", providerName = "MTN Mobile Money", providerCode = "MTN",
            providerColor = "#FFCC00", ussdBaseCode = "*170#",
            ussdSendToPhone = "*170*1*1*{phone}*{amount}#",
            ussdPayMerchant = "*170*2*1*{merchant}*{amount}#",
            ussdNotes = "Option 1-1 for MoMo user, Option 2-1 for MoMoPay merchant",
            hasUssdSupport = true, hasAppSupport = true, launchPriority = 14
        ),
        CountryConfig(
            id = "bj", code = "BJ", name = "Benin", nameLocal = "Bénin", nameFrench = "Bénin",
            currency = "XOF", currencySymbol = "CFA", currencyDecimals = 0,
            phonePrefix = "+229", phoneLength = 8, flagEmoji = "🇧🇯",
            primaryLanguage = "fr", providerName = "MTN Mobile Money", providerCode = "MTN",
            providerColor = "#FFCC00", ussdBaseCode = "*880#",
            ussdSendToPhone = "*880*1*{phone}*{amount}#",
            ussdPayMerchant = "*880*3*{merchant}*{amount}#",
            hasUssdSupport = true, launchPriority = 15
        ),
        CountryConfig(
            id = "bf", code = "BF", name = "Burkina Faso", nameLocal = "Burkina Faso", nameFrench = "Burkina Faso",
            currency = "XOF", currencySymbol = "CFA", currencyDecimals = 0,
            phonePrefix = "+226", phoneLength = 8, flagEmoji = "🇧🇫",
            primaryLanguage = "fr", providerName = "Orange Money", providerCode = "ORANGE",
            providerColor = "#FF6600", ussdBaseCode = "*144#",
            ussdSendToPhone = "*144*1*{phone}*{amount}#",
            ussdPayMerchant = "*144*4*{merchant}*{amount}#",
            hasUssdSupport = true, launchPriority = 16
        ),
        CountryConfig(
            id = "cf", code = "CF", name = "Central African Republic",
            nameLocal = "Ködörösêse tî Bêafrîka", nameFrench = "République Centrafricaine",
            currency = "XAF", currencySymbol = "FCFA", currencyDecimals = 0,
            phonePrefix = "+236", phoneLength = 8, flagEmoji = "🇨🇫",
            primaryLanguage = "fr", providerName = "Orange Money", providerCode = "ORANGE",
            providerColor = "#FF6600", ussdBaseCode = "#150#",
            ussdSendToPhone = "#150*2*{phone}*{amount}#",
            ussdPayMerchant = "#150*4*{merchant}*{amount}#",
            ussdNotes = "Uses # prefix",
            hasUssdSupport = true, launchPriority = 17
        ),
        CountryConfig(
            id = "td", code = "TD", name = "Chad", nameLocal = "Tchad", nameFrench = "Tchad",
            currency = "XAF", currencySymbol = "FCFA", currencyDecimals = 0,
            phonePrefix = "+235", phoneLength = 8, flagEmoji = "🇹🇩",
            primaryLanguage = "fr", providerName = "Airtel Money", providerCode = "AIRTEL",
            providerColor = "#ED1C24", ussdBaseCode = "*211#",
            ussdSendToPhone = "*211*{phone}*{amount}#",
            ussdPayMerchant = "*211*{merchant}*{amount}#",
            hasUssdSupport = true, launchPriority = 18
        ),
        CountryConfig(
            id = "km", code = "KM", name = "Comoros", nameLocal = "Komori", nameFrench = "Comores",
            currency = "KMF", currencySymbol = "CF", currencyDecimals = 0,
            phonePrefix = "+269", phoneLength = 7, flagEmoji = "🇰🇲",
            primaryLanguage = "fr", providerName = "Telma/YAZ MVola", providerCode = "MVOLA",
            providerColor = "#E31E24", ussdBaseCode = "*150*01#",
            ussdSendToPhone = "*150*01*1*1*{phone}*{amount}#",
            ussdPayMerchant = "*150*01*1*2*{merchant}*{amount}#",
            hasUssdSupport = true, launchPriority = 19
        ),
        CountryConfig(
            id = "cg", code = "CG", name = "Congo (Republic)",
            nameLocal = "Congo-Brazzaville", nameFrench = "République du Congo",
            currency = "XAF", currencySymbol = "FCFA", currencyDecimals = 0,
            phonePrefix = "+242", phoneLength = 9, flagEmoji = "🇨🇬",
            primaryLanguage = "fr", providerName = "MTN MoMo", providerCode = "MTN",
            providerColor = "#FFCC00", ussdBaseCode = "*133#",
            ussdSendToPhone = "*133*2*{phone}*{amount}#",
            ussdPayMerchant = "*133*5*{merchant}*{amount}#",
            hasUssdSupport = true, launchPriority = 20
        ),
        CountryConfig(
            id = "ci", code = "CI", name = "Côte d'Ivoire",
            nameLocal = "Côte d'Ivoire", nameFrench = "Côte d'Ivoire",
            currency = "XOF", currencySymbol = "CFA", currencyDecimals = 0,
            phonePrefix = "+225", phoneLength = 10, flagEmoji = "🇨🇮",
            primaryLanguage = "fr", providerName = "Orange Money", providerCode = "ORANGE",
            providerColor = "#FF6600", ussdBaseCode = "*144#",
            ussdSendToPhone = "*144*1*{phone}*{amount}#",
            ussdPayMerchant = "*144*4*{merchant}*{amount}#",
            hasUssdSupport = true, hasAppSupport = true, launchPriority = 21
        ),
        CountryConfig(
            id = "dj", code = "DJ", name = "Djibouti", nameLocal = "Jabuuti", nameFrench = "Djibouti",
            currency = "DJF", currencySymbol = "Fdj", currencyDecimals = 0,
            phonePrefix = "+253", phoneLength = 8, flagEmoji = "🇩🇯",
            primaryLanguage = "fr", providerName = "Djibouti Telecom D-Money", providerCode = "DMONEY",
            providerColor = "#00A651", ussdBaseCode = "*131#",
            ussdSendToPhone = "*131*{phone}*{amount}#",
            ussdPayMerchant = "*133*{merchant}*{amount}#",
            ussdNotes = "*130# balance, *131# send, *133# bills",
            hasUssdSupport = true, launchPriority = 22
        ),
        CountryConfig(
            id = "gq", code = "GQ", name = "Equatorial Guinea",
            nameLocal = "Guinea Ecuatorial", nameFrench = "Guinée Équatoriale",
            currency = "XAF", currencySymbol = "FCFA", currencyDecimals = 0,
            phonePrefix = "+240", phoneLength = 9, flagEmoji = "🇬🇶",
            primaryLanguage = "es", providerName = "GETESA Mobile Money", providerCode = "GETESA",
            providerColor = "#009639", ussdBaseCode = "*222#",
            ussdSendToPhone = "*222*2*{phone}*{amount}#",
            ussdPayMerchant = "*222*4*{merchant}*{amount}#",
            hasUssdSupport = true, launchPriority = 23
        ),
        CountryConfig(
            id = "ga", code = "GA", name = "Gabon", nameLocal = "Gabon", nameFrench = "Gabon",
            currency = "XAF", currencySymbol = "FCFA", currencyDecimals = 0,
            phonePrefix = "+241", phoneLength = 9, flagEmoji = "🇬🇦",
            primaryLanguage = "fr", providerName = "Airtel Money", providerCode = "AIRTEL",
            providerColor = "#ED1C24", ussdBaseCode = "*150#",
            ussdSendToPhone = "*150*2*{phone}*{amount}#",
            ussdPayMerchant = "*150*4*{merchant}*{amount}#",
            hasUssdSupport = true, launchPriority = 24
        ),
        CountryConfig(
            id = "gn", code = "GN", name = "Guinea", nameLocal = "Guinée", nameFrench = "Guinée",
            currency = "GNF", currencySymbol = "FG", currencyDecimals = 0,
            phonePrefix = "+224", phoneLength = 9, flagEmoji = "🇬🇳",
            primaryLanguage = "fr", providerName = "Orange Money", providerCode = "ORANGE",
            providerColor = "#FF6600", ussdBaseCode = "*144#",
            ussdSendToPhone = "*144*1*{phone}*{amount}#",
            ussdPayMerchant = "*144*4*{merchant}*{amount}#",
            hasUssdSupport = true, launchPriority = 25
        ),
        CountryConfig(
            id = "ml", code = "ML", name = "Mali", nameLocal = "Mali", nameFrench = "Mali",
            currency = "XOF", currencySymbol = "CFA", currencyDecimals = 0,
            phonePrefix = "+223", phoneLength = 8, flagEmoji = "🇲🇱",
            primaryLanguage = "fr", providerName = "Orange Money", providerCode = "ORANGE",
            providerColor = "#FF6600", ussdBaseCode = "#144#",
            ussdSendToPhone = "#144#*1*{phone}*{amount}#",
            ussdPayMerchant = "#144#*2*{merchant}*{amount}#",
            ussdNotes = "Uses #144# code (with # prefix)",
            hasUssdSupport = true, launchPriority = 26
        ),
        CountryConfig(
            id = "mr", code = "MR", name = "Mauritania", nameLocal = "Mauritanie", nameFrench = "Mauritanie",
            currency = "MRU", currencySymbol = "UM", currencyDecimals = 2,
            phonePrefix = "+222", phoneLength = 8, flagEmoji = "🇲🇷",
            primaryLanguage = "ar", providerName = "Moov Mauritel Money", providerCode = "MOOV",
            providerColor = "#6F2C91", ussdBaseCode = "*900#",
            ussdSendToPhone = "*900*2*{phone}*{amount}#",
            ussdPayMerchant = "*900*4*{merchant}*{amount}#",
            hasUssdSupport = true, launchPriority = 27
        ),
        CountryConfig(
            id = "ne", code = "NE", name = "Niger", nameLocal = "Niger", nameFrench = "Niger",
            currency = "XOF", currencySymbol = "CFA", currencyDecimals = 0,
            phonePrefix = "+227", phoneLength = 8, flagEmoji = "🇳🇪",
            primaryLanguage = "fr", providerName = "Airtel Money", providerCode = "AIRTEL",
            providerColor = "#ED1C24", ussdBaseCode = "*400#",
            ussdSendToPhone = "*400*{phone}*{amount}#",
            ussdPayMerchant = "*400*{merchant}*{amount}#",
            hasUssdSupport = true, launchPriority = 28
        ),
        CountryConfig(
            id = "sn", code = "SN", name = "Senegal", nameLocal = "Sénégal", nameFrench = "Sénégal",
            currency = "XOF", currencySymbol = "CFA", currencyDecimals = 0,
            phonePrefix = "+221", phoneLength = 9, flagEmoji = "🇸🇳",
            primaryLanguage = "fr", providerName = "Orange Money", providerCode = "ORANGE",
            providerColor = "#FF6600", ussdBaseCode = "#144#",
            ussdSendToPhone = "#144*1*{phone}*{amount}#",
            ussdPayMerchant = "#144*2*{merchant}*{amount}#",
            ussdNotes = "Uses #144# code. Option 1 send, Option 2 pay",
            hasUssdSupport = true, hasAppSupport = true, launchPriority = 29
        ),
        CountryConfig(
            id = "tg", code = "TG", name = "Togo", nameLocal = "Togo", nameFrench = "Togo",
            currency = "XOF", currencySymbol = "CFA", currencyDecimals = 0,
            phonePrefix = "+228", phoneLength = 8, flagEmoji = "🇹🇬",
            primaryLanguage = "fr", providerName = "Togocom T-Money", providerCode = "TMONEY",
            providerColor = "#00A651", ussdBaseCode = "*145#",
            ussdSendToPhone = "*145*1*{amount}*{phone}#", // NOTE: Amount BEFORE phone
            ussdPayMerchant = "*145*3*{merchant}*{amount}#",
            ussdNotes = "IMPORTANT: Amount entered BEFORE phone number for send",
            hasUssdSupport = true, launchPriority = 30
        )
    )

    val PRIMARY_LAUNCH: List<CountryConfig> = FALLBACK_COUNTRIES.filter { it.isPrimaryMarket }

    fun getByCode(code: String): CountryConfig? =
        FALLBACK_COUNTRIES.find { it.code.equals(code, ignoreCase = true) }

    fun getDefault(): CountryConfig = FALLBACK_COUNTRIES.first()

    fun getCurrencyForCountry(code: String): String =
        getByCode(code)?.currency ?: "RWF"

    fun getPrimaryProviderForCountry(code: String): String =
        getByCode(code)?.providerCode ?: "MTN"

    fun getProviderDisplayName(providerCode: String): String = when (providerCode.uppercase()) {
        "MTN" -> "MTN MoMo"
        "AIRTEL" -> "Airtel Money"
        "VODACOM" -> "M-Pesa"
        "ORANGE" -> "Orange Money"
        "ECOCASH" -> "EcoCash"
        "MVOLA" -> "MVola"
        "TMONEY" -> "T-Money"
        "MOOV" -> "Moov Money"
        "MYT" -> "my.t money"
        "MTC" -> "MTC Money"
        "DMONEY" -> "D-Money"
        "GETESA" -> "GETESA Mobile Money"
        else -> providerCode
    }

    fun getAllCountries(): List<CountryConfig> = FALLBACK_COUNTRIES
}
