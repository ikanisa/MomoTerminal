package com.momoterminal.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entity for SMS-based transactions - core of offline-first architecture.
 */
@Entity(
    tableName = "sms_transactions",
    indices = [
        Index(value = ["reference"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["synced"]),
        Index(value = ["sync_status"])
    ]
)
data class SmsTransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "raw_message")
    val rawMessage: String,
    
    val sender: String,
    val amount: Double,
    val currency: String = "GHS",
    val type: SmsTransactionType = SmsTransactionType.UNKNOWN,
    val balance: Double? = null,
    val reference: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    
    @ColumnInfo(name = "wallet_credited")
    val walletCredited: Boolean = false,
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    
    @ColumnInfo(name = "parsed_by")
    val parsedBy: String = "regex",
    
    @ColumnInfo(name = "ai_confidence")
    val aiConfidence: Float = 0.0f,
    
    @ColumnInfo(name = "supabase_id")
    val supabaseId: String? = null
)

enum class SmsTransactionType {
    RECEIVED, SENT, CASH_OUT, AIRTIME, DEPOSIT, UNKNOWN
}

enum class SyncStatus { PENDING, SYNCING, SYNCED, FAILED }
