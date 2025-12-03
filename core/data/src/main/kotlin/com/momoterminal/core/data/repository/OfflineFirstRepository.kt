package com.momoterminal.core.data.repository

import com.momoterminal.core.common.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

abstract class OfflineFirstRepository<T> {
    
    fun getData(
        forceRefresh: Boolean = false
    ): Flow<Result<T>> = flow {
        emit(Result.Loading)
        
        if (!forceRefresh) {
            val cached = loadFromCache()
            if (cached != null) {
                emit(Result.Success(cached))
            }
        }
        
        try {
            val remote = fetchFromNetwork()
            saveToCache(remote)
            emit(Result.Success(remote))
        } catch (e: Exception) {
            val cached = loadFromCache()
            if (cached != null) {
                emit(Result.Success(cached))
            } else {
                emit(Result.Error(e))
            }
        }
    }
    
    protected abstract suspend fun loadFromCache(): T?
    protected abstract suspend fun fetchFromNetwork(): T
    protected abstract suspend fun saveToCache(data: T)
}
