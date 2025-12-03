package com.momoterminal.data.local.dao

import androidx.room.*
import com.momoterminal.data.local.entity.TokenTransactionEntity
import com.momoterminal.data.local.entity.TokenWalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    
    @Query("SELECT * FROM token_wallets WHERE userId = :userId LIMIT 1")
    fun observeByUserId(userId: String): Flow<TokenWalletEntity?>
    
    @Query("SELECT * FROM token_wallets WHERE id = :id")
    suspend fun getById(id: String): TokenWalletEntity?
    
    @Query("SELECT * FROM token_wallets WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): TokenWalletEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: TokenWalletEntity)
    
    @Query("UPDATE token_wallets SET balance = :balance, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBalance(id: String, balance: Long, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE token_wallets SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
    
    // Transactions
    @Query("SELECT * FROM token_transactions WHERE walletId = :walletId ORDER BY createdAt DESC")
    fun observeTransactions(walletId: String): Flow<List<TokenTransactionEntity>>
    
    @Query("SELECT * FROM token_transactions WHERE walletId = :walletId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentTransactions(walletId: String, limit: Int = 50): List<TokenTransactionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TokenTransactionEntity)
    
    @Query("SELECT * FROM token_transactions WHERE syncStatus = 'PENDING'")
    suspend fun getUnsyncedTransactions(): List<TokenTransactionEntity>
    
    @Query("UPDATE token_transactions SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markTransactionsSynced(ids: List<String>)
    
    @Transaction
    suspend fun applyTransaction(wallet: TokenWalletEntity, transaction: TokenTransactionEntity): TokenWalletEntity {
        insertTransaction(transaction)
        updateBalance(wallet.id, transaction.balanceAfter)
        return wallet.copy(balance = transaction.balanceAfter, updatedAt = System.currentTimeMillis())
    }
}
