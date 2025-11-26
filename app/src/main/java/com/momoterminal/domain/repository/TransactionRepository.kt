package com.momoterminal.domain.repository

import androidx.paging.PagingData
import com.momoterminal.domain.model.SyncStatus
import com.momoterminal.domain.model.Transaction
import com.momoterminal.domain.model.TransactionFilter
import com.momoterminal.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for transaction operations.
 * Defines the contract between domain and data layers.
 */
interface TransactionRepository {
    
    /**
     * Insert a new transaction.
     */
    suspend fun insertTransaction(transaction: Transaction): Result<Long>
    
    /**
     * Get all pending transactions.
     */
    suspend fun getPendingTransactions(): Result<List<Transaction>>
    
    /**
     * Update transaction status.
     */
    suspend fun updateTransactionStatus(id: Long, status: SyncStatus): Result<Unit>
    
    /**
     * Get recent transactions as a Flow for UI updates.
     */
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>>
    
    /**
     * Get count of pending transactions as a Flow.
     */
    fun getPendingCount(): Flow<Int>
    
    /**
     * Sync pending transactions to the server.
     */
    suspend fun syncPendingTransactions(): Result<Int>
    
    /**
     * Delete old synced transactions.
     */
    suspend fun deleteOldTransactions(olderThanDays: Int = 30): Result<Int>
    
    // ============= Pagination Methods =============
    
    /**
     * Get paginated transactions with optional filters.
     * Uses Paging 3 library for efficient infinite scrolling.
     */
    fun getTransactionsPaged(filter: TransactionFilter = TransactionFilter()): Flow<PagingData<Transaction>>
    
    /**
     * Get count of transactions matching the filter.
     */
    fun getFilteredTransactionCount(filter: TransactionFilter): Flow<Int>
}
