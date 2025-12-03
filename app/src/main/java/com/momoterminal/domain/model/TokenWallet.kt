package com.momoterminal.domain.model

import java.time.Instant

data class TokenWallet(
    val id: String,
    val userId: String,
    val balance: Long,
    val currency: String = "CREDITS",
    val walletType: WalletType = WalletType.CREDITS,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

enum class WalletType {
    CREDITS, VOUCHERS, LOYALTY_POINTS, INTERNAL_CURRENCY
}

data class TokenTransaction(
    val id: String,
    val walletId: String,
    val amount: Long,
    val type: TokenTransactionType,
    val balanceBefore: Long,
    val balanceAfter: Long,
    val reference: String? = null,
    val referenceType: ReferenceType? = null,
    val description: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Instant = Instant.now(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

enum class TokenTransactionType {
    SMS_CREDIT,
    NFC_CREDIT,
    NFC_DEBIT,
    MANUAL_CREDIT,
    MANUAL_DEBIT,
    TRANSFER_IN,
    TRANSFER_OUT,
    EXPIRY
}

enum class ReferenceType {
    SMS_TRANSACTION, NFC_TAG, MANUAL
}
