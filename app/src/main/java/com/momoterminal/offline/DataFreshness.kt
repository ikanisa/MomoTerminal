package com.momoterminal.offline

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.freshnessDataStore by preferencesDataStore(name = "data_freshness")

/**
 * Tracks data freshness for offline-first caching strategy.
 * Determines when cached data should be refreshed.
 */
@Singleton
class DataFreshness @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Data types that can be tracked for freshness.
     */
    enum class DataType(val maxAgeMs: Long) {
        TRANSACTIONS(TimeUnit.MINUTES.toMillis(5)),
        WALLET(TimeUnit.MINUTES.toMillis(1)),
        USER_PROFILE(TimeUnit.HOURS.toMillis(1)),
        SETTINGS(TimeUnit.DAYS.toMillis(1))
    }
    
    /**
     * Mark data as fresh (just fetched from network).
     */
    suspend fun markFresh(dataType: DataType) {
        val key = longPreferencesKey("${dataType.name}_last_fetch")
        context.freshnessDataStore.edit { prefs ->
            prefs[key] = System.currentTimeMillis()
        }
    }
    
    /**
     * Check if data is stale and needs refresh.
     */
    fun isStale(dataType: DataType): Flow<Boolean> {
        val key = longPreferencesKey("${dataType.name}_last_fetch")
        return context.freshnessDataStore.data.map { prefs ->
            val lastFetch = prefs[key] ?: 0L
            val age = System.currentTimeMillis() - lastFetch
            age > dataType.maxAgeMs
        }
    }
    
    /**
     * Get time since last fetch.
     */
    fun getAge(dataType: DataType): Flow<Long> {
        val key = longPreferencesKey("${dataType.name}_last_fetch")
        return context.freshnessDataStore.data.map { prefs ->
            val lastFetch = prefs[key] ?: 0L
            System.currentTimeMillis() - lastFetch
        }
    }
    
    /**
     * Get freshness status for UI display.
     */
    fun getFreshnessStatus(dataType: DataType): Flow<FreshnessStatus> {
        return getAge(dataType).map { age ->
            when {
                age < dataType.maxAgeMs / 2 -> FreshnessStatus.FRESH
                age < dataType.maxAgeMs -> FreshnessStatus.AGING
                else -> FreshnessStatus.STALE
            }
        }
    }
    
    /**
     * Clear freshness data (force refresh on next access).
     */
    suspend fun invalidate(dataType: DataType) {
        val key = longPreferencesKey("${dataType.name}_last_fetch")
        context.freshnessDataStore.edit { prefs ->
            prefs.remove(key)
        }
    }
    
    /**
     * Clear all freshness data.
     */
    suspend fun invalidateAll() {
        context.freshnessDataStore.edit { it.clear() }
    }
}

enum class FreshnessStatus {
    FRESH,  // Data is recent
    AGING,  // Data is getting old but still usable
    STALE   // Data should be refreshed
}
