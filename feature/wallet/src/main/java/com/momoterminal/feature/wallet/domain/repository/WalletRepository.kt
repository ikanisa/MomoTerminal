package com.momoterminal.feature.wallet.domain.repository

import com.momoterminal.feature.wallet.domain.model.Token
import com.momoterminal.feature.wallet.domain.model.WalletBalance
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for wallet operations.
 */
interface WalletRepository {
    /**
     * Get the current wallet balance as a Flow.
     */
    fun getBalance(): Flow<WalletBalance>
    
    /**
     * Get all active tokens.
     */
    fun getActiveTokens(): Flow<List<Token>>
    
    /**
     * Get all tokens (including spent/expired).
     */
    fun getAllTokens(): Flow<List<Token>>
    
    /**
     * Add a new token to the wallet.
     */
    suspend fun addToken(token: Token): Result<Token>
    
    /**
     * Spend tokens from the wallet.
     */
    suspend fun spendTokens(amount: Long, reference: String): Result<Unit>
    
    /**
     * Get token by ID.
     */
    suspend fun getTokenById(id: String): Token?
    
    /**
     * Sync wallet with remote backend.
     */
    suspend fun syncWallet(): Result<Unit>
}
