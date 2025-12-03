package com.momoterminal.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "token_wallets")
data class TokenWalletEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val balance: Long,
    val currency: String = "CREDITS",
    val walletType: String = "CREDITS",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)
