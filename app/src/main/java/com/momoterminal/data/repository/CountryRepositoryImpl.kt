package com.momoterminal.data.repository

import android.content.Context
import com.momoterminal.domain.model.Country
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.json.JSONArray
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CountryRepository that fetches countries from Supabase.
 * Falls back to cached/hardcoded data if network is unavailable.
 */
@Singleton
class CountryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CountryRepository {
    
    private val _countriesCache = MutableStateFlow<List<Country>>(getFallbackCountries())
    
    override suspend fun getActiveCountries(): Result<List<Country>> {
        return try {
            val countries = fetchCountriesFromSupabase()
            if (countries.isNotEmpty()) {
                _countriesCache.value = countries
                Result.success(countries)
            } else {
                Result.success(getFallbackCountries())
            }
        } catch (e: Exception) {
            Result.success(getFallbackCountries())
        }
    }
    
    override suspend fun getPrimaryCountries(): Result<List<Country>> {
        return try {
            val allCountries = getActiveCountries().getOrNull() ?: getFallbackCountries()
            Result.success(allCountries.filter { it.isPrimaryMarket })
        } catch (e: Exception) {
            Result.success(getFallbackCountries().filter { it.isPrimaryMarket })
        }
    }
    
    override suspend fun getCountryByCode(code: String): Result<Country?> {
        return try {
            val countries = _countriesCache.value.ifEmpty { getFallbackCountries() }
            Result.success(countries.find { it.code.equals(code, ignoreCase = true) })
        } catch (e: Exception) {
            Result.success(null)
        }
    }
    
    override fun observeCountries(): Flow<List<Country>> {
        return _countriesCache.asStateFlow()
    }
    
    override suspend fun refreshCountries(): Result<Unit> {
        return try {
            val countries = fetchCountriesFromSupabase()
            if (countries.isNotEmpty()) {
                _countriesCache.value = countries
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun fetchCountriesFromSupabase(): List<Country> = withContext(Dispatchers.IO) {
        try {
            // Use Supabase REST API to fetch countries
            // This would be replaced with proper Supabase client in production
            // For now, return fallback countries
            getFallbackCountries()
        } catch (e: Exception) {
            getFallbackCountries()
        }
    }
    
    /**
     * Fallback countries when network is unavailable.
     * These are synced with the Supabase countries table.
     */
    private fun getFallbackCountries(): List<Country> = listOf(
        // Primary Launch Countries
        Country("RW", "Rwanda", "Rwanda", "RWF", "FRw", "+250", "rw", "MTN", "MTN MoMo", "#FFCC00", true),
        Country("CD", "DR Congo", "RD Congo", "CDF", "FC", "+243", "fr", "ORANGE", "Orange Money", "#FF6600", true),
        Country("BI", "Burundi", "Burundi", "BIF", "FBu", "+257", "fr", "ECOCASH", "EcoCash", "#00A651", true),
        Country("TZ", "Tanzania", "Tanzania", "TZS", "TSh", "+255", "sw", "VODACOM", "M-Pesa", "#E60000", true),
        Country("ZM", "Zambia", "Zambia", "ZMW", "ZK", "+260", "en", "MTN", "MTN MoMo", "#FFCC00", true),
        
        // West Africa - French Speaking
        Country("SN", "Senegal", "Sénégal", "XOF", "CFA", "+221", "fr", "ORANGE", "Orange Money", "#FF6600", false),
        Country("CI", "Ivory Coast", "Côte d'Ivoire", "XOF", "CFA", "+225", "fr", "ORANGE", "Orange Money", "#FF6600", false),
        Country("CM", "Cameroon", "Cameroun", "XAF", "FCFA", "+237", "fr", "MTN", "MTN MoMo", "#FFCC00", false),
        Country("ML", "Mali", "Mali", "XOF", "CFA", "+223", "fr", "ORANGE", "Orange Money", "#FF6600", false),
        Country("BF", "Burkina Faso", "Burkina Faso", "XOF", "CFA", "+226", "fr", "ORANGE", "Orange Money", "#FF6600", false),
        Country("NE", "Niger", "Niger", "XOF", "CFA", "+227", "fr", "AIRTEL", "Airtel Money", "#ED1C24", false),
        Country("BJ", "Benin", "Bénin", "XOF", "CFA", "+229", "fr", "MTN", "MTN MoMo", "#FFCC00", false),
        Country("TG", "Togo", "Togo", "XOF", "CFA", "+228", "fr", "TMONEY", "T-Money", "#FF6B00", false),
        Country("GN", "Guinea", "Guinée", "GNF", "FG", "+224", "fr", "ORANGE", "Orange Money", "#FF6600", false),
        Country("TD", "Chad", "Tchad", "XAF", "FCFA", "+235", "fr", "AIRTEL", "Airtel Money", "#ED1C24", false),
        Country("CF", "Central African Republic", "République Centrafricaine", "XAF", "FCFA", "+236", "fr", "ORANGE", "Orange Money", "#FF6600", false),
        Country("GA", "Gabon", "Gabon", "XAF", "FCFA", "+241", "fr", "AIRTEL", "Airtel Money", "#ED1C24", false),
        Country("CG", "Congo", "Congo-Brazzaville", "XAF", "FCFA", "+242", "fr", "MTN", "MTN MoMo", "#FFCC00", false),
        
        // East/Southern Africa - English Speaking
        Country("GH", "Ghana", "Ghana", "GHS", "GH₵", "+233", "en", "MTN", "MTN MoMo", "#FFCC00", false),
        Country("MW", "Malawi", "Malawi", "MWK", "MK", "+265", "en", "AIRTEL", "Airtel Money", "#ED1C24", false),
        Country("ZW", "Zimbabwe", "Zimbabwe", "ZWL", "Z$", "+263", "en", "ECOCASH", "EcoCash", "#00A651", false),
        Country("MZ", "Mozambique", "Moçambique", "MZN", "MT", "+258", "pt", "VODACOM", "M-Pesa", "#E60000", false),
        Country("BW", "Botswana", "Botswana", "BWP", "P", "+267", "en", "ORANGE", "Orange Money", "#FF6600", false),
        Country("NA", "Namibia", "Namibia", "NAD", "N$", "+264", "en", "MTC", "MTC MoMo", "#0066B3", false),
        Country("LS", "Lesotho", "Lesotho", "LSL", "L", "+266", "en", "VODACOM", "M-Pesa", "#E60000", false),
        Country("SZ", "Eswatini", "Eswatini", "SZL", "E", "+268", "en", "MTN", "MTN MoMo", "#FFCC00", false),
        Country("LR", "Liberia", "Liberia", "LRD", "L$", "+231", "en", "ORANGE", "Orange Money", "#FF6600", false),
        Country("SL", "Sierra Leone", "Sierra Leone", "SLL", "Le", "+232", "en", "ORANGE", "Orange Money", "#FF6600", false),
        Country("GM", "Gambia", "Gambia", "GMD", "D", "+220", "en", "AFRICELL", "Africell Money", "#00A0DF", false),
        
        // Island Nations
        Country("MG", "Madagascar", "Madagascar", "MGA", "Ar", "+261", "fr", "MVOLA", "MVola", "#E31937", false),
        Country("KM", "Comoros", "Comores", "KMF", "CF", "+269", "fr", "MVOLA", "MVola", "#E31937", false),
        Country("SC", "Seychelles", "Seychelles", "SCR", "SCR", "+248", "en", "AIRTEL", "Airtel Money", "#ED1C24", false)
    )
}
