package com.momoterminal.data.repository

import com.momoterminal.core.common.config.SupportedCountries
import com.momoterminal.core.common.model.CountryConfig
import com.momoterminal.data.model.CountryListItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for country configurations.
 * Primary: Supabase | Fallback: Local data
 */
@Singleton
class CountryRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : CountryRepository {

    companion object {
        private const val TABLE_NAME = "countries"
        private const val CACHE_DURATION_MS = 3600000L // 1 hour
    }

    private val _countries = MutableStateFlow<List<CountryConfig>>(SupportedCountries.PRIMARY_LAUNCH)
    override val countries: Flow<List<CountryConfig>> = _countries.asStateFlow()

    private val _selectedCountry = MutableStateFlow(CountryConfig.DEFAULT)
    override val selectedCountry: Flow<CountryConfig> = _selectedCountry.asStateFlow()

    private var lastFetchTime: Long = 0

    override suspend fun fetchCountries(): Result<List<CountryConfig>> = withContext(Dispatchers.IO) {
        if (_countries.value.isNotEmpty() && System.currentTimeMillis() - lastFetchTime < CACHE_DURATION_MS) {
            return@withContext Result.success(_countries.value)
        }

        try {
            val result = supabaseClient.postgrest[TABLE_NAME]
                .select {
                    filter { eq("is_active", true) }
                    order("launch_priority", Order.ASCENDING)
                }
                .decodeList<CountryConfig>()

            if (result.isNotEmpty()) {
                _countries.value = result
                lastFetchTime = System.currentTimeMillis()
                Timber.d("Loaded ${result.size} countries from Supabase")
            } else {
                _countries.value = SupportedCountries.getAllCountries()
            }
            Result.success(_countries.value)
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch countries, using fallback")
            _countries.value = SupportedCountries.getAllCountries()
            Result.failure(e)
        }
    }

    override fun getByCode(code: String): CountryConfig? =
        _countries.value.find { it.code.equals(code, ignoreCase = true) }
            ?: SupportedCountries.getByCode(code)

    override fun setSelectedCountry(country: CountryConfig) {
        _selectedCountry.value = country
    }

    override fun getCurrentCountry(): CountryConfig = _selectedCountry.value

    override fun getCachedCountries(): List<CountryConfig> = _countries.value

    fun getPrimaryMarkets(): List<CountryConfig> =
        _countries.value.filter { it.isPrimaryMarket }.sortedBy { it.launchPriority }

    fun getUssdSupportedCountries(): List<CountryConfig> =
        _countries.value.filter { it.hasUssdSupport }

    fun getCountryListItems(): List<CountryListItem> = _countries.value.map {
        CountryListItem(it.code, it.name, it.flagEmoji, it.providerName, it.currency, it.phonePrefix, it.hasUssdSupport)
    }

    fun searchCountries(query: String): List<CountryConfig> {
        val q = query.lowercase()
        return _countries.value.filter {
            it.name.lowercase().contains(q) || it.code.lowercase() == q ||
            it.nameLocal?.lowercase()?.contains(q) == true || it.providerName.lowercase().contains(q)
        }
    }
}
