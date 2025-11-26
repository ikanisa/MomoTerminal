package com.momoterminal.domain.model

/**
 * Enum representing supported mobile money providers.
 */
enum class Provider(val displayName: String, val ussdPrefix: String) {
    /**
     * MTN Mobile Money Ghana
     */
    MTN("MTN MoMo", "*170*1*1*"),
    
    /**
     * Vodafone Cash Ghana
     */
    VODAFONE("Vodafone Cash", "*110*1*"),
    
    /**
     * AirtelTigo Money Ghana
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
                
                sender.contains("Vodafone", ignoreCase = true) ||
                sender.contains("VodaCash", ignoreCase = true) -> VODAFONE
                
                sender.contains("AirtelTigo", ignoreCase = true) ||
                sender.contains("ATMoney", ignoreCase = true) -> AIRTELTIGO
                
                else -> null
            }
        }
    }
}
