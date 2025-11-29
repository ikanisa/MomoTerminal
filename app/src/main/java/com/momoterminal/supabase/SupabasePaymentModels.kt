package com.momoterminal.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data model for Supabase payments table.
 * Represents a Mobile Money transaction to be stored in Supabase.
 */
@Serializable
data class SupabasePayment(
    @SerialName("id")
    val id: String? = null,
    
    @SerialName("amount_in_pesewas")
    val amountInPesewas: Long,
    
    @SerialName("currency")
    val currency: String = "GHS",
    
    @SerialName("sender_phone")
    val senderPhone: String? = null,
    
    @SerialName("recipient_phone")
    val recipientPhone: String? = null,
    
    @SerialName("transaction_id")
    val transactionId: String? = null,
    
    @SerialName("transaction_type")
    val transactionType: String,
    
    @SerialName("provider")
    val provider: String,
    
    @SerialName("balance_in_pesewas")
    val balanceInPesewas: Long? = null,
    
    @SerialName("raw_message")
    val rawMessage: String,
    
    @SerialName("device_id")
    val deviceId: String? = null,
    
    @SerialName("merchant_code")
    val merchantCode: String? = null,
    
    @SerialName("parsed_by")
    val parsedBy: String = "gemini",
    
    @SerialName("local_id")
    val localId: Long? = null,
    
    @SerialName("created_at")
    val createdAt: String? = null,
    
    @SerialName("synced_at")
    val syncedAt: String? = null
) {
    /**
     * Get display amount in main currency unit.
     */
    fun getDisplayAmount(): Double = amountInPesewas / 100.0
    
    /**
     * Get display balance in main currency unit.
     */
    fun getDisplayBalance(): Double? = balanceInPesewas?.let { it / 100.0 }
}

/**
 * Insert request for creating a new payment record.
 * Uses only the fields required for insertion (excludes auto-generated fields).
 */
@Serializable
data class SupabasePaymentInsert(
    @SerialName("amount_in_pesewas")
    val amountInPesewas: Long,
    
    @SerialName("currency")
    val currency: String = "GHS",
    
    @SerialName("sender_phone")
    val senderPhone: String? = null,
    
    @SerialName("recipient_phone")
    val recipientPhone: String? = null,
    
    @SerialName("transaction_id")
    val transactionId: String? = null,
    
    @SerialName("transaction_type")
    val transactionType: String,
    
    @SerialName("provider")
    val provider: String,
    
    @SerialName("balance_in_pesewas")
    val balanceInPesewas: Long? = null,
    
    @SerialName("raw_message")
    val rawMessage: String,
    
    @SerialName("device_id")
    val deviceId: String? = null,
    
    @SerialName("merchant_code")
    val merchantCode: String? = null,
    
    @SerialName("parsed_by")
    val parsedBy: String = "gemini",
    
    @SerialName("local_id")
    val localId: Long? = null
)

/**
 * Result of Supabase payment operation.
 */
sealed class PaymentResult {
    data class Success(val payment: SupabasePayment) : PaymentResult()
    data class Error(val message: String, val exception: Exception? = null) : PaymentResult()
}
