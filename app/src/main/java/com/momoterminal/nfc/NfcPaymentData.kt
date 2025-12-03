package com.momoterminal.nfc

/**
 * Data class representing payment data for NFC transactions.
 * 
 * Amount is stored in minor units (smallest currency unit) to avoid
 * floating-point precision errors. E.g., 100 = 1.00 in main currency.
 */
data class NfcPaymentData(
    val merchantPhone: String,
    val amountInMinorUnits: Long,
    val currency: String,
    val provider: Provider = Provider.MTN,
    val reference: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Legacy alias for backward compatibility
    val amountInPesewas: Long get() = amountInMinorUnits

    /**
     * Supported Mobile Money providers across Africa.
     */
    enum class Provider(
        val displayName: String,
        val colorHex: String
    ) {
        MTN("MTN Mobile Money", "#FFCC00"),
        AIRTEL("Airtel Money", "#ED1C24"),
        VODACOM("Vodacom M-Pesa", "#E60000"),
        VODAFONE("Vodafone Cash", "#E60000"),
        ORANGE("Orange Money", "#FF6600"),
        TIGO("Tigo Pesa", "#00A0DF"),
        WAVE("Wave", "#1A1F71"),
        MOOV("Moov Money", "#0066B3");

        companion object {
            fun fromString(value: String): Provider {
                return entries.find { 
                    it.name.equals(value, ignoreCase = true) || 
                    it.displayName.contains(value, ignoreCase = true)
                } ?: MTN
            }
        }
    }

    /**
     * Get the amount in main currency units for display.
     */
    fun getDisplayAmount(): Double = amountInMinorUnits / 100.0

    /**
     * Get the amount formatted as a string (e.g., "50.00").
     */
    fun getFormattedAmount(): String = "%.2f".format(getDisplayAmount())

    /**
     * Generate USSD dial string for this payment.
     * Note: Actual USSD codes vary by country and provider.
     */
    fun toUssdString(): String {
        val amount = getFormattedAmount()
        return "tel:*182*8*1*$merchantPhone*$amount#" // Generic format
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
    fun isValid(): Boolean = merchantPhone.isNotBlank() && amountInMinorUnits > 0

    companion object {
        /**
         * Convert a Double amount to minor units.
         */
        fun toMinorUnits(amount: Double): Long = (amount * 100).toLong()

        /**
         * Create NfcPaymentData from a Double amount.
         */
        fun fromAmount(
            merchantPhone: String,
            amount: Double,
            currency: String,
            provider: Provider = Provider.MTN,
            reference: String? = null
        ): NfcPaymentData {
            return NfcPaymentData(
                merchantPhone = merchantPhone,
                amountInMinorUnits = toMinorUnits(amount),
                currency = currency,
                provider = provider,
                reference = reference
            )
        }
    }
}
