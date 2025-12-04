package com.momoterminal.core.data.mapper

import com.momoterminal.core.database.entity.TransactionEntity
import com.momoterminal.core.domain.model.Transaction
import com.momoterminal.core.domain.model.TransactionStatus
import com.momoterminal.core.domain.model.TransactionType
import java.time.Instant

/**
 * Mapper functions for converting between domain models and entities.
 */
object TransactionMapper {
    
    /**
     * Convert TransactionEntity to domain Transaction.
     */
    fun entityToDomain(entity: TransactionEntity): Transaction {
        return Transaction(
            id = entity.id.toString(),
            amount = entity.amount ?: 0.0,
            currency = entity.currency ?: "RWF",
            status = mapStatus(entity.status),
            type = TransactionType.PAYMENT, // Default type
            reference = entity.transactionId ?: "",
            description = entity.body,
            createdAt = Instant.ofEpochMilli(entity.timestamp),
            updatedAt = Instant.ofEpochMilli(entity.timestamp)
        )
    }
    
    /**
     * Convert domain Transaction to TransactionEntity.
     */
    fun domainToEntity(transaction: Transaction): TransactionEntity {
        return TransactionEntity(
            id = transaction.id.toLongOrNull() ?: 0L,
            sender = "", // Not in new domain model
            body = transaction.description ?: "",
            amount = transaction.amount,
            currency = transaction.currency,
            transactionId = transaction.reference,
            timestamp = transaction.createdAt.toEpochMilli(),
            status = transaction.status.name,
            merchantCode = null
        )
    }
    
    /**
     * Map status string to TransactionStatus enum.
     */
    private fun mapStatus(status: String): TransactionStatus {
        return when (status.uppercase()) {
            "PENDING" -> TransactionStatus.PENDING
            "COMPLETED", "SUCCESS" -> TransactionStatus.COMPLETED
            "FAILED" -> TransactionStatus.FAILED
            "CANCELLED" -> TransactionStatus.CANCELLED
            else -> TransactionStatus.PENDING
        }
    }
    
    /**
     * Convert list of TransactionEntity to list of domain Transaction.
     */
    fun entityListToDomain(entities: List<TransactionEntity>): List<Transaction> {
        return entities.map { entityToDomain(it) }
    }
}
