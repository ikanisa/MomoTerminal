package com.momoterminal.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.momoterminal.core.common.Result
import com.momoterminal.core.data.mapper.TransactionMapper
import com.momoterminal.core.database.dao.TransactionDao
import com.momoterminal.core.domain.model.PaginatedResult
import com.momoterminal.core.domain.model.Transaction
import com.momoterminal.core.domain.model.TransactionFilter
import com.momoterminal.core.domain.repository.TransactionRepository
import com.momoterminal.core.network.api.MomoApiService
import com.momoterminal.core.security.SecureStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val apiService: MomoApiService,
    private val secureStorage: SecureStorage
) : TransactionRepository {

    override fun getTransactions(page: Int, pageSize: Int): Flow<Result<PaginatedResult<Transaction>>> = flow {
        emit(Result.Success(PaginatedResult(emptyList(), page, pageSize, 0, 0)))
    }

    override fun getTransactionById(id: String): Flow<Result<Transaction>> = flow {
        emit(Result.Error(Exception("Not implemented")))
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flow {
        emit(emptyList())
    }

    override suspend fun getPendingCount(): Int = 0

    override suspend fun createTransaction(transaction: Transaction): Result<Transaction> {
        return Result.Error(Exception("Not implemented"))
    }

    override suspend fun syncTransactions(): Result<Unit> {
        return Result.Success(Unit)
    }
}
