package com.momoterminal.core.domain.repository

import com.momoterminal.core.common.Result
import com.momoterminal.core.domain.model.PaginatedResult
import com.momoterminal.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactions(page: Int, pageSize: Int): Flow<Result<PaginatedResult<Transaction>>>
    fun getTransactionById(id: String): Flow<Result<Transaction>>
    fun getRecentTransactions(limit: Int = 20): Flow<List<Transaction>>
    suspend fun getPendingCount(): Int
    suspend fun createTransaction(transaction: Transaction): Result<Transaction>
    suspend fun syncTransactions(): Result<Unit>
}
