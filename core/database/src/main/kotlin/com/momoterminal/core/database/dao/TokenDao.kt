package com.momoterminal.core.database.dao

import androidx.room.*
import com.momoterminal.core.database.entity.TokenEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for wallet token operations.
 */
@Dao
interface TokenDao {
    
    @Query("SELECT * FROM wallet_tokens WHERE user_id = :userId AND status = 'ACTIVE' ORDER BY timestamp DESC")
    fun getActiveTokens(userId: String): Flow<List<TokenEntity>>
    
    @Query("SELECT * FROM wallet_tokens WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getAllTokens(userId: String): Flow<List<TokenEntity>>
    
    @Query("SELECT SUM(amount) FROM wallet_tokens WHERE user_id = :userId AND status = 'ACTIVE'")
    fun getTotalBalance(userId: String): Flow<Long?>
    
    @Query("SELECT COUNT(*) FROM wallet_tokens WHERE user_id = :userId AND status = 'ACTIVE'")
    fun getActiveTokenCount(userId: String): Flow<Int>
    
    @Query("SELECT * FROM wallet_tokens WHERE id = :tokenId")
    suspend fun getTokenById(tokenId: String): TokenEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToken(token: TokenEntity): Long
    
    @Update
    suspend fun updateToken(token: TokenEntity)
    
    @Query("UPDATE wallet_tokens SET status = 'SPENT' WHERE id IN (:tokenIds)")
    suspend fun markTokensAsSpent(tokenIds: List<String>)
    
    @Query("DELETE FROM wallet_tokens WHERE user_id = :userId")
    suspend fun deleteAllTokens(userId: String)
}
