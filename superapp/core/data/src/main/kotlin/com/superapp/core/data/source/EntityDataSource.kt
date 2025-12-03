package com.superapp.core.data.source

import com.superapp.core.database.dao.EntityDao
import com.superapp.core.database.entity.EntityEntity
import com.superapp.core.network.api.EntityApiService
import com.superapp.core.network.model.EntityDto
import com.superapp.core.network.model.PaginatedResponseDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface EntityRemoteDataSource {
    suspend fun getEntities(page: Int, pageSize: Int): PaginatedResponseDto<EntityDto>
    suspend fun getEntityById(id: String): EntityDto
}

class EntityRemoteDataSourceImpl @Inject constructor(
    private val apiService: EntityApiService
) : EntityRemoteDataSource {

    override suspend fun getEntities(page: Int, pageSize: Int): PaginatedResponseDto<EntityDto> {
        return apiService.getEntities(page, pageSize)
    }

    override suspend fun getEntityById(id: String): EntityDto {
        return apiService.getEntityById(id)
    }
}

interface EntityLocalDataSource {
    fun getEntities(limit: Int, offset: Int): Flow<List<EntityEntity>>
    fun getEntityById(id: String): Flow<EntityEntity?>
    suspend fun insertEntities(entities: List<EntityEntity>)
    suspend fun clearAll()
}

class EntityLocalDataSourceImpl @Inject constructor(
    private val dao: EntityDao
) : EntityLocalDataSource {

    override fun getEntities(limit: Int, offset: Int): Flow<List<EntityEntity>> {
        return dao.getEntities(limit, offset)
    }

    override fun getEntityById(id: String): Flow<EntityEntity?> {
        return dao.getEntityById(id)
    }

    override suspend fun insertEntities(entities: List<EntityEntity>) {
        dao.insertEntities(entities)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}
