package com.momoterminal.data.repository

import com.momoterminal.data.local.dao.SmsTransactionDao
import com.momoterminal.core.database.entity.SmsTransactionEntity
import com.momoterminal.data.local.entity.SmsTransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface SmsRepository {
    fun getTransactions(): Flow<List<SmsTransactionEntity>>
    suspend fun insert(transaction: SmsTransactionEntity)
    suspend fun markSynced(ids: List<String>)
    suspend fun getUnsynced(): List<SmsTransactionEntity>
    suspend fun getUncreditedReceived(): List<SmsTransactionEntity>
    suspend fun markWalletCredited(id: String)
    suspend fun findByReference(ref: String): SmsTransactionEntity?
    fun observeUnsyncedCount(): Flow<Int>
}

@Singleton
class SmsRepositoryImpl @Inject constructor(
    private val dao: SmsTransactionDao
) : SmsRepository {
    override fun getTransactions() = dao.getAll()
    override suspend fun insert(transaction: SmsTransactionEntity) = dao.insert(transaction)
    override suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)
    override suspend fun getUnsynced() = dao.getUnsynced()
    override suspend fun getUncreditedReceived() = dao.getUncreditedByType(SmsTransactionType.RECEIVED)
    override suspend fun markWalletCredited(id: String) = dao.markWalletCredited(id)
    override suspend fun findByReference(ref: String) = dao.findByReference(ref)
    override fun observeUnsyncedCount() = dao.observeUnsyncedCount()
}
