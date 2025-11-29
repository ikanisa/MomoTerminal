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
