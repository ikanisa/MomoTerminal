package com.momoterminal.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for transaction data sent to the server.
 */
data class TransactionDto(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("sender")
    val sender: String,
    
    @SerializedName("body")
    val body: String? = null,
    
    @SerializedName("amount")
    val amount: Double? = null,
    
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
