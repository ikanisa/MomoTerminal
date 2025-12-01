package com.momoterminal.domain.model

/**
 * Enum representing supported mobile money providers.
 * Targeting: Rwanda, DR Congo, Tanzania, Burundi, Zambia
 */
enum class Provider(val displayName: String, val ussdPrefix: String) {
    /**
     * MTN Mobile Money (Rwanda, Uganda, Zambia, etc.)
     */
    MTN("MTN MoMo", "*182*8*1*"),
    
    /**
     * Airtel Money (Tanzania, Zambia, DRC, etc.)
     */
    AIRTEL("Airtel Money", "*150*60*1*"),
    
    /**
     * Tigo Pesa (Tanzania)
     */
    TIGO("Tigo Pesa", "*150*01*"),
    
    /**
     * Vodacom M-Pesa (Tanzania, DRC)
     */
    VODACOM("Vodacom M-Pesa", "*150*00*"),
    
    /**
     * Halotel (Tanzania)
     */
    HALOTEL("Halotel", "*150*88*"),
    
    /**
     * Lumicash (DRC)
     */
    LUMICASH("Lumicash", "*150*3*"),
    
    /**
     * EcoCash (Zambia, Zimbabwe)
     */
    ECOCASH("EcoCash", "*151*"),
    
    /**
     * Vodafone Cash (Ghana - secondary market)
     */
    VODAFONE("Vodafone Cash", "*110*1*"),
    
    /**
     * AirtelTigo Money (Ghana - secondary market)
     */
    AIRTELTIGO("AirtelTigo Money", "*500*1*");
    
    /**
     * Generate USSD code for this provider.
     */
    fun generateUssdCode(merchantCode: String, amount: Double): String {
        val formattedAmount = "%.2f".format(amount)
        return "$ussdPrefix$merchantCode*$formattedAmount#"
    }
    
    companion object {
        /**
         * Detect provider from sender ID or message content.
         */
        fun fromSender(sender: String): Provider? {
            return when {
                sender.contains("MTN", ignoreCase = true) ||
                sender.contains("MoMo", ignoreCase = true) -> MTN
                
                sender.contains("Airtel", ignoreCase = true) -> AIRTEL
                
                sender.contains("Tigo", ignoreCase = true) -> TIGO
                
                sender.contains("Vodacom", ignoreCase = true) ||
                sender.contains("M-PESA", ignoreCase = true) ||
                sender.contains("MPESA", ignoreCase = true) -> VODACOM
                
                sender.contains("Halotel", ignoreCase = true) -> HALOTEL
                
                sender.contains("Lumicash", ignoreCase = true) -> LUMICASH
                
                sender.contains("EcoCash", ignoreCase = true) -> ECOCASH
                
                sender.contains("Vodafone", ignoreCase = true) ||
                sender.contains("VodaCash", ignoreCase = true) -> VODAFONE
                
                sender.contains("AirtelTigo", ignoreCase = true) ||
                sender.contains("ATMoney", ignoreCase = true) -> AIRTELTIGO
                
                else -> null
            }
        }
    }
}
