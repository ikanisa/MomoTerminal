package com.momoterminal.core.data.repository

// import com.google.null // gson.Gson
// import com.google.null // gson.reflect.TypeToken
import com.momoterminal.core.database.dao.WalletDao
import com.momoterminal.core.database.entity.TokenTransactionEntity
import com.momoterminal.core.database.entity.TokenWalletEntity
import com.momoterminal.core.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface WalletRepository {
    fun observeWallet(userId: String): Flow<TokenWallet?>
    fun observeTransactions(walletId: String): Flow<List<TokenTransaction>>
    suspend fun getOrCreateWallet(userId: String): TokenWallet
    suspend fun applyTransaction(
        walletId: String,
        amount: Long,
        type: TokenTransactionType,
        reference: String? = null,
        referenceType: ReferenceType? = null,
        description: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): Result<TokenWallet>
    suspend fun getBalance(userId: String): Long
}

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val walletDao: WalletDao
) : WalletRepository {
    
    // private val gson = Gson()  // TODO: Add Gson dependency if needed
    
    override fun observeWallet(userId: String): Flow<TokenWallet?> {
        return walletDao.observeByUserId(userId).map { it?.toDomain() }
    }
    
    override fun observeTransactions(walletId: String): Flow<List<TokenTransaction>> {
        return walletDao.observeTransactions(walletId).map { list ->
            list.map { it.toDomain() }
        }
    }
    
    override suspend fun getOrCreateWallet(userId: String): TokenWallet {
        val existing = walletDao.getByUserId(userId)
        if (existing != null) return existing.toDomain()
        
        val wallet = TokenWalletEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            balance = 0
        )
        walletDao.insert(wallet)
        return wallet.toDomain()
    }
    
    override suspend fun applyTransaction(
        walletId: String,
        amount: Long,
        type: TokenTransactionType,
        reference: String?,
        referenceType: ReferenceType?,
        description: String?,
        metadata: Map<String, String>
    ): Result<TokenWallet> {
        return try {
            val wallet = walletDao.getById(walletId)
                ?: return Result.failure(IllegalStateException("Wallet not found"))
            
            val newBalance = wallet.balance + amount
            if (newBalance < 0) {
                return Result.failure(IllegalStateException("Insufficient balance"))
            }
            
            val transaction = TokenTransactionEntity(
                id = UUID.randomUUID().toString(),
                walletId = walletId,
                amount = amount,
                type = type.name,
                balanceBefore = wallet.balance,
                balanceAfter = newBalance,
                reference = reference,
                referenceType = referenceType?.name,
                description = description,
                metadata = null // if (metadata.isNotEmpty()) gson.toJson(metadata) else null
            )
            
            val updated = walletDao.applyTransaction(wallet, transaction)
            Result.success(updated.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getBalance(userId: String): Long {
        return walletDao.getByUserId(userId)?.balance ?: 0
    }
    
    private fun TokenWalletEntity.toDomain() = TokenWallet(
        id = id,
        userId = userId,
        balance = balance,
        currency = currency,
        walletType = WalletType.valueOf(walletType),
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        syncStatus = SyncStatus.valueOf(syncStatus)
    )
    
    private fun TokenTransactionEntity.toDomain(): TokenTransaction {
        val type = String::class.java
        return TokenTransaction(
            id = id,
            walletId = walletId,
            amount = amount,
            type = TokenTransactionType.valueOf(this.type),
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            reference = reference,
            referenceType = referenceType?.let { ReferenceType.valueOf(it) },
            description = description,
            metadata = emptyMap(), // metadata?.let { gson.fromJson<Map<String, String>>(it, type) } ?: emptyMap(),
            createdAt = Instant.ofEpochMilli(createdAt),
            syncStatus = SyncStatus.valueOf(syncStatus)
        )
    }
}
