package com.momoterminal.nfc

/**
 * Data class representing payment data for NFC transactions.
 */
data class NfcPaymentData(
    val merchantPhone: String,
    val amount: String,
    val currency: String = "GHS",
    val provider: Provider = Provider.MTN,
    val reference: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Supported Mobile Money providers.
     */
    enum class Provider(
        val displayName: String,
        val ussdPrefix: String,
        val colorHex: String
    ) {
        MTN("MTN MoMo", "*170#", "#FFCC00"),
        VODAFONE("Vodafone Cash", "*110#", "#E60000"),
        AIRTEL_TIGO("AirtelTigo Money", "*500#", "#ED1C24");
        
        companion object {
            fun fromString(value: String): Provider {
                return entries.find { 
                    it.name.equals(value, ignoreCase = true) || 
                    it.displayName.equals(value, ignoreCase = true)
                } ?: MTN
            }
        }
    }
    
    /**
     * Generate USSD payment string for this provider.
     */
    fun toUssdString(): String {
        return when (provider) {
            Provider.MTN -> "tel:*170*1*1*${merchantPhone}*${amount}#"
            Provider.VODAFONE -> "tel:*110*1*${merchantPhone}*${amount}#"
            Provider.AIRTEL_TIGO -> "tel:*500*1*${merchantPhone}*${amount}#"
        }
    }
    
    /**
     * Generate payment URI for NFC transmission.
     */
    fun toPaymentUri(): String {
        val ref = reference?.let { "&ref=$it" } ?: ""
        return "momo://pay?to=$merchantPhone&amount=$amount&currency=$currency&provider=${provider.name}$ref"
    }
    
    /**
     * Validate the payment data.
     */
    fun isValid(): Boolean {
        return merchantPhone.isNotBlank() &&
                amount.isNotBlank() &&
                amount.toDoubleOrNull()?.let { it > 0 } ?: false
    }
}
