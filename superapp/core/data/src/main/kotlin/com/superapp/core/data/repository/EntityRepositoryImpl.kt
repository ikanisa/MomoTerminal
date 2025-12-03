package com.superapp.core.data.repository

import com.superapp.core.common.result.Result
import com.superapp.core.data.mapper.toDomain
import com.superapp.core.data.mapper.toEntity
import com.superapp.core.data.source.EntityLocalDataSource
import com.superapp.core.data.source.EntityRemoteDataSource
import com.superapp.core.domain.model.Entity
import com.superapp.core.domain.model.PaginatedResult
import com.superapp.core.domain.repository.EntityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EntityRepositoryImpl @Inject constructor(
    private val remoteDataSource: EntityRemoteDataSource,
    private val localDataSource: EntityLocalDataSource
) : EntityRepository {

    override fun getEntities(page: Int, pageSize: Int): Flow<Result<PaginatedResult<Entity>>> = flow {
        emit(Result.Loading)

        try {
            val offset = (page - 1) * pageSize
            localDataSource.getEntities(pageSize, offset)
                .map { entities -> entities.map { it.toDomain() } }
                .collect { cachedEntities ->
                    if (cachedEntities.isNotEmpty()) {
                        emit(Result.Success(PaginatedResult(
                            data = cachedEntities,
                            pagination = com.superapp.core.domain.model.Pagination(
                                page = page,
                                pageSize = pageSize,
                                totalPages = 1,
                                totalItems = cachedEntities.size
                            )
                        )))
                    }
                }

            val response = remoteDataSource.getEntities(page, pageSize)
            localDataSource.insertEntities(response.data.map { it.toEntity() })
            
            emit(Result.Success(response.toDomain { it.toDomain() }))
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }

    override fun getEntityById(id: String): Flow<Result<Entity>> = flow {
        emit(Result.Loading)

        try {
            localDataSource.getEntityById(id).collect { cached ->
                cached?.let { emit(Result.Success(it.toDomain())) }
            }

            val remote = remoteDataSource.getEntityById(id)
            localDataSource.insertEntities(listOf(remote.toEntity()))
            emit(Result.Success(remote.toDomain()))
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }

    override suspend fun refreshEntities() {
        localDataSource.clearAll()
    }
}
