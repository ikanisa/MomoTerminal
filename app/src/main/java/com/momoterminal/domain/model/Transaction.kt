package com.momoterminal.domain.model

/**
 * Domain model representing a transaction.
 * This is the clean domain representation without any framework dependencies.
 * 
 * Note: Amount is stored in pesewas (smallest currency unit) to avoid
 * floating-point precision errors. 1 GHS = 100 pesewas.
 */
data class Transaction(
    val id: Long = 0,
    val sender: String,
    val body: String,
    val amountInPesewas: Long? = null,
    val currency: String = "GHS",
    val transactionId: String? = null,
    val timestamp: Long,
    val status: SyncStatus,
    val merchantCode: String? = null
) {
    /**
     * Get the amount as a Double for display purposes.
     * Returns the amount in main currency units (e.g., GHS, not pesewas).
     */
    fun getDisplayAmount(): Double? = amountInPesewas?.let { it / 100.0 }
}
