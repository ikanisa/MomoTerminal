package com.momoterminal.domain.usecase

import com.momoterminal.core.database.entity.SmsTransactionEntity
import com.momoterminal.data.repository.SmsRepository
// import com.momoterminal.sms.SmsWalletIntegrationService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSmsTransactionsUseCase @Inject constructor(
    private val smsRepository: SmsRepository
) {
    fun observeAll(): Flow<List<SmsTransactionEntity>> = smsRepository.getTransactions()
    
    fun observeUnsyncedCount(): Flow<Int> = smsRepository.observeUnsyncedCount()
    
    suspend fun getUnsynced(): List<SmsTransactionEntity> = smsRepository.getUnsynced()
}

/*
// Temporarily disabled - requires SMS wallet integration service
class ProcessIncomingSmsUseCase @Inject constructor(
    private val smsWalletService: SmsWalletIntegrationService
) {
    suspend operator fun invoke(
        userId: String,
        sender: String,
        body: String
    ): SmsWalletIntegrationService.ProcessResult {
        return smsWalletService.processIncomingSms(userId, sender, body)
    }
}

class ProcessUncreditedSmsUseCase @Inject constructor(
    private val smsWalletService: SmsWalletIntegrationService
) {
    suspend operator fun invoke(userId: String): Int {
        return smsWalletService.processUncreditedTransactions(userId)
    }
}
*/

class MarkSmsSyncedUseCase @Inject constructor(
    private val smsRepository: SmsRepository
) {
    suspend operator fun invoke(ids: List<String>) {
        smsRepository.markSynced(ids)
    }
}
