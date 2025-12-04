package com.momoterminal.core.common

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.momoterminal.core.common.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app localization and language switching.
 * Supports automatic detection based on country selection.
 */
@Singleton
class LocaleManager @Inject constructor(
    private val userPreferences: UserPreferences
) {
    companion object {
        const val LANG_ENGLISH = "en"
        const val LANG_FRENCH = "fr"
        const val LANG_SWAHILI = "sw"
        const val LANG_KINYARWANDA = "rw"
        const val LANG_PORTUGUESE = "pt"
        const val LANG_ARABIC = "ar"
        const val LANG_SPANISH = "es"

        val SUPPORTED_LANGUAGES = mapOf(
            LANG_ENGLISH to "English",
            LANG_FRENCH to "Français",
            LANG_SWAHILI to "Kiswahili",
            LANG_KINYARWANDA to "Ikinyarwanda",
            LANG_PORTUGUESE to "Português",
            LANG_ARABIC to "العربية",
            LANG_SPANISH to "Español"
        )

        private val COUNTRY_LANGUAGE_MAP = mapOf(
            // French-speaking countries
            "BI" to LANG_FRENCH,  // Burundi
            "CM" to LANG_FRENCH,  // Cameroon
            "CF" to LANG_FRENCH,  // Central African Republic
            "TD" to LANG_FRENCH,  // Chad
            "KM" to LANG_FRENCH,  // Comoros
            "CG" to LANG_FRENCH,  // Congo
            "CD" to LANG_FRENCH,  // DR Congo
            "DJ" to LANG_FRENCH,  // Djibouti
            "GA" to LANG_FRENCH,  // Gabon
            "GN" to LANG_FRENCH,  // Guinea
            "CI" to LANG_FRENCH,  // Côte d'Ivoire
            "MG" to LANG_FRENCH,  // Madagascar
            "ML" to LANG_FRENCH,  // Mali
            "MR" to LANG_FRENCH,  // Mauritania
            "NE" to LANG_FRENCH,  // Niger
            "SN" to LANG_FRENCH,  // Senegal
            "TG" to LANG_FRENCH,  // Togo
            "BF" to LANG_FRENCH,  // Burkina Faso
            "BJ" to LANG_FRENCH,  // Benin
            // English-speaking countries
            "GH" to LANG_ENGLISH, // Ghana
            "MW" to LANG_ENGLISH, // Malawi
            "MU" to LANG_ENGLISH, // Mauritius
            "NA" to LANG_ENGLISH, // Namibia
            "SC" to LANG_ENGLISH, // Seychelles
            "ZM" to LANG_ENGLISH, // Zambia
            "ZW" to LANG_ENGLISH, // Zimbabwe
            // Swahili-speaking
            "TZ" to LANG_SWAHILI, // Tanzania
            // Kinyarwanda
            "RW" to LANG_KINYARWANDA, // Rwanda
            // Spanish-speaking
            "GQ" to LANG_SPANISH, // Equatorial Guinea
            // Portuguese-speaking
            "MZ" to LANG_PORTUGUESE // Mozambique
        )
    }

    fun getCurrentLanguage(): String {
        return runBlocking {
            userPreferences.languageFlow.first().ifEmpty {
                Locale.getDefault().language
            }
        }
    }

    suspend fun setLanguage(languageCode: String) {
        userPreferences.setLanguage(languageCode)
        applyLanguage(languageCode)
    }

    fun applyLanguage(languageCode: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getRecommendedLanguage(countryCode: String): String {
        return COUNTRY_LANGUAGE_MAP[countryCode.uppercase()] ?: LANG_ENGLISH
    }

    fun shouldSuggestLanguageChange(
        currentLanguage: String,
        selectedCountryCode: String
    ): Boolean {
        val recommendedLanguage = getRecommendedLanguage(selectedCountryCode)
        return currentLanguage != recommendedLanguage &&
               SUPPORTED_LANGUAGES.containsKey(recommendedLanguage)
    }

    fun getLocaleForCountry(countryCode: String): Locale {
        val language = getRecommendedLanguage(countryCode)
        return Locale(language, countryCode)
    }

    fun wrapContext(context: Context): Context {
        val languageCode = getCurrentLanguage()
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        return context.createConfigurationContext(config)
    }

    fun getSupportedLanguagesList(): List<LanguageOption> {
        return SUPPORTED_LANGUAGES.map { (code, name) ->
            LanguageOption(code, name, code == getCurrentLanguage())
        }
    }

    data class LanguageOption(
        val code: String,
        val displayName: String,
        val isSelected: Boolean
    )
}
