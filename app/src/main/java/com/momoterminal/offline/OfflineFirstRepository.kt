package com.momoterminal.offline

import com.momoterminal.core.error.MomoError
import com.momoterminal.core.error.toMomoError
import com.momoterminal.core.logging.MomoLogger
import com.momoterminal.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Base class for offline-first repository pattern.
 * 
 * Strategy:
 * 1. Always read from local DB first (immediate response)
 * 2. Fetch from network in background when available
 * 3. Update local DB with fresh data
 * 4. Emit updates via Flow
 */
abstract class OfflineFirstRepository<T>(
    protected val networkMonitor: NetworkMonitor
) {
    /**
     * Get data with offline-first strategy.
     * Returns local data immediately, then refreshes from network.
     */
    fun getData(): Flow<OfflineResult<T>> {
        return getLocalData()
            .map<T, OfflineResult<T>> { data ->
                // Try to refresh from network if connected
                if (networkMonitor.isConnected) {
                    try {
                        val fresh = fetchFromNetwork()
                        saveToLocal(fresh)
                        OfflineResult.Fresh(fresh)
                    } catch (e: Exception) {
                        MomoLogger.w(TAG, "Network fetch failed, using cached data")
                        OfflineResult.Cached(data, stale = true)
                    }
                } else {
                    OfflineResult.Cached(data, stale = false)
                }
            }
            .catch { e ->
                MomoLogger.e(TAG, "Error getting data", e)
                emit(OfflineResult.Error(e.toMomoError()))
            }
    }
    
    /**
     * Force refresh from network.
     */
    suspend fun refresh(): Result<T> {
        return try {
            if (!networkMonitor.isConnected) {
                return Result.failure(Exception("No network connection"))
            }
            val data = fetchFromNetwork()
            saveToLocal(data)
            Result.success(data)
        } catch (e: Exception) {
            MomoLogger.e(TAG, "Refresh failed", e)
            Result.failure(e)
        }
    }
    
    protected abstract fun getLocalData(): Flow<T>
    protected abstract suspend fun fetchFromNetwork(): T
    protected abstract suspend fun saveToLocal(data: T)
    
    companion object {
        private const val TAG = "OfflineRepo"
    }
}

/**
 * Result wrapper for offline-first data.
 */
sealed class OfflineResult<out T> {
    data class Fresh<T>(val data: T) : OfflineResult<T>()
    data class Cached<T>(val data: T, val stale: Boolean) : OfflineResult<T>()
    data class Error(val error: MomoError) : OfflineResult<Nothing>()
    
    val dataOrNull: T?
        get() = when (this) {
            is Fresh -> data
            is Cached -> data
            is Error -> null
        }
    
    val isStale: Boolean
        get() = this is Cached && stale
}
