package com.momoterminal.domain.model

/**
 * Domain model representing a transaction.
 * This is the clean domain representation without any framework dependencies.
 */
data class Transaction(
    val id: Long = 0,
    val sender: String,
    val body: String,
    val amount: Double? = null,
    val currency: String = "GHS",
    val transactionId: String? = null,
    val timestamp: Long,
    val status: SyncStatus,
    val merchantCode: String? = null
)
