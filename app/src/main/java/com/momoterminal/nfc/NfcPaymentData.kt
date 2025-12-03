package com.momoterminal.nfc

import com.momoterminal.config.UssdConfig

/**
 * Data class representing payment data for NFC transactions.
 */
data class NfcPaymentData(
    val merchantPhone: String,
    val amountInMinorUnits: Long,
    val currency: String,
    val countryCode: String = "RW",
    val provider: Provider = Provider.MTN,
    val reference: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Legacy alias
    val amountInPesewas: Long get() = amountInMinorUnits

    enum class Provider(val displayName: String, val colorHex: String) {
        MTN("MTN MoMo", "#FFCC00"),
        AIRTEL("Airtel Money", "#ED1C24"),
        VODACOM("M-Pesa", "#E60000"),
        VODAFONE("Vodafone Cash", "#E60000"),
        ORANGE("Orange Money", "#FF6600"),
        TIGO("Tigo Pesa", "#00A0DF"),
        WAVE("Wave", "#1A1F71"),
        MOOV("Moov Money", "#0066B3"),
        ECOCASH("EcoCash", "#00A651"),
        TMONEY("T-Money", "#FF6B00"),
        MVOLA("MVola", "#E31937");

        companion object {
            fun fromString(value: String): Provider {
                return entries.find { 
                    it.name.equals(value, ignoreCase = true) || 
                    it.displayName.contains(value, ignoreCase = true)
                } ?: MTN
            }
        }
    }

    fun getDisplayAmount(): Double = amountInMinorUnits / 100.0
    fun getFormattedAmount(): String = "%.2f".format(getDisplayAmount())
    
    /**
     * Get the whole number amount (no decimals) for USSD.
     * Most African mobile money uses whole numbers.
     */
    fun getWholeAmount(): String = amountInMinorUnits.div(100).toString()

    /**
     * Generate USSD code for merchant payment using country config.
     */
    fun toUssdString(): String {
        val rawUssd = getRawUssdCode()
        return try {
            "tel:${android.net.Uri.encode(rawUssd)}"
        } catch (e: Exception) {
            // Fallback for unit tests where Android classes aren't available
            "tel:$rawUssd"
        }
    }

    /**
     * Get raw USSD code (without tel: prefix).
     */
    fun getRawUssdCode(): String {
        return UssdConfig.generateMerchantPaymentUssd(
            countryCode = countryCode,
            merchantCode = merchantPhone,
            amount = getWholeAmount()
        ) ?: "*182*8*1*$merchantPhone*${getWholeAmount()}#"
    }

    fun toPaymentUri(): String {
        val ref = reference?.let { "&ref=$it" } ?: ""
        return "momo://pay?to=$merchantPhone&amount=${getFormattedAmount()}&currency=$currency&provider=${provider.name}$ref"
    }

    fun isValid(): Boolean = merchantPhone.isNotBlank() && amountInMinorUnits > 0

    companion object {
        fun toMinorUnits(amount: Double): Long = (amount * 100).toLong()

        fun fromAmount(
            merchantPhone: String,
            amount: Double,
            currency: String,
            countryCode: String = "RW",
            provider: Provider = Provider.MTN,
            reference: String? = null
        ): NfcPaymentData {
            return NfcPaymentData(
                merchantPhone = merchantPhone,
                amountInMinorUnits = toMinorUnits(amount),
                currency = currency,
                countryCode = countryCode,
                provider = provider,
                reference = reference
            )
        }
    }
}
