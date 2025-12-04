package com.momoterminal.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.momoterminal.core.database.dao.TransactionDao
import com.momoterminal.core.data.mapper.TransactionMapper
import com.momoterminal.data.remote.api.MomoApiService
import com.momoterminal.core.domain.model.SyncStatus
import com.momoterminal.core.domain.model.Transaction
import com.momoterminal.core.domain.model.TransactionFilter
import com.momoterminal.core.domain.repository.TransactionRepository
import com.momoterminal.core.security.SecureStorage
import com.momoterminal.core.common.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TransactionRepository.
 * Handles data operations between local database and remote API.
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val apiService: MomoApiService,
    private val secureStorage: SecureStorage
) : TransactionRepository {
    
    companion object {
        private const val PAGE_SIZE = 20
        private const val PREFETCH_DISTANCE = 5
    }
    
    override suspend fun insertTransaction(transaction: Transaction): Result<Long> {
        return try {
            val entity = TransactionMapper.domainToEntity(transaction)
            val id = transactionDao.insert(entity)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getPendingTransactions(): Result<List<Transaction>> {
        return try {
            val entities = transactionDao.getPendingTransactions()
            val transactions = TransactionMapper.entityListToDomain(entities)
            Result.Success(transactions)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun updateTransactionStatus(id: Long, status: SyncStatus): Result<Unit> {
        return try {
            transactionDao.updateStatus(id, status.name)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit).map { entities ->
            TransactionMapper.entityListToDomain(entities)
        }
    }
    
    override fun getPendingCount(): Flow<Int> {
        return transactionDao.getPendingCount()
    }
    
    override suspend fun syncPendingTransactions(): Result<Int> {
        return try {
            val merchantPhone = secureStorage.getMerchantCode() ?: ""
            val pendingEntities = transactionDao.getPendingTransactions()
            var syncedCount = 0
            
            for (entity in pendingEntities) {
                try {
                    val request = TransactionMapper.entityToSyncRequest(entity, merchantPhone)
                    val response = apiService.syncTransaction(request)
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        transactionDao.updateStatus(entity.id, SyncStatus.SENT.name)
                        syncedCount++
                    } else {
                        // Keep as pending for retry
                    }
                } catch (e: Exception) {
                    // Individual sync failure - continue with others
                }
            }
            
            Result.Success(syncedCount)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun deleteOldTransactions(olderThanDays: Int): Result<Int> {
        return try {
            val threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(olderThanDays.toLong())
            val deletedCount = transactionDao.deleteOldTransactions(threshold)
            Result.Success(deletedCount)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    // ============= Pagination Methods =============
    
    override fun getTransactionsPaged(filter: TransactionFilter): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                transactionDao.getFilteredTransactionsPagingSource(
                    status = filter.status?.name,
                    provider = filter.provider?.name,
                    startTimestamp = filter.startDate?.time,
                    endTimestamp = filter.endDate?.time,
                    searchQuery = filter.searchQuery?.takeIf { it.isNotBlank() },
                    minAmount = filter.minAmount,
                    maxAmount = filter.maxAmount
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { entity ->
                TransactionMapper.entityToDomain(entity)
            }
        }
    }
    
    override fun getFilteredTransactionCount(filter: TransactionFilter): Flow<Int> {
        return transactionDao.getFilteredTransactionCount(
            status = filter.status?.name,
            provider = filter.provider?.name,
            startTimestamp = filter.startDate?.time,
            endTimestamp = filter.endDate?.time,
            searchQuery = filter.searchQuery?.takeIf { it.isNotBlank() },
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount
        )
    }
}
