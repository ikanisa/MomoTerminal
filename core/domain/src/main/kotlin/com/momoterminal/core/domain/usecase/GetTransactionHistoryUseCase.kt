package com.momoterminal.core.domain.usecase

import com.momoterminal.core.domain.model.Transaction
import com.momoterminal.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class GetTransactionHistoryUseCase(
    private val repository: TransactionRepository
) {
    fun getRecentTransactions(limit: Int = 20): Flow<List<Transaction>> =
        repository.getRecentTransactions(limit)
    
    suspend fun getPendingCount(): Int = repository.getPendingCount()
}
