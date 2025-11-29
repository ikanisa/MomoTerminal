package com.momoterminal.nfc

/**
 * Data class representing payment data for NFC transactions.
 * 
 * Note: Amount is stored in pesewas (smallest currency unit) to avoid
 * floating-point precision errors. 1 GHS = 100 pesewas.
 */
data class NfcPaymentData(
    val merchantPhone: String,
    val amountInPesewas: Long,
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
     * Get the amount as a Double for display purposes.
     * Returns the amount in main currency units (e.g., GHS, not pesewas).
     */
    fun getDisplayAmount(): Double = amountInPesewas / 100.0
    
    /**
     * Get the amount formatted as a string for display (e.g., "50.00").
     */
    fun getFormattedAmount(): String = "%.2f".format(getDisplayAmount())
    
    /**
     * Generate USSD payment string for this provider.
     */
    fun toUssdString(): String {
        val formattedAmount = getFormattedAmount()
        return when (provider) {
            Provider.MTN -> "tel:*170*1*1*${merchantPhone}*${formattedAmount}#"
            Provider.VODAFONE -> "tel:*110*1*${merchantPhone}*${formattedAmount}#"
            Provider.AIRTEL_TIGO -> "tel:*500*1*${merchantPhone}*${formattedAmount}#"
        }
    }
    
    /**
     * Generate payment URI for NFC transmission.
     */
    fun toPaymentUri(): String {
        val ref = reference?.let { "&ref=$it" } ?: ""
        return "momo://pay?to=$merchantPhone&amount=${getFormattedAmount()}&currency=$currency&provider=${provider.name}$ref"
    }
    
    /**
     * Validate the payment data.
     */
    fun isValid(): Boolean {
        return merchantPhone.isNotBlank() && amountInPesewas > 0
    }
    
    companion object {
        /**
         * Convert a Double amount to pesewas (Long).
         * @param amount The amount in main currency units (e.g., GHS)
         * @return The amount in pesewas
         */
        fun toPesewas(amount: Double): Long = (amount * 100).toLong()
        
        /**
         * Create NfcPaymentData from a Double amount.
         * @param merchantPhone The merchant phone number
         * @param amount The amount in main currency units (e.g., GHS)
         * @param currency The currency code (default: GHS)
         * @param provider The mobile money provider
         * @param reference Optional payment reference
         */
        fun fromAmount(
            merchantPhone: String,
            amount: Double,
            currency: String = "GHS",
            provider: Provider = Provider.MTN,
            reference: String? = null
        ): NfcPaymentData {
            return NfcPaymentData(
                merchantPhone = merchantPhone,
                amountInPesewas = toPesewas(amount),
                currency = currency,
                provider = provider,
                reference = reference
            )
        }
    }
}
