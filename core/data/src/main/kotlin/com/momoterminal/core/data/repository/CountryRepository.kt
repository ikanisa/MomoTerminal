package com.momoterminal.core.data.repository

import com.momoterminal.data.model.CountryConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for country data operations.
 */
interface CountryRepository {
    val countries: Flow<List<CountryConfig>>
    val selectedCountry: Flow<CountryConfig>

    suspend fun fetchCountries(): Result<List<CountryConfig>>
    fun getByCode(code: String): CountryConfig?
    fun setSelectedCountry(country: CountryConfig)
    fun getCurrentCountry(): CountryConfig
    fun getCachedCountries(): List<CountryConfig>
}
