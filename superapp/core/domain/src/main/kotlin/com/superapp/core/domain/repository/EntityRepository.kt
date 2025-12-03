package com.superapp.core.domain.repository

import com.superapp.core.common.result.Result
import com.superapp.core.domain.model.Entity
import com.superapp.core.domain.model.PaginatedResult
import kotlinx.coroutines.flow.Flow

interface EntityRepository {
    fun getEntities(page: Int, pageSize: Int): Flow<Result<PaginatedResult<Entity>>>
    fun getEntityById(id: String): Flow<Result<Entity>>
    suspend fun refreshEntities()
}
