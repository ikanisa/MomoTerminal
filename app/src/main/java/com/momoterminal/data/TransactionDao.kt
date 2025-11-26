package com.momoterminal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for transaction operations.
 * Provides methods to insert, query, and update transactions in the local database.
 */
@Dao
interface TransactionDao {
    
    @Insert
    suspend fun insert(txn: TransactionEntity)
    
    @Query("SELECT * FROM transactions WHERE status = 'PENDING'")
    suspend fun getPendingTransactions(): List<TransactionEntity>
    
    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 10")
    fun getRecentTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>
}
