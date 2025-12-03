package com.momoterminal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "token_transactions",
    foreignKeys = [
        ForeignKey(
            entity = TokenWalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("walletId"), Index("createdAt")]
)
data class TokenTransactionEntity(
    @PrimaryKey
    val id: String,
    val walletId: String,
    val amount: Long,
    val type: String,
    val balanceBefore: Long,
    val balanceAfter: Long,
    val reference: String? = null,
    val referenceType: String? = null,
    val description: String? = null,
    val metadata: String? = null, // JSON string
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)
