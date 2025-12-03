package com.superapp.core.domain.usecase.entity

import com.superapp.core.common.result.Result
import com.superapp.core.domain.model.Entity
import com.superapp.core.domain.model.PaginatedResult
import com.superapp.core.domain.repository.EntityRepository
import com.superapp.core.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class GetEntitiesParams(
    val page: Int = 1,
    val pageSize: Int = 20
)

class GetEntitiesUseCase @Inject constructor(
    private val repository: EntityRepository
) : FlowUseCase<GetEntitiesParams, PaginatedResult<Entity>>() {

    override fun invoke(params: GetEntitiesParams): Flow<Result<PaginatedResult<Entity>>> {
        return repository.getEntities(params.page, params.pageSize)
    }
}
