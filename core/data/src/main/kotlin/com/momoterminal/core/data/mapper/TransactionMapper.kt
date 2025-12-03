package com.momoterminal.core.data.mapper

import com.momoterminal.core.database.entity.TransactionEntity
import com.momoterminal.data.remote.dto.SyncRequestDto
import com.momoterminal.data.remote.dto.TransactionDto
import com.momoterminal.core.domain.model.SyncStatus
import com.momoterminal.core.domain.model.Transaction
import android.os.Build
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Mapper functions for converting between domain models, entities, and DTOs.
 * 
 * Note: All amount values in domain/DTO are in pesewas (smallest currency unit) to avoid
 * floating-point precision errors. 1 GHS = 100 pesewas.
 * Entity stores amount as Double for Room compatibility but uses precise conversion.
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
     * Uses BigDecimal for precise currency conversion from pesewas to main unit.
     */
    fun domainToEntity(transaction: Transaction): TransactionEntity {
        return TransactionEntity(
            id = transaction.id,
            sender = transaction.sender,
            body = transaction.body,
            amount = transaction.amountInPesewas?.let { pesewas ->
                BigDecimal(pesewas).divide(BigDecimal(100), 2, RoundingMode.HALF_UP).toDouble()
            },
            currency = transaction.currency,
            transactionId = transaction.transactionId,
            timestamp = transaction.timestamp,
            status = transaction.status.name,
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
