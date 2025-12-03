package com.momoterminal.core.data.repository

import com.momoterminal.core.database.dao.NfcTagDao
import com.momoterminal.core.database.entity.NfcTagEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface NfcRepository {
    fun getAllTags(): Flow<List<NfcTagEntity>>
    fun getTagsByType(type: String): Flow<List<NfcTagEntity>>
    suspend fun findByTagId(tagId: String): NfcTagEntity?
    suspend fun findByEntityId(entityId: String): NfcTagEntity?
    suspend fun saveTag(tag: NfcTagEntity)
    suspend fun updateLastScanned(tagId: String)
    suspend fun deleteTag(tagId: String)
}

@Singleton
class NfcRepositoryImpl @Inject constructor(
    private val dao: NfcTagDao
) : NfcRepository {
    override fun getAllTags() = dao.getAll()
    override fun getTagsByType(type: String) = dao.getByType(type)
    override suspend fun findByTagId(tagId: String) = dao.findByTagId(tagId)
    override suspend fun findByEntityId(entityId: String) = dao.findByEntityId(entityId)
    override suspend fun saveTag(tag: NfcTagEntity) = dao.insert(tag)
    override suspend fun updateLastScanned(tagId: String) = dao.updateLastScanned(tagId)
    override suspend fun deleteTag(tagId: String) = dao.deleteByTagId(tagId)
}
