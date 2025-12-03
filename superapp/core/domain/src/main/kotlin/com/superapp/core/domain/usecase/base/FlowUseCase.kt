package com.superapp.core.domain.usecase.base

import com.superapp.core.common.result.Result
import kotlinx.coroutines.flow.Flow

abstract class FlowUseCase<in Params, out T> {
    abstract operator fun invoke(params: Params): Flow<Result<T>>
}

abstract class NoParamsFlowUseCase<out T> {
    abstract operator fun invoke(): Flow<Result<T>>
}
