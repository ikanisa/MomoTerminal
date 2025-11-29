package com.momoterminal.data.mapper

import com.momoterminal.data.local.entity.TransactionEntity
import com.momoterminal.data.remote.dto.SyncRequestDto
import com.momoterminal.data.remote.dto.TransactionDto
import com.momoterminal.domain.model.SyncStatus
import com.momoterminal.domain.model.Transaction
import android.os.Build

/**
 * Mapper functions for converting between domain models, entities, and DTOs.
 * 
 * Note: All amount values are in pesewas (smallest currency unit) to avoid
 * floating-point precision errors. 1 GHS = 100 pesewas.
 */
object TransactionMapper {
    
    /**
     * Convert TransactionEntity to domain Transaction.
     */
    fun entityToDomain(entity: TransactionEntity): Transaction {
        return Transaction(
            id = entity.id,
            sender = entity.sender,
            body = entity.body,
            amountInPesewas = entity.amountInPesewas,
            currency = entity.currency ?: "GHS",
            transactionId = entity.transactionId,
            timestamp = entity.timestamp,
            status = SyncStatus.fromValue(entity.status),
            merchantCode = entity.merchantCode
        )
    }
    
    /**
     * Convert domain Transaction to TransactionEntity.
     */
    fun domainToEntity(transaction: Transaction): TransactionEntity {
        return TransactionEntity(
            id = transaction.id,
            sender = transaction.sender,
            body = transaction.body,
            amount = transaction.amountInPesewas?.let { it / 100.0 },
            currency = transaction.currency,
            transactionId = transaction.transactionId,
            timestamp = transaction.timestamp,
            status = transaction.status.value,
            merchantCode = transaction.merchantCode
        )
    }
    
    /**
     * Convert TransactionEntity to SyncRequestDto for API calls.
     */
    fun entityToSyncRequest(
        entity: TransactionEntity,
        merchantPhone: String
    ): SyncRequestDto {
        return SyncRequestDto(
            sender = entity.sender,
            text = entity.body,
            timestamp = entity.timestamp,
            device = Build.MODEL,
            merchant = merchantPhone,
            amountInPesewas = entity.amountInPesewas,
            transactionId = entity.transactionId
        )
    }
    
    /**
     * Convert TransactionDto to domain Transaction.
     */
    fun dtoToDomain(dto: TransactionDto): Transaction {
        return Transaction(
            id = dto.id?.toLongOrNull() ?: 0,
            sender = dto.sender,
            body = dto.body ?: "",
            amountInPesewas = dto.amountInPesewas,
            currency = dto.currency,
            transactionId = dto.transactionId,
            timestamp = dto.timestamp,
            status = SyncStatus.fromValue(dto.status),
            merchantCode = dto.merchantCode
        )
    }
    
    /**
     * Convert list of TransactionEntity to list of domain Transaction.
     */
    fun entityListToDomain(entities: List<TransactionEntity>): List<Transaction> {
        return entities.map { entityToDomain(it) }
    }
}
