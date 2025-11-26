package com.momoterminal.data

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
    val sender: String,
    val body: String,
    val timestamp: Long,
    val status: String  // Values: 'PENDING', 'SENT', 'FAILED'
)
