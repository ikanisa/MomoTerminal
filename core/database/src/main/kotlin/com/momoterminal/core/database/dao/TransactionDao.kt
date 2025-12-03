package com.momoterminal.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.momoterminal.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for transaction operations.
 * Provides methods to insert, query, and update transactions in the local database.
 * Includes PagingSource queries for efficient pagination.
 */
@Dao
interface TransactionDao {
    
    /**
     * Insert a transaction. Returns the row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long
    
    /**
     * Delete a transaction.
     */
    @Delete
    suspend fun delete(transaction: TransactionEntity)
    
    /**
     * Get all pending transactions.
     */
    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY timestamp DESC")
    suspend fun getPendingTransactions(): List<TransactionEntity>
    
    /**
     * Update transaction status.
     */
    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
    
    /**
     * Get recent transactions as a Flow.
     */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>>
    
    /**
     * Get count of pending transactions as a Flow.
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>
    
    /**
     * Delete transactions older than specified timestamp.
     */
    @Query("DELETE FROM transactions WHERE timestamp < :timestamp AND status = 'SENT'")
    suspend fun deleteOldTransactions(timestamp: Long): Int
    
    /**
     * Get transactions by date range (for analytics).
     */
    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getTransactionsByDateRange(startTime: Long, endTime: Long): List<TransactionEntity>
    
    /**
     * Get transaction by ID.
     */
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?
    
    /**
     * Get transaction by ID (alias for getById).
     */
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?
    
    /**
     * Get all transactions (for backup/export).
     */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>
    
    /**
     * Clear all transactions.
     */
    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    // ============= Sync Queries =============

    /**
     * Get unsynced transactions.
     */
    @Query("SELECT * FROM transactions WHERE syncedAt IS NULL ORDER BY timestamp ASC LIMIT 50")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    /**
     * Mark transaction as synced.
     */
    @Query("UPDATE transactions SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markAsSynced(id: Long, syncedAt: Long = System.currentTimeMillis())

    // ============= Pagination Queries =============
    
    /**
     * Get all transactions with pagination support.
     * Used with Paging 3 library for efficient infinite scrolling.
     */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getTransactionsPagingSource(): PagingSource<Int, TransactionEntity>
    
    /**
     * Get transactions filtered by status with pagination.
     */
    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY timestamp DESC")
    fun getTransactionsByStatusPagingSource(status: String): PagingSource<Int, TransactionEntity>
    
    /**
     * Get transactions filtered by sender (provider) with pagination.
     */
    @Query("SELECT * FROM transactions WHERE sender LIKE '%' || :provider || '%' ORDER BY timestamp DESC")
    fun getTransactionsByProviderPagingSource(provider: String): PagingSource<Int, TransactionEntity>
    
    /**
     * Get transactions filtered by date range with pagination.
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp 
        ORDER BY timestamp DESC
    """)
    fun getTransactionsByDateRangePagingSource(
        startTimestamp: Long,
        endTimestamp: Long
    ): PagingSource<Int, TransactionEntity>
    
    /**
     * Search transactions by text content with pagination.
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE body LIKE '%' || :query || '%' 
           OR sender LIKE '%' || :query || '%'
           OR transactionId LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchTransactionsPagingSource(query: String): PagingSource<Int, TransactionEntity>
    
    /**
     * Get transactions with combined filters.
     * Filters are applied when the parameter is not null.
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE (:status IS NULL OR status = :status)
          AND (:provider IS NULL OR sender LIKE '%' || :provider || '%')
          AND (:startTimestamp IS NULL OR timestamp >= :startTimestamp)
          AND (:endTimestamp IS NULL OR timestamp <= :endTimestamp)
          AND (:searchQuery IS NULL OR body LIKE '%' || :searchQuery || '%' 
               OR sender LIKE '%' || :searchQuery || '%'
               OR transactionId LIKE '%' || :searchQuery || '%')
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
        ORDER BY timestamp DESC
    """)
    fun getFilteredTransactionsPagingSource(
        status: String?,
        provider: String?,
        startTimestamp: Long?,
        endTimestamp: Long?,
        searchQuery: String?,
        minAmount: Double?,
        maxAmount: Double?
    ): PagingSource<Int, TransactionEntity>
    
    /**
     * Get total count of transactions matching filters.
     * Useful for showing total count in UI.
     */
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE (:status IS NULL OR status = :status)
          AND (:provider IS NULL OR sender LIKE '%' || :provider || '%')
          AND (:startTimestamp IS NULL OR timestamp >= :startTimestamp)
          AND (:endTimestamp IS NULL OR timestamp <= :endTimestamp)
          AND (:searchQuery IS NULL OR body LIKE '%' || :searchQuery || '%' 
               OR sender LIKE '%' || :searchQuery || '%')
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
    """)
    fun getFilteredTransactionCount(
        status: String?,
        provider: String?,
        startTimestamp: Long?,
        endTimestamp: Long?,
        searchQuery: String?,
        minAmount: Double?,
        maxAmount: Double?
    ): Flow<Int>
}
