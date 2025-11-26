package com.momoterminal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a transaction stored in the local database.
 * Used for offline-first reliability - SMS messages are saved immediately
 * and synced to the configured webhook when connectivity is available.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * The sender of the SMS (e.g., "MTN MoMo").
     */
    val sender: String,
    
    /**
     * The raw SMS body content.
     */
    val body: String,
    
    /**
     * The timestamp when the SMS was received.
     */
    val timestamp: Long,
    
    /**
     * The sync status: 'PENDING', 'SENT', 'FAILED'.
     */
    val status: String,
    
    /**
     * Optional: Extracted amount from the SMS.
     */
    val amount: Double? = null,
    
    /**
     * Optional: Currency code (default: GHS).
     */
    val currency: String? = "GHS",
    
    /**
     * Optional: Extracted transaction ID.
     */
    val transactionId: String? = null,
    
    /**
     * Optional: Merchant code associated with this transaction.
     */
    val merchantCode: String? = null
)
