package com.momoterminal.feature.wallet.domain.model

/**
 * Represents the current wallet balance.
 */
data class WalletBalance(
    val totalTokens: Long,
    val currency: String,
    val activeTokenCount: Int,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * Format the balance for display.
     */
    fun formatAmount(): String {
        val major = totalTokens / 100
        val minor = totalTokens % 100
        return String.format("%,d.%02d", major, minor)
    }
}
