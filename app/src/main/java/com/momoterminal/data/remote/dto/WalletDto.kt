package com.momoterminal.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.momoterminal.data.local.entity.*

data class WalletDto(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val balance: Long,
    val currency: String = "CREDITS",
    @SerializedName("wallet_type") val walletType: String = "CREDITS",
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long
) {
    fun toEntity() = TokenWalletEntity(
        id = id, userId = userId, balance = balance,
        currency = currency, walletType = walletType,
        createdAt = createdAt, updatedAt = updatedAt, syncStatus = "SYNCED"
    )
    
    companion object {
        fun fromEntity(e: TokenWalletEntity) = WalletDto(
            id = e.id, userId = e.userId, balance = e.balance,
            currency = e.currency, walletType = e.walletType,
            createdAt = e.createdAt, updatedAt = e.updatedAt
        )
    }
}

data class TokenTransactionDto(
    val id: String,
    @SerializedName("wallet_id") val walletId: String,
    val amount: Long,
    val type: String,
    @SerializedName("balance_before") val balanceBefore: Long,
    @SerializedName("balance_after") val balanceAfter: Long,
    val reference: String?,
    @SerializedName("reference_type") val referenceType: String?,
    val description: String?,
    val metadata: String?,
    @SerializedName("created_at") val createdAt: Long
) {
    fun toEntity() = TokenTransactionEntity(
        id = id, walletId = walletId, amount = amount, type = type,
        balanceBefore = balanceBefore, balanceAfter = balanceAfter,
        reference = reference, referenceType = referenceType,
        description = description, metadata = metadata,
        createdAt = createdAt, syncStatus = "SYNCED"
    )
    
    companion object {
        fun fromEntity(e: TokenTransactionEntity) = TokenTransactionDto(
            id = e.id, walletId = e.walletId, amount = e.amount, type = e.type,
            balanceBefore = e.balanceBefore, balanceAfter = e.balanceAfter,
            reference = e.reference, referenceType = e.referenceType,
            description = e.description, metadata = e.metadata, createdAt = e.createdAt
        )
    }
}

data class SmsTransactionDto(
    val id: String,
    @SerializedName("raw_message") val rawMessage: String,
    val sender: String,
    val amount: Double,
    val currency: String,
    val type: String,
    val balance: Double?,
    val reference: String?,
    val timestamp: Long
) {
    companion object {
        fun fromEntity(e: SmsTransactionEntity) = SmsTransactionDto(
            id = e.id, rawMessage = e.rawMessage, sender = e.sender,
            amount = e.amount, currency = e.currency, type = e.type.name,
            balance = e.balance, reference = e.reference, timestamp = e.timestamp
        )
    }
}

data class NfcTagDto(
    @SerializedName("tag_id") val tagId: String,
    @SerializedName("entity_type") val entityType: String,
    @SerializedName("entity_id") val entityId: String,
    val metadata: String?,
    @SerializedName("last_scanned") val lastScanned: Long
) {
    fun toEntity() = NfcTagEntity(
        tagId = tagId, entityType = entityType, entityId = entityId,
        metadata = metadata, lastScanned = lastScanned
    )
    
    companion object {
        fun fromEntity(e: NfcTagEntity) = NfcTagDto(
            tagId = e.tagId, entityType = e.entityType, entityId = e.entityId,
            metadata = e.metadata, lastScanned = e.lastScanned
        )
    }
}
