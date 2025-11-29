package com.momoterminal.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for transaction data sent to the server.
 * 
 * Note: Amount is stored in pesewas (smallest currency unit) to avoid
 * floating-point precision errors. 1 GHS = 100 pesewas.
 */
data class TransactionDto(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("sender")
    val sender: String,
    
    @SerializedName("body")
    val body: String? = null,
    
    @SerializedName("amount_in_pesewas")
    val amountInPesewas: Long? = null,
    
    @SerializedName("currency")
    val currency: String = "GHS",
    
    @SerializedName("transaction_id")
    val transactionId: String? = null,
    
    @SerializedName("timestamp")
    val timestamp: Long,
    
    @SerializedName("status")
    val status: String = "PENDING",
    
    @SerializedName("merchant_code")
    val merchantCode: String? = null
)
