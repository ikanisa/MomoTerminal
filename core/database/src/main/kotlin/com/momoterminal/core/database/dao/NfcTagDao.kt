package com.momoterminal.core.database.dao

import androidx.room.*
import com.momoterminal.core.database.entity.NfcTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NfcTagDao {
    @Query("SELECT * FROM nfc_tags ORDER BY lastScanned DESC")
    fun getAll(): Flow<List<NfcTagEntity>>

    @Query("SELECT * FROM nfc_tags WHERE tagId = :tagId")
    suspend fun findByTagId(tagId: String): NfcTagEntity?

    @Query("SELECT * FROM nfc_tags WHERE entityId = :entityId LIMIT 1")
    suspend fun findByEntityId(entityId: String): NfcTagEntity?

    @Query("SELECT * FROM nfc_tags WHERE entityType = :type ORDER BY lastScanned DESC")
    fun getByType(type: String): Flow<List<NfcTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: NfcTagEntity)

    @Update
    suspend fun update(tag: NfcTagEntity)

    @Delete
    suspend fun delete(tag: NfcTagEntity)

    @Query("DELETE FROM nfc_tags WHERE tagId = :tagId")
    suspend fun deleteByTagId(tagId: String)

    @Query("UPDATE nfc_tags SET lastScanned = :timestamp WHERE tagId = :tagId")
    suspend fun updateLastScanned(tagId: String, timestamp: Long = System.currentTimeMillis())
}
