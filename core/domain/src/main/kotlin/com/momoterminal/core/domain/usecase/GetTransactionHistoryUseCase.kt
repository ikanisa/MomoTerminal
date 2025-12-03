package com.momoterminal.core.domain.usecase

import com.momoterminal.core.domain.model.Transaction
import com.momoterminal.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving transaction history.
 */
class GetTransactionHistoryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    
    /**
     * Get recent transactions as a Flow.
     * 
     * @param limit Maximum number of transactions to retrieve
     * @return Flow of transaction list
     */
    operator fun invoke(limit: Int = 10): Flow<List<Transaction>> {
        return transactionRepository.getRecentTransactions(limit)
    }
    
    /**
     * Get count of pending transactions.
     * 
     * @return Flow of pending count
     */
    fun getPendingCount(): Flow<Int> {
        return transactionRepository.getPendingCount()
    }
}
