package com.momoterminal.data.local.dao

import androidx.room.*
import com.momoterminal.core.database.entity.SmsTransactionEntity
import com.momoterminal.data.local.entity.SmsTransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsTransactionDao {
    @Query("SELECT * FROM sms_transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SmsTransactionEntity>>

    @Query("SELECT * FROM sms_transactions WHERE synced = 0")
    suspend fun getUnsynced(): List<SmsTransactionEntity>

    @Query("SELECT * FROM sms_transactions WHERE wallet_credited = 0 AND type = :type")
    suspend fun getUncreditedByType(type: SmsTransactionType = SmsTransactionType.RECEIVED): List<SmsTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: SmsTransactionEntity)

    @Query("UPDATE sms_transactions SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("UPDATE sms_transactions SET wallet_credited = 1 WHERE id = :id")
    suspend fun markWalletCredited(id: String)

    @Query("SELECT * FROM sms_transactions WHERE id = :id")
    suspend fun getById(id: String): SmsTransactionEntity?

    @Query("SELECT * FROM sms_transactions WHERE reference = :ref LIMIT 1")
    suspend fun findByReference(ref: String): SmsTransactionEntity?

    @Query("SELECT COUNT(*) FROM sms_transactions WHERE synced = 0")
    fun observeUnsyncedCount(): Flow<Int>
}
