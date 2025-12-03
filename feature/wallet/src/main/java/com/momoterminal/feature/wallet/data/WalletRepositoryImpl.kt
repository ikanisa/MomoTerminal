package com.momoterminal.feature.wallet.data

import com.momoterminal.core.database.dao.TokenDao
import com.momoterminal.core.database.entity.TokenEntity
import com.momoterminal.feature.wallet.domain.model.Token
import com.momoterminal.feature.wallet.domain.model.TokenSourceType
import com.momoterminal.feature.wallet.domain.model.TokenStatus
import com.momoterminal.feature.wallet.domain.model.WalletBalance
import com.momoterminal.feature.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of WalletRepository using Room database.
 */
@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val tokenDao: TokenDao,
    private val userId: String // Injected from TokenManager or similar
) : WalletRepository {

    override fun getBalance(): Flow<WalletBalance> {
        return combine(
            tokenDao.getTotalBalance(userId),
            tokenDao.getActiveTokenCount(userId)
        ) { total, count ->
            WalletBalance(
                totalTokens = total ?: 0L,
                currency = "RWF", // TODO: Get from config
                activeTokenCount = count
            )
        }
    }

    override fun getActiveTokens(): Flow<List<Token>> {
        return tokenDao.getActiveTokens(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getAllTokens(): Flow<List<Token>> {
        return tokenDao.getAllTokens(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun addToken(token: Token): Result<Token> {
        return try {
            val entity = token.toEntity(userId)
            tokenDao.insertToken(entity)
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun spendTokens(amount: Long, reference: String): Result<Unit> {
        return try {
            // TODO: Implement token spending logic
            // This should select tokens FIFO and mark them as spent
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTokenById(id: String): Token? {
        return tokenDao.getTokenById(id)?.toDomainModel()
    }

    override suspend fun syncWallet(): Result<Unit> {
        return try {
            // TODO: Implement sync with remote backend
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Extension functions for mapping
private fun TokenEntity.toDomainModel(): Token {
    return Token(
        id = id,
        amount = amount,
        currency = currency,
        sourceReference = sourceReference,
        sourceType = TokenSourceType.valueOf(sourceType),
        timestamp = timestamp,
        status = TokenStatus.valueOf(status),
        expiresAt = expiresAt
    )
}

private fun Token.toEntity(userId: String): TokenEntity {
    return TokenEntity(
        id = id,
        amount = amount,
        currency = currency,
        sourceReference = sourceReference,
        sourceType = sourceType.name,
        timestamp = timestamp,
        status = status.name,
        expiresAt = expiresAt,
        userId = userId
    )
}
