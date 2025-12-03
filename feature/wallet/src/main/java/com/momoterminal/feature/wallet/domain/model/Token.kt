package com.momoterminal.feature.wallet.domain.model

import java.util.UUID

/**
 * Represents a token in the wallet system.
 * Tokens are the core unit of value in MomoTerminal.
 */
data class Token(
    val id: String = UUID.randomUUID().toString(),
    val amount: Long, // Amount in smallest currency unit (e.g., cents)
    val currency: String,
    val sourceReference: String? = null, // Reference to SMS or NFC transaction
    val sourceType: TokenSourceType,
    val timestamp: Long = System.currentTimeMillis(),
    val status: TokenStatus = TokenStatus.ACTIVE,
    val expiresAt: Long? = null
)

enum class TokenSourceType {
    SMS_RECEIVED,
    NFC_RECEIVED,
    MANUAL_CREDIT,
    REFUND
}

enum class TokenStatus {
    ACTIVE,
    SPENT,
    EXPIRED,
    PENDING
}
