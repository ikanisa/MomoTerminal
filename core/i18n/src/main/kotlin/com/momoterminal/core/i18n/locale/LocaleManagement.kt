package com.momoterminal.core.i18n.locale

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class AppLocale(
    val languageCode: String,
    val regionCode: String? = null,
    val displayName: String,
    val nativeName: String,
    val isRtl: Boolean = false
) {
    val localeTag: String get() = if (regionCode != null) "${languageCode}_$regionCode" else languageCode
    fun toLocale(): Locale = if (regionCode != null) Locale(languageCode, regionCode) else Locale(languageCode)
}

object SupportedLocales {
    val ENGLISH = AppLocale("en", null, "English", "English")
    val FRENCH = AppLocale("fr", null, "French", "Français")
    val ARABIC = AppLocale("ar", null, "Arabic", "العربية", isRtl = true)
    
    val ALL = listOf(ENGLISH, FRENCH, ARABIC)
    
    fun fromLocaleTag(tag: String): AppLocale? = ALL.find { it.localeTag == tag }
}

private val Context.localeDataStore: DataStore<Preferences> by preferencesDataStore("locale_prefs")

@Singleton
class LocalePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val LANGUAGE_CODE = stringPreferencesKey("language_code")
    
    val currentLocale: Flow<AppLocale> = context.localeDataStore.data.map { prefs ->
        val languageCode = prefs[LANGUAGE_CODE] ?: "en"
        SupportedLocales.ALL.find { it.languageCode == languageCode } ?: SupportedLocales.ENGLISH
    }
    
    suspend fun setLocale(locale: AppLocale) {
        context.localeDataStore.edit { prefs ->
            prefs[LANGUAGE_CODE] = locale.languageCode
        }
    }
}

@Singleton
class LocaleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun applyLocale(locale: AppLocale): Context {
        val newLocale = locale.toLocale()
        Locale.setDefault(newLocale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(newLocale)
        return context.createConfigurationContext(config)
    }
    
    fun isRtl(): Boolean = context.resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
}
