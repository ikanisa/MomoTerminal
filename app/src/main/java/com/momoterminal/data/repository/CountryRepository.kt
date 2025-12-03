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
import com.momoterminal.config.SupportedCountries
import com.momoterminal.data.model.CountryConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for country configurations.
 * Fetches from Supabase with fallback to local data.
 */
@Singleton
class CountryRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val _countries = MutableStateFlow<List<CountryConfig>>(SupportedCountries.PRIMARY_LAUNCH)
    val countries: Flow<List<CountryConfig>> = _countries.asStateFlow()

    private val _selectedCountry = MutableStateFlow(CountryConfig.DEFAULT)
    val selectedCountry: Flow<CountryConfig> = _selectedCountry.asStateFlow()

    suspend fun fetchCountries(): Result<List<CountryConfig>> {
        return try {
            val result = supabaseClient.postgrest["countries"]
                .select { filter { eq("is_active", true) } }
                .decodeList<CountryConfig>()
            
            if (result.isNotEmpty()) {
                _countries.value = result
                Timber.d("Loaded ${result.size} countries from Supabase")
            } else {
                _countries.value = SupportedCountries.PRIMARY_LAUNCH
                Timber.d("Using fallback countries (empty response)")
            }
            Result.success(_countries.value)
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch countries, using fallback")
            _countries.value = SupportedCountries.PRIMARY_LAUNCH
            Result.failure(e)
        }
    }

    fun getByCode(code: String): CountryConfig? {
        return _countries.value.find { it.code.equals(code, ignoreCase = true) }
            ?: SupportedCountries.getByCode(code)
    }

    fun setSelectedCountry(country: CountryConfig) {
        _selectedCountry.value = country
    }

    fun getCurrentCountry(): CountryConfig = _selectedCountry.value

    fun getCachedCountries(): List<CountryConfig> = _countries.value
}
