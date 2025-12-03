package com.momoterminal.core.domain.model

import java.time.Instant

data class Transaction(
    val id: String,
    val amount: Double,
    val currency: String,
    val status: TransactionStatus,
    val type: TransactionType,
    val reference: String,
    val description: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class TransactionStatus {
    PENDING, COMPLETED, FAILED, CANCELLED
}

enum class TransactionType {
    PAYMENT, REFUND, TRANSFER
}

data class PaginatedResult<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int
) {
    val hasNext: Boolean get() = page < totalPages - 1
    val hasPrevious: Boolean get() = page > 0
}
