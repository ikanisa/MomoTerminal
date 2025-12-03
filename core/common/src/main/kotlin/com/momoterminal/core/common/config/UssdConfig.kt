package com.momoterminal.core.common.config

/**
 * USSD configuration for Mobile Money services across Africa.
 * Contains USSD codes for sending money and paying merchants.
 */
object UssdConfig {

    data class UssdTemplate(
        val countryCode: String,
        val countryName: String,
        val provider: String,
        val currency: String,
        val sendToPhone: String,      // Template: {phone}, {amount}
        val payMerchant: String,      // Template: {merchant}, {amount}
        val baseCode: String          // Main USSD menu code
    )

    /**
     * All supported USSD configurations by country code.
     */
    val configurations: Map<String, UssdTemplate> = mapOf(
        // Primary Launch Countries
        "RW" to UssdTemplate(
            countryCode = "RW", countryName = "Rwanda", provider = "MTN",
            currency = "RWF", baseCode = "*182#",
            sendToPhone = "*182*1*1*{phone}*{amount}#",
            payMerchant = "*182*8*1*{merchant}*{amount}#"
        ),
        "CD" to UssdTemplate(
            countryCode = "CD", countryName = "DR Congo", provider = "Orange",
            currency = "CDF", baseCode = "*144#",
            sendToPhone = "*144*1*{phone}*{amount}#",
            payMerchant = "*144*4*{merchant}*{amount}#"
        ),
        "BI" to UssdTemplate(
            countryCode = "BI", countryName = "Burundi", provider = "EcoCash",
            currency = "BIF", baseCode = "*151#",
            sendToPhone = "*151*1*1*{phone}*{amount}#",
            payMerchant = "*151*1*2*{merchant}*{amount}#"
        ),
        "TZ" to UssdTemplate(
            countryCode = "TZ", countryName = "Tanzania", provider = "Vodacom M-Pesa",
            currency = "TZS", baseCode = "*150*00#",
            sendToPhone = "*150*00*{phone}*{amount}#",
            payMerchant = "*150*00*{merchant}*{amount}#"
        ),
        "ZM" to UssdTemplate(
            countryCode = "ZM", countryName = "Zambia", provider = "MTN",
            currency = "ZMW", baseCode = "*115#",
            sendToPhone = "*115*2*{phone}*{amount}#",
            payMerchant = "*115*5*{merchant}*{amount}#"
        ),

        // West Africa - French Speaking
        "SN" to UssdTemplate(
            countryCode = "SN", countryName = "Senegal", provider = "Orange",
            currency = "XOF", baseCode = "#144#",
            sendToPhone = "#144*1*{phone}*{amount}#",
            payMerchant = "#144*2*{merchant}*{amount}#"
        ),
        "CI" to UssdTemplate(
            countryCode = "CI", countryName = "Côte d'Ivoire", provider = "Orange",
            currency = "XOF", baseCode = "*144#",
            sendToPhone = "*144*1*{phone}*{amount}#",
            payMerchant = "*144*4*{merchant}*{amount}#"
        ),
        "ML" to UssdTemplate(
            countryCode = "ML", countryName = "Mali", provider = "Orange",
            currency = "XOF", baseCode = "#144#",
            sendToPhone = "#144#*1*{phone}*{amount}#",
            payMerchant = "#144#*2*{merchant}*{amount}#"
        ),
        "BF" to UssdTemplate(
            countryCode = "BF", countryName = "Burkina Faso", provider = "Orange",
            currency = "XOF", baseCode = "*144#",
            sendToPhone = "*144*1*{phone}*{amount}#",
            payMerchant = "*144*4*{merchant}*{amount}#"
        ),
        "NE" to UssdTemplate(
            countryCode = "NE", countryName = "Niger", provider = "Airtel",
            currency = "XOF", baseCode = "*400#",
            sendToPhone = "*400*{phone}*{amount}#",
            payMerchant = "*400*{merchant}*{amount}#"
        ),
        "BJ" to UssdTemplate(
            countryCode = "BJ", countryName = "Benin", provider = "MTN",
            currency = "XOF", baseCode = "*880#",
            sendToPhone = "*880*1*{phone}*{amount}#",
            payMerchant = "*880*3*{merchant}*{amount}#"
        ),
        "TG" to UssdTemplate(
            countryCode = "TG", countryName = "Togo", provider = "T-Money",
            currency = "XOF", baseCode = "*145#",
            sendToPhone = "*145*1*{amount}*{phone}#", // Note: amount before phone
            payMerchant = "*145*3*{merchant}*{amount}#"
        ),
        "GN" to UssdTemplate(
            countryCode = "GN", countryName = "Guinea", provider = "Orange",
            currency = "GNF", baseCode = "*144#",
            sendToPhone = "*144*1*{phone}*{amount}#",
            payMerchant = "*144*4*{merchant}*{amount}#"
        ),
        "MR" to UssdTemplate(
            countryCode = "MR", countryName = "Mauritania", provider = "Moov",
            currency = "MRU", baseCode = "*900#",
            sendToPhone = "*900*2*{phone}*{amount}#",
            payMerchant = "*900*4*{merchant}*{amount}#"
        ),

        // Central Africa - French Speaking
        "CM" to UssdTemplate(
            countryCode = "CM", countryName = "Cameroon", provider = "MTN",
            currency = "XAF", baseCode = "*126#",
            sendToPhone = "*126*2*{phone}*{amount}#",
            payMerchant = "*126*4*{merchant}*{amount}#"
        ),
        "GA" to UssdTemplate(
            countryCode = "GA", countryName = "Gabon", provider = "Airtel",
            currency = "XAF", baseCode = "*150#",
            sendToPhone = "*150*2*{phone}*{amount}#",
            payMerchant = "*150*4*{merchant}*{amount}#"
        ),
        "CG" to UssdTemplate(
            countryCode = "CG", countryName = "Congo", provider = "MTN",
            currency = "XAF", baseCode = "*133#",
            sendToPhone = "*133*2*{phone}*{amount}#",
            payMerchant = "*133*5*{merchant}*{amount}#"
        ),
        "CF" to UssdTemplate(
            countryCode = "CF", countryName = "Central African Rep.", provider = "Orange",
            currency = "XAF", baseCode = "#150#",
            sendToPhone = "#150*2*{phone}*{amount}#",
            payMerchant = "#150*4*{merchant}*{amount}#"
        ),
        "TD" to UssdTemplate(
            countryCode = "TD", countryName = "Chad", provider = "Airtel",
            currency = "XAF", baseCode = "*211#",
            sendToPhone = "*211*{phone}*{amount}#",
            payMerchant = "*211*{merchant}*{amount}#"
        ),
        "GQ" to UssdTemplate(
            countryCode = "GQ", countryName = "Equatorial Guinea", provider = "GETESA",
            currency = "XAF", baseCode = "*222#",
            sendToPhone = "*222*2*{phone}*{amount}#",
            payMerchant = "*222*4*{merchant}*{amount}#"
        ),

        // East Africa - English Speaking
        "MW" to UssdTemplate(
            countryCode = "MW", countryName = "Malawi", provider = "Airtel",
            currency = "MWK", baseCode = "*211#",
            sendToPhone = "*211*{phone}*{amount}#",
            payMerchant = "*211*{merchant}*{amount}#"
        ),
        "ZW" to UssdTemplate(
            countryCode = "ZW", countryName = "Zimbabwe", provider = "EcoCash",
            currency = "ZWL", baseCode = "*151#",
            sendToPhone = "*151*1*1*{phone}*{amount}#",
            payMerchant = "*151*2*{merchant}*{amount}#"
        ),
        "NA" to UssdTemplate(
            countryCode = "NA", countryName = "Namibia", provider = "MTC",
            currency = "NAD", baseCode = "*140*682#",
            sendToPhone = "*140*682*{phone}*{amount}#",
            payMerchant = "*140*682*{merchant}*{amount}#"
        ),

        // Ghana (secondary market)
        "GH" to UssdTemplate(
            countryCode = "GH", countryName = "Ghana", provider = "MTN",
            currency = "GHS", baseCode = "*170#",
            sendToPhone = "*170*1*1*{phone}*{amount}#",
            payMerchant = "*170*2*1*{merchant}*{amount}#"
        ),

        // Island Nations
        "MG" to UssdTemplate(
            countryCode = "MG", countryName = "Madagascar", provider = "Telma MVola",
            currency = "MGA", baseCode = "#111#",
            sendToPhone = "#111*2*{phone}*{amount}#",
            payMerchant = "#111*4*{merchant}*{amount}#"
        ),
        "KM" to UssdTemplate(
            countryCode = "KM", countryName = "Comoros", provider = "MVola",
            currency = "KMF", baseCode = "*150*01#",
            sendToPhone = "*150*01*1*1*{phone}*{amount}#",
            payMerchant = "*150*01*1*2*{merchant}*{amount}#"
        ),
        "SC" to UssdTemplate(
            countryCode = "SC", countryName = "Seychelles", provider = "Airtel",
            currency = "SCR", baseCode = "*202#",
            sendToPhone = "*202*{phone}*{amount}#",
            payMerchant = "*202*{merchant}*{amount}#"
        ),
        "DJ" to UssdTemplate(
            countryCode = "DJ", countryName = "Djibouti", provider = "D-Money",
            currency = "DJF", baseCode = "*131#",
            sendToPhone = "*131*{phone}*{amount}#",
            payMerchant = "*133*{merchant}*{amount}#"
        )
    )

    /**
     * Get USSD config for a country.
     */
    fun getConfig(countryCode: String): UssdTemplate? = configurations[countryCode.uppercase()]

    /**
     * Generate USSD code to pay a merchant.
     */
    fun generateMerchantPaymentUssd(
        countryCode: String,
        merchantCode: String,
        amount: String
    ): String? {
        val config = getConfig(countryCode) ?: return null
        return config.payMerchant
            .replace("{merchant}", merchantCode)
            .replace("{amount}", amount)
    }

    /**
     * Generate USSD code to send money to a phone.
     */
    fun generateSendMoneyUssd(
        countryCode: String,
        phone: String,
        amount: String
    ): String? {
        val config = getConfig(countryCode) ?: return null
        return config.sendToPhone
            .replace("{phone}", phone)
            .replace("{amount}", amount)
    }

    /**
     * Generate tel: URI for dialing.
     */
    fun generateDialUri(ussdCode: String): String {
        return "tel:${android.net.Uri.encode(ussdCode)}"
    }
}
