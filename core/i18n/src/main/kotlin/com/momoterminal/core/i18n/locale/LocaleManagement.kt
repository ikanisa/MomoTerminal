package com.momoterminal.core.i18n.locale

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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

// 1. SUPPORTED LOCALES (Domain-Agnostic)

data class AppLocale(
    val languageCode: String,      // ISO 639-1
    val regionCode: String? = null, // ISO 3166-1
    val displayName: String,
    val nativeName: String,
    val isRtl: Boolean = false
) {
    val localeTag: String
        get() = if (regionCode != null) "${languageCode}_$regionCode" else languageCode
    
    fun toLocale(): Locale = if (regionCode != null) {
        Locale(languageCode, regionCode)
    } else {
        Locale(languageCode)
    }
}

object SupportedLocales {
    val ENGLISH = AppLocale("en", null, "English", "English")
    val SPANISH = AppLocale("es", null, "Spanish", "Español")
    val SPANISH_MX = AppLocale("es", "MX", "Spanish (Mexico)", "Español (México)")
    val SPANISH_ES = AppLocale("es", "ES", "Spanish (Spain)", "Español (España)")
    val FRENCH = AppLocale("fr", null, "French", "Français")
    val GERMAN = AppLocale("de", null, "German", "Deutsch")
    val ITALIAN = AppLocale("it", null, "Italian", "Italiano")
    val PORTUGUESE = AppLocale("pt", null, "Portuguese", "Português")
    val PORTUGUESE_BR = AppLocale("pt", "BR", "Portuguese (Brazil)", "Português (Brasil)")
    val CHINESE = AppLocale("zh", null, "Chinese", "中文")
    val JAPANESE = AppLocale("ja", null, "Japanese", "日本語")
    val KOREAN = AppLocale("ko", null, "Korean", "한국어")
    val ARABIC = AppLocale("ar", null, "Arabic", "العربية", isRtl = true)
    val HEBREW = AppLocale("he", null, "Hebrew", "עברית", isRtl = true)
    val HINDI = AppLocale("hi", null, "Hindi", "हिन्दी")
    val RUSSIAN = AppLocale("ru", null, "Russian", "Русский")
    
    val ALL = listOf(
        ENGLISH, SPANISH, SPANISH_MX, SPANISH_ES, FRENCH, GERMAN,
        ITALIAN, PORTUGUESE, PORTUGUESE_BR, CHINESE, JAPANESE,
        KOREAN, ARABIC, HEBREW, HINDI, RUSSIAN
    )
    
    fun fromLocaleTag(tag: String): AppLocale? {
        return ALL.find { it.localeTag == tag }
    }
    
    fun fromLanguageCode(code: String): AppLocale? {
        return ALL.find { it.languageCode == code }
    }
}

// 2. LOCALE PREFERENCES (DataStore)

private val Context.localeDataStore: DataStore<Preferences> by preferencesDataStore("locale_prefs")

@Singleton
class LocalePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val LANGUAGE_CODE = stringPreferencesKey("language_code")
    private val REGION_CODE = stringPreferencesKey("region_code")
    
    val currentLocale: Flow<AppLocale> = context.localeDataStore.data.map { prefs ->
        val languageCode = prefs[LANGUAGE_CODE] ?: "en"
        val regionCode = prefs[REGION_CODE]
        
        SupportedLocales.ALL.find {
            it.languageCode == languageCode && it.regionCode == regionCode
        } ?: SupportedLocales.ENGLISH
    }
    
    suspend fun setLocale(locale: AppLocale) {
        context.localeDataStore.edit { prefs ->
            prefs[LANGUAGE_CODE] = locale.languageCode
            if (locale.regionCode != null) {
                prefs[REGION_CODE] = locale.regionCode
            } else {
                prefs.remove(REGION_CODE)
            }
        }
    }
    
    suspend fun clear() {
        context.localeDataStore.edit { it.clear() }
    }
}

// 3. LOCALE MANAGER (Apply at Runtime)

@Singleton
class LocaleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localePreferences: LocalePreferences
) {
    
    fun applyLocale(locale: AppLocale): Context {
        val newLocale = locale.toLocale()
        Locale.setDefault(newLocale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(newLocale)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(newLocale)
        }
        
        return context.createConfigurationContext(config)
    }
    
    fun getCurrentLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }
    
    fun isRtl(): Boolean {
        return context.resources.configuration.layoutDirection == 
            android.view.View.LAYOUT_DIRECTION_RTL
    }
}

// 4. COMPOSE INTEGRATION

val LocalAppLocale = compositionLocalOf { SupportedLocales.ENGLISH }

@Composable
fun ProvideAppLocale(
    localePreferences: LocalePreferences,
    content: @Composable () -> Unit
) {
    val locale by localePreferences.currentLocale.collectAsState(initial = SupportedLocales.ENGLISH)
    
    CompositionLocalProvider(LocalAppLocale provides locale) {
        content()
    }
}

@Composable
fun rememberAppLocale(): AppLocale {
    return LocalAppLocale.current
}

// 5. LANGUAGE PICKER UI

@Composable
fun LanguagePickerScreen(
    viewModel: LanguagePickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_language_title)) },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding)
        ) {
            items(SupportedLocales.ALL) { locale ->
                LanguageItem(
                    locale = locale,
                    isSelected = locale == uiState.currentLocale,
                    onClick = { viewModel.onEvent(LanguagePickerEvent.SelectLocale(locale)) }
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(
    locale: AppLocale,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(locale.nativeName) },
        supportingContent = { Text(locale.displayName) },
        trailingContent = {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// 6. VIEWMODEL

data class LanguagePickerUiState(
    val currentLocale: AppLocale = SupportedLocales.ENGLISH,
    val availableLocales: List<AppLocale> = SupportedLocales.ALL
) : UiState

sealed interface LanguagePickerEvent : UiEvent {
    data class SelectLocale(val locale: AppLocale) : LanguagePickerEvent
}

sealed interface LanguagePickerEffect : UiEffect {
    data object LocaleChanged : LanguagePickerEffect
}

@HiltViewModel
class LanguagePickerViewModel @Inject constructor(
    private val localePreferences: LocalePreferences,
    private val localeManager: LocaleManager
) : BaseViewModel<LanguagePickerUiState, LanguagePickerEvent, LanguagePickerEffect>(
    initialState = LanguagePickerUiState()
) {
    
    init {
        viewModelScope.launch {
            localePreferences.currentLocale.collect { locale ->
                updateState { copy(currentLocale = locale) }
            }
        }
    }
    
    override fun onEvent(event: LanguagePickerEvent) {
        when (event) {
            is LanguagePickerEvent.SelectLocale -> selectLocale(event.locale)
        }
    }
    
    private fun selectLocale(locale: AppLocale) {
        viewModelScope.launch {
            localePreferences.setLocale(locale)
            localeManager.applyLocale(locale)
            sendEffect(LanguagePickerEffect.LocaleChanged)
        }
    }
}

// 7. ACTIVITY INTEGRATION

abstract class LocaleAwareActivity : ComponentActivity() {
    @Inject lateinit var localeManager: LocaleManager
    @Inject lateinit var localePreferences: LocalePreferences
    
    override fun attachBaseContext(newBase: Context) {
        // Apply saved locale
        val locale = runBlocking {
            localePreferences.currentLocale.first()
        }
        val context = localeManager.applyLocale(locale)
        super.attachBaseContext(context)
    }
}

// Usage in MainActivity
@AndroidEntryPoint
class MainActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ProvideAppLocale(localePreferences) {
                AppTheme {
                    AppNavHost()
                }
            }
        }
    }
}

// 8. STRING RESOURCE HELPER

@Composable
fun stringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    val locale = rememberAppLocale()
    val context = LocalContext.current
    
    return remember(id, locale, *formatArgs) {
        context.getString(id, *formatArgs)
    }
}

@Composable
fun pluralStringResource(
    @PluralsRes id: Int,
    quantity: Int,
    vararg formatArgs: Any
): String {
    val locale = rememberAppLocale()
    val context = LocalContext.current
    
    return remember(id, quantity, locale, *formatArgs) {
        context.resources.getQuantityString(id, quantity, *formatArgs)
    }
}
