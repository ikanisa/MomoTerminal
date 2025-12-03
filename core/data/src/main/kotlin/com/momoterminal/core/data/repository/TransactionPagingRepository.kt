package com.momoterminal.core.data.repository

import androidx.paging.*
import com.momoterminal.core.database.dao.TransactionDao
import com.momoterminal.core.database.entity.TransactionEntity
import com.momoterminal.core.domain.model.TransactionFilter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for paginated transaction access.
 * Uses Paging 3 for efficient infinite scrolling of large SMS histories.
 */
@Singleton
class TransactionPagingRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    /**
     * Get all transactions with pagination.
     */
    fun getTransactionsPaged(pageSize: Int = PAGE_SIZE): Flow<PagingData<TransactionEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = pageSize / 2,
                enablePlaceholders = false,
                initialLoadSize = pageSize * 2
            ),
            pagingSourceFactory = { transactionDao.getTransactionsPagingSource() }
        ).flow
    }
    
    /**
     * Get transactions filtered by status.
     */
    fun getTransactionsByStatus(status: String): Flow<PagingData<TransactionEntity>> {
        return Pager(
            config = defaultPagingConfig(),
            pagingSourceFactory = { transactionDao.getTransactionsByStatusPagingSource(status) }
        ).flow
    }
    
    /**
     * Get transactions filtered by provider.
     */
    fun getTransactionsByProvider(provider: String): Flow<PagingData<TransactionEntity>> {
        return Pager(
            config = defaultPagingConfig(),
            pagingSourceFactory = { transactionDao.getTransactionsByProviderPagingSource(provider) }
        ).flow
    }
    
    /**
     * Get transactions filtered by date range.
     */
    fun getTransactionsByDateRange(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<PagingData<TransactionEntity>> {
        return Pager(
            config = defaultPagingConfig(),
            pagingSourceFactory = { 
                transactionDao.getTransactionsByDateRangePagingSource(startTimestamp, endTimestamp) 
            }
        ).flow
    }
    
    /**
     * Search transactions by query.
     */
    fun searchTransactions(query: String): Flow<PagingData<TransactionEntity>> {
        return Pager(
            config = defaultPagingConfig(),
            pagingSourceFactory = { transactionDao.searchTransactionsPagingSource(query) }
        ).flow
    }
    
    /**
     * Get transactions with combined filters.
     */
    fun getFilteredTransactions(filter: TransactionFilter): Flow<PagingData<TransactionEntity>> {
        return Pager(
            config = defaultPagingConfig(),
            pagingSourceFactory = {
                transactionDao.getFilteredTransactionsPagingSource(
                    status = filter.status?.name,
                    provider = filter.provider?.name,
                    startTimestamp = filter.startDate?.time,
                    endTimestamp = filter.endDate?.time,
                    searchQuery = filter.searchQuery,
                    minAmount = filter.minAmount,
                    maxAmount = filter.maxAmount
                )
            }
        ).flow
    }
    
    /**
     * Get count of filtered transactions.
     */
    fun getFilteredCount(filter: TransactionFilter): Flow<Int> {
        return transactionDao.getFilteredTransactionCount(
            status = filter.status?.name,
            provider = filter.provider?.name,
            startTimestamp = filter.startDate?.time,
            endTimestamp = filter.endDate?.time,
            searchQuery = filter.searchQuery,
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount
        )
    }
    
    private fun defaultPagingConfig() = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = PAGE_SIZE / 2,
        enablePlaceholders = false,
        initialLoadSize = PAGE_SIZE * 2
    )
    
    companion object {
        private const val PAGE_SIZE = 20
    }
}
