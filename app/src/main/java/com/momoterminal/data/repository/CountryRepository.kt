package com.momoterminal.data.repository

import com.momoterminal.domain.model.Country
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for country data operations.
 */
interface CountryRepository {
    /**
     * Get all active countries from the remote source.
     */
    suspend fun getActiveCountries(): Result<List<Country>>
    
    /**
     * Get primary market countries (for initial display).
     */
    suspend fun getPrimaryCountries(): Result<List<Country>>
    
    /**
     * Get a country by its code.
     */
    suspend fun getCountryByCode(code: String): Result<Country?>
    
    /**
     * Observe countries (with local cache).
     */
    fun observeCountries(): Flow<List<Country>>
    
    /**
     * Refresh countries from remote source.
     */
    suspend fun refreshCountries(): Result<Unit>
}
