package com.momoterminal.data.repository

import com.momoterminal.data.model.CountryConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching country configurations from Supabase.
 */
@Singleton
class CountryRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val _countries = MutableStateFlow<List<CountryConfig>>(emptyList())
    val countries: Flow<List<CountryConfig>> = _countries.asStateFlow()

    private val _selectedCountry = MutableStateFlow(CountryConfig.DEFAULT)
    val selectedCountry: Flow<CountryConfig> = _selectedCountry.asStateFlow()

    /**
     * Fetch all active countries from Supabase.
     */
    suspend fun fetchCountries(): Result<List<CountryConfig>> {
        return try {
            val result = supabaseClient.postgrest["countries"]
                .select {
                    filter { eq("is_active", true) }
                }
                .decodeList<CountryConfig>()
            
            _countries.value = result.ifEmpty { listOf(CountryConfig.DEFAULT) }
            Result.success(_countries.value)
        } catch (e: Exception) {
            // Fallback to default on error
            _countries.value = listOf(CountryConfig.DEFAULT)
            Result.failure(e)
        }
    }

    /**
     * Get country by code.
     */
    fun getByCode(code: String): CountryConfig? {
        return _countries.value.find { it.code.equals(code, ignoreCase = true) }
    }

    /**
     * Set selected country for MoMo operations.
     */
    fun setSelectedCountry(country: CountryConfig) {
        _selectedCountry.value = country
    }

    /**
     * Get current selected country.
     */
    fun getCurrentCountry(): CountryConfig = _selectedCountry.value

    /**
     * Get cached countries list.
     */
    fun getCachedCountries(): List<CountryConfig> = _countries.value
}
