package com.momoterminal.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Data class representing a payment transaction from SMS.
 */
data class PaymentTransaction(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("currency")
    val currency: String = "GHS",

    @SerializedName("sender_number")
    val senderNumber: String,

    @SerializedName("recipient_number")
    val recipientNumber: String? = null,

    @SerializedName("transaction_id")
    val transactionId: String,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @SerializedName("raw_message")
    val rawMessage: String,

    @SerializedName("status")
    val status: TransactionStatus = TransactionStatus.PENDING,

    @SerializedName("merchant_code")
    val merchantCode: String? = null
) : Serializable

/**
 * Enum for transaction status.
 */
enum class TransactionStatus {
    @SerializedName("pending")
    PENDING,

    @SerializedName("confirmed")
    CONFIRMED,

    @SerializedName("failed")
    FAILED,

    @SerializedName("synced")
    SYNCED
}

/**
 * Response from the PWA API after syncing a transaction.
 */
data class SyncResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("transaction_id")
    val transactionId: String? = null
)

/**
 * NFC payment data to be transmitted via HCE.
 */
data class NfcPaymentData(
    val amount: Double,
    val currency: String = "GHS",
    val merchantCode: String,
    val ussdCode: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Converts the payment data to a byte array for NFC transmission.
     * Format: USSD dial string that can be directly launched
     */
    fun toNdefPayload(): String {
        // Create a USSD string that can be dialed
        // Format varies by provider, this is a generic template
        // *xxx*merchantCode*amount#
        return "tel:$ussdCode"
    }

    companion object {
        /**
         * Creates a USSD dial string for MTN Mobile Money Ghana.
         */
        fun createMtnUssd(merchantCode: String, amount: Double): String {
            return "*170*1*1*$merchantCode*${"%.2f".format(amount)}#"
        }

        /**
         * Creates a USSD dial string for Vodafone Cash Ghana.
         */
        fun createVodafoneUssd(merchantCode: String, amount: Double): String {
            return "*110*1*$merchantCode*${"%.2f".format(amount)}#"
        }

        /**
         * Creates a USSD dial string for AirtelTigo Money Ghana.
         */
        fun createAirtelTigoUssd(merchantCode: String, amount: Double): String {
            return "*500*1*$merchantCode*${"%.2f".format(amount)}#"
        }
    }
}
