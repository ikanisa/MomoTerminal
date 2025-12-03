package com.momoterminal.core.i18n.api

import com.momoterminal.core.i18n.locale.AppLocale
import com.momoterminal.core.i18n.locale.LocalePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

// 1. LOCALE INTERCEPTOR (Add Accept-Language header)

@Singleton
class LocaleInterceptor @Inject constructor(
    private val localePreferences: LocalePreferences
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val locale = runBlocking {
            localePreferences.currentLocale.first()
        }
        
        val request = chain.request().newBuilder()
            .addHeader("Accept-Language", locale.toAcceptLanguageHeader())
            .addHeader("X-User-Locale", locale.localeTag)
            .build()
        
        return chain.proceed(request)
    }
    
    private fun AppLocale.toAcceptLanguageHeader(): String {
        return if (regionCode != null) {
            "$languageCode-$regionCode,$languageCode;q=0.9,en;q=0.8"
        } else {
            "$languageCode,en;q=0.8"
        }
    }
}

// 2. USER PROFILE WITH LOCALE

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val locale: UserLocale? = null,
    val preferences: UserPreferences = UserPreferences()
)

data class UserLocale(
    val languageCode: String,      // "en", "es", "fr"
    val regionCode: String? = null, // "US", "MX", "FR"
    val timezone: String? = null    // "America/New_York", "Europe/Paris"
)

data class UserPreferences(
    val dateFormat: String = "default",        // "default", "iso", "custom"
    val numberFormat: String = "default",      // "default", "compact"
    val currencySymbol: String = "¤",          // Generic symbol
    val firstDayOfWeek: Int = 1,              // 1 = Monday, 0 = Sunday
    val use24HourFormat: Boolean = true
)

// 3. API MODELS

// Request: Update user locale
data class UpdateLocaleRequest(
    val languageCode: String,
    val regionCode: String?,
    val timezone: String?
)

// Response: Localized content
data class LocalizedContent(
    val locale: String,
    val content: Map<String, String> // key -> localized value
)

// 4. API SERVICE

interface UserApiService {
    @PUT("users/me/locale")
    suspend fun updateLocale(@Body request: UpdateLocaleRequest): UserProfile
    
    @GET("content/{key}")
    suspend fun getLocalizedContent(
        @Path("key") key: String,
        @Header("Accept-Language") language: String
    ): LocalizedContent
}

// 5. REPOSITORY WITH LOCALE SYNC

@Singleton
class UserRepository @Inject constructor(
    private val api: UserApiService,
    private val localePreferences: LocalePreferences
) {
    
    suspend fun updateUserLocale(locale: AppLocale) {
        // 1. Save locally
        localePreferences.setLocale(locale)
        
        // 2. Sync to backend
        try {
            api.updateLocale(
                UpdateLocaleRequest(
                    languageCode = locale.languageCode,
                    regionCode = locale.regionCode,
                    timezone = TimeZone.getDefault().id
                )
            )
        } catch (e: Exception) {
            // Queue for later sync if offline
        }
    }
    
    suspend fun getLocalizedContent(key: String): String {
        val locale = localePreferences.currentLocale.first()
        val content = api.getLocalizedContent(
            key = key,
            language = locale.toAcceptLanguageHeader()
        )
        return content.content[key] ?: ""
    }
}

// 6. AI/LLM INTEGRATION

data class AiRequest(
    val prompt: String,
    val context: AiContext
)

data class AiContext(
    val languageCode: String,      // User's language
    val regionCode: String?,       // User's region
    val timezone: String,          // User's timezone
    val formatPreferences: FormatPreferences
)

data class FormatPreferences(
    val dateFormat: String,
    val numberFormat: String,
    val currencySymbol: String,
    val distanceUnit: String       // "metric" or "imperial"
)

@Singleton
class AiService @Inject constructor(
    private val localePreferences: LocalePreferences,
    private val api: AiApiService
) {
    
    suspend fun generateResponse(prompt: String): String {
        val locale = localePreferences.currentLocale.first()
        
        val request = AiRequest(
            prompt = prompt,
            context = AiContext(
                languageCode = locale.languageCode,
                regionCode = locale.regionCode,
                timezone = TimeZone.getDefault().id,
                formatPreferences = FormatPreferences(
                    dateFormat = "default",
                    numberFormat = "default",
                    currencySymbol = "¤",
                    distanceUnit = if (locale.regionCode == "US") "imperial" else "metric"
                )
            )
        )
        
        return api.generateResponse(request)
    }
}

// 7. EXAMPLE: AI PROMPT WITH LOCALE

suspend fun generateLocalizedAiResponse(
    userPrompt: String,
    locale: AppLocale
): String {
    val systemPrompt = """
        You are a helpful assistant.
        User language: ${locale.languageCode}
        User region: ${locale.regionCode ?: "unknown"}
        
        Instructions:
        - Respond in ${locale.displayName}
        - Use appropriate date format for ${locale.regionCode ?: locale.languageCode}
        - Use appropriate number format
        - Be culturally appropriate for the region
        
        User prompt: $userPrompt
    """.trimIndent()
    
    return aiService.generate(systemPrompt)
}

// 8. NETWORK MODULE INTEGRATION

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        localeInterceptor: LocaleInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(localeInterceptor) // Add locale headers
            .build()
    }
}

// 9. USAGE IN VIEWMODEL

@HiltViewModel
class ContentViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val localePreferences: LocalePreferences,
    private val dateFormatter: DateFormatter,
    private val numberFormatter: NumberFormatter
) : ViewModel() {
    
    private val locale = localePreferences.currentLocale
        .stateIn(viewModelScope, SharingStarted.Eagerly, SupportedLocales.ENGLISH)
    
    fun formatDate(timestamp: Long): String {
        return dateFormatter.formatDate(timestamp, locale.value.toLocale())
    }
    
    fun formatNumber(number: Double): String {
        return numberFormatter.formatNumber(number, locale.value.toLocale())
    }
}

// 10. COMPLETE EXAMPLE: SETTINGS SCREEN

@Composable
fun LanguageSettingsScreen(
    viewModel: LanguageSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(uiState.localeChanged) {
        if (uiState.localeChanged) {
            // Recreate activity to apply new locale
            (context as? ComponentActivity)?.recreate()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_language_title)) }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Text(
                    text = stringResource(R.string.settings_language_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            items(SupportedLocales.ALL) { locale ->
                LanguageItem(
                    locale = locale,
                    isSelected = locale == uiState.currentLocale,
                    onClick = {
                        viewModel.onEvent(LanguageSettingsEvent.SelectLocale(locale))
                    }
                )
            }
        }
    }
}

@HiltViewModel
class LanguageSettingsViewModel @Inject constructor(
    private val localePreferences: LocalePreferences,
    private val userRepository: UserRepository
) : BaseViewModel<LanguageSettingsUiState, LanguageSettingsEvent, LanguageSettingsEffect>(
    initialState = LanguageSettingsUiState()
) {
    
    init {
        viewModelScope.launch {
            localePreferences.currentLocale.collect { locale ->
                updateState { copy(currentLocale = locale) }
            }
        }
    }
    
    override fun onEvent(event: LanguageSettingsEvent) {
        when (event) {
            is LanguageSettingsEvent.SelectLocale -> selectLocale(event.locale)
        }
    }
    
    private fun selectLocale(locale: AppLocale) {
        viewModelScope.launch {
            // Save locally
            localePreferences.setLocale(locale)
            
            // Sync to backend
            userRepository.updateUserLocale(locale)
            
            // Trigger UI update
            updateState { copy(localeChanged = true) }
        }
    }
}
