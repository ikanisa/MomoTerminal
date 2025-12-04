package com.momoterminal.domain.usecase

import com.momoterminal.core.database.entity.SmsTransactionEntity
import com.momoterminal.data.repository.SmsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Simple SMS transaction use cases.
 * Just read and mark synced - no complex integration.
 */

class GetSmsTransactionsUseCase @Inject constructor(
    private val smsRepository: SmsRepository
) {
    fun observeAll(): Flow<List<SmsTransactionEntity>> = smsRepository.getTransactions()
    
    fun observeUnsyncedCount(): Flow<Int> = smsRepository.observeUnsyncedCount()
    
    suspend fun getUnsynced(): List<SmsTransactionEntity> = smsRepository.getUnsynced()
}

class MarkSmsSyncedUseCase @Inject constructor(
    private val smsRepository: SmsRepository
) {
    suspend operator fun invoke(ids: List<String>) {
        smsRepository.markSynced(ids)
    }
}
