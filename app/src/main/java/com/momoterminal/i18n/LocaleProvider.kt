package com.momoterminal.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.momoterminal.core.common.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

/**
 * Composition local for current app locale.
 */
val LocalAppLocale = compositionLocalOf { Locale.getDefault() }

/**
 * Composition local for currency code based on user's MoMo country.
 */
val LocalCurrency = compositionLocalOf { "RWF" }

/**
 * Composition local for current language code.
 */
val LocalLanguageCode = compositionLocalOf { "en" }

/**
 * Provides locale context to Compose hierarchy.
 */
@Composable
fun LocaleProvider(
    userPreferences: UserPreferences,
    content: @Composable () -> Unit
) {
    val language by userPreferences.languageFlow.collectAsState(initial = "")
    val countryCode by userPreferences.momoCountryFlow.collectAsState(initial = "RW")
    
    val effectiveLanguage = language.ifEmpty { Locale.getDefault().language }
    
    val locale = remember(effectiveLanguage, countryCode) {
        Locale(effectiveLanguage, countryCode)
    }
    
    val currency = remember(countryCode) {
        CurrencyFormatter.getCurrencyForCountry(countryCode)
    }
    
    CompositionLocalProvider(
        LocalAppLocale provides locale,
        LocalCurrency provides currency,
        LocalLanguageCode provides effectiveLanguage
    ) {
        content()
    }
}

/**
 * Get localized context for string resources.
 */
@Composable
fun localizedContext(): Context {
    val context = LocalContext.current
    val locale = LocalAppLocale.current
    
    return remember(locale) {
        val config = context.resources.configuration.apply {
            setLocale(locale)
        }
        context.createConfigurationContext(config)
    }
}

/**
 * Extension to get formatted currency from UserPreferences.
 */
fun UserPreferences.currencyFlow(): Flow<String> = momoCountryFlow.map { 
    CurrencyFormatter.getCurrencyForCountry(it) 
}
