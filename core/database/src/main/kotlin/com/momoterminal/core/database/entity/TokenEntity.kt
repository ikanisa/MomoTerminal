package com.momoterminal.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity for wallet tokens.
 */
@Entity(
    tableName = "wallet_tokens",
    indices = [
        Index(value = ["source_reference"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["status"])
    ]
)
data class TokenEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val amount: Long,
    val currency: String,
    
    @ColumnInfo(name = "source_reference")
    val sourceReference: String? = null,
    
    @ColumnInfo(name = "source_type")
    val sourceType: String, // TokenSourceType as String
    
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // TokenStatus as String
    
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long? = null,
    
    @ColumnInfo(name = "user_id")
    val userId: String
)
