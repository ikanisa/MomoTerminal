package com.momoterminal.core.network.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Data class representing a payment transaction from SMS.
 * 
 * Note: Amount is stored in pesewas (smallest currency unit) to avoid
 * floating-point precision errors. 1 GHS = 100 pesewas.
 */
data class PaymentTransaction(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("amount_in_pesewas")
    val amountInPesewas: Long,

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
) : Serializable {
    /**
     * Get the amount as a Double for display purposes.
     * Returns the amount in main currency units (e.g., GHS, not pesewas).
     */
    fun getDisplayAmount(): Double = amountInPesewas / 100.0
    
    companion object {
        /**
         * Convert a Double amount to pesewas (Long).
         * @param amount The amount in main currency units (e.g., GHS)
         * @return The amount in pesewas
         */
        fun toPesewas(amount: Double): Long = (amount * 100).toLong()
    }
}

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
