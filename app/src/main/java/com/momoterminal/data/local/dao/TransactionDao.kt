package com.momoterminal.data.local.dao

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
}
