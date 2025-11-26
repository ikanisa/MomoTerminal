package com.momoterminal.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for sync request to the server.
 */
data class SyncRequestDto(
    @SerializedName("sender")
    val sender: String,
    
    @SerializedName("text")
    val text: String,
    
    @SerializedName("timestamp")
    val timestamp: Long,
    
    @SerializedName("device")
    val device: String,
    
    @SerializedName("merchant")
    val merchant: String,
    
    @SerializedName("amount")
    val amount: Double? = null,
    
    @SerializedName("transaction_id")
    val transactionId: String? = null
)

/**
 * Data Transfer Object for sync response from the server.
 */
data class SyncResponseDto(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("transaction_id")
    val transactionId: String? = null,
    
    @SerializedName("error")
    val error: String? = null
)
