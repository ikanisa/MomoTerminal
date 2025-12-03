package com.momoterminal.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.roundToLong

/**
 * Room Entity representing a transaction stored in the local database.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** SMS sender (e.g., "MTN MoMo"). */
    val sender: String,
    
    /** Raw SMS body content. */
    val body: String,
    
    /** Timestamp when SMS was received. */
    val timestamp: Long,
    
    /** Sync status: 'PENDING', 'SENT', 'FAILED', 'completed'. */
    val status: String,
    
    /** Amount in main currency unit. */
    @ColumnInfo(name = "amount")
    val amount: Double? = null,
    
    /** Currency code. */
    val currency: String? = "RWF",
    
    /** Transaction ID from provider. */
    val transactionId: String? = null,
    
    /** Merchant code. */
    val merchantCode: String? = null,

    /** Transaction type: 'received', 'sent', 'payment', 'withdrawal'. */
    val type: String? = null,

    /** Provider name: 'MTN', 'AIRTEL', etc. */
    val provider: String? = null,

    /** Sender phone number. */
    val senderPhone: String? = null,

    /** Sender name. */
    val senderName: String? = null,

    /** Timestamp when synced to cloud. Null if not synced. */
    val syncedAt: Long? = null
) {
    /** Amount in minor units (pesewas/cents) for precise calculations. */
    val amountInPesewas: Long?
        get() = amount?.let { (it * 100).roundToLong() }
}
