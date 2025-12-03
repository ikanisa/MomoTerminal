package com.momoterminal.presentation.screens.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.config.AppConfig
import com.momoterminal.config.SupportedCountries
import com.momoterminal.auth.AuthRepository
import com.momoterminal.auth.SessionManager
import com.momoterminal.data.model.CountryConfig
import com.momoterminal.data.preferences.UserPreferences
import com.momoterminal.data.repository.CountryRepository
import com.momoterminal.security.BiometricHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Supports separate profile country (from WhatsApp registration) and 
 * mobile money country (for transactions).
 */
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
    private val biometricHelper: BiometricHelper,
    private val countryRepository: CountryRepository
) : ViewModel() {
    
    /**
     * UI state for the Settings screen.
     */
    data class SettingsUiState(
        // Profile info (from WhatsApp registration - read only)
        val authPhone: String = "",
        val profileCountryCode: String = "RW",
        val profileCountryName: String = "Rwanda",
        // Mobile Money configuration (user can change country)
        val momoCountryCode: String = "RW",
        val momoCountryName: String = "Rwanda",
        val momoCurrency: String = "RWF",
        val momoProviderName: String = "MTN MoMo",
        val merchantPhone: String = "",
        // Legacy compatibility
        val countryCode: String = "RW",
        // Other settings
        val isConfigured: Boolean = false,
        val isBiometricEnabled: Boolean = false,
        val isBiometricAvailable: Boolean = false,

    data class PermissionState(
        val smsGranted: Boolean = false,
        val cameraGranted: Boolean = false,
        val notificationsGranted: Boolean = false,
        val nfcEnabled: Boolean = false,
        val nfcAvailable: Boolean = false,
        val keepScreenOnEnabled: Boolean = false,
        val vibrationEnabled: Boolean = true,
        val batteryOptimizationIgnored: Boolean = false
    )

    data class SettingsUiState(
        val userName: String = "",
        val whatsappNumber: String = "",
        val profileCountryCode: String = "RW",
        val profileCountryName: String = "Rwanda",
        val momoCountryCode: String = "RW",
        val momoPhonePlaceholder: String = "78XXXXXXX",
        val momoProviderName: String = "MTN MoMo",
        // MoMo identifier can be phone number or code
        val useMomoCode: Boolean = false,
        val momoIdentifier: String = "",
        val isMomoIdentifierValid: Boolean = true,
        val availableCountries: List<CountryConfig> = emptyList(),
        val isBiometricEnabled: Boolean = false,
        val isBiometricAvailable: Boolean = false,
        val smsAutoSyncEnabled: Boolean = true,
        val appVersion: String = "1.0.0",
        val isConfigured: Boolean = false,
        val showSaveSuccess: Boolean = false,
        val showLogoutDialog: Boolean = false,
        val permissions: PermissionState = PermissionState()
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    

    init {
        loadSettings()
        loadCountries()
        checkBiometricAvailability()
        loadAppVersion()
        refreshPermissionStates()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            val biometricEnabled = userPreferences.biometricEnabledFlow.first()
            
            val profileCountryCode = appConfig.getProfileCountryCode()
            val profileCountry = SupportedCountries.getByCode(profileCountryCode)
            
            val momoCountryCode = appConfig.getMomoCountryCode().ifBlank { profileCountryCode }
            val momoCountry = SupportedCountries.getByCode(momoCountryCode)
            val providerName = appConfig.getMomoProvider()
            
            _uiState.value = _uiState.value.copy(
                authPhone = appConfig.getAuthPhone(),
                profileCountryCode = profileCountryCode,
                profileCountryName = profileCountry?.name ?: "Rwanda",
                momoCountryCode = momoCountryCode,
                momoCountryName = momoCountry?.name ?: "Rwanda",
                momoCurrency = momoCountry?.currency ?: "RWF",
                momoProviderName = getProviderDisplayName(providerName),
                merchantPhone = appConfig.getMerchantPhone(),
                countryCode = momoCountryCode,
                isConfigured = appConfig.isConfigured(),
                isBiometricAvailable = biometricHelper.isBiometricAvailable(),
                isBiometricEnabled = biometricEnabled
            )
        }
    }
    
    private fun getProviderDisplayName(providerCode: String): String {
        return when (providerCode.uppercase()) {
            "MTN" -> "MTN MoMo"
            "AIRTEL" -> "Airtel Money"
            "VODACOM" -> "M-Pesa"
            "VODAFONE" -> "Vodafone Cash"
            "ORANGE" -> "Orange Money"
            "TIGO" -> "Tigo Pesa"
            "WAVE" -> "Wave"
            "MOOV" -> "Moov Money"
            "ECOCASH" -> "EcoCash"
            "TMONEY" -> "T-Money"
            "MVOLA" -> "MVola"
            else -> providerCode
        }
    }
    

    fun refreshPermissionStates() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        
        val keepScreenOn = runBlocking { userPreferences.keepScreenOnEnabledFlow.first() }
        val vibration = runBlocking { userPreferences.vibrationEnabledFlow.first() }
        
        val permissions = PermissionState(
            smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED,
            cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
            notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true,
            nfcAvailable = nfcAdapter != null,
            nfcEnabled = nfcAdapter?.isEnabled == true,
            keepScreenOnEnabled = keepScreenOn,
            vibrationEnabled = vibration,
            batteryOptimizationIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        )
        
        _uiState.update { it.copy(permissions = permissions) }
    }

    private fun loadAppVersion() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            _uiState.update { it.copy(appVersion = packageInfo.versionName ?: "1.0.0") }
        } catch (e: Exception) {
            _uiState.update { it.copy(appVersion = "1.0.0") }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            userPreferences.userPreferencesFlow.collect { prefs ->
                val profileCountry = countryRepository.getByCode(prefs.countryCode) ?: CountryConfig.DEFAULT
                val momoCountry = countryRepository.getByCode(prefs.momoCountryCode.ifEmpty { prefs.countryCode }) ?: profileCountry
                
                _uiState.update {
                    it.copy(
                        userName = prefs.merchantName,
                        whatsappNumber = formatPhoneDisplay(prefs.phoneNumber, profileCountry.phonePrefix),
                        profileCountryCode = prefs.countryCode.ifEmpty { "RW" },
                        profileCountryName = profileCountry.name,
                        momoCountryCode = prefs.momoCountryCode.ifEmpty { prefs.countryCode },
                        momoIdentifier = prefs.merchantPhone,
                        useMomoCode = prefs.useMomoCode,
                        momoPhonePlaceholder = "X".repeat(momoCountry.phoneLength),
                        momoProviderName = momoCountry.providerName,
                        isBiometricEnabled = prefs.biometricEnabled,
                        isConfigured = prefs.merchantPhone.isNotBlank(),
                        isMomoIdentifierValid = prefs.merchantPhone.isBlank() || validateMomoIdentifier(prefs.merchantPhone, prefs.useMomoCode, momoCountry)
                    )
                }
            }
        }
        
        viewModelScope.launch {
            userPreferences.smsAutoSyncEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(smsAutoSyncEnabled = enabled) }
            }
        }
    }
    
    fun updateMerchantPhone(phone: String) {
        _uiState.value = _uiState.value.copy(merchantPhone = phone)
    }

    fun updateMomoCountryCode(code: String) {
        val country = SupportedCountries.getByCode(code)
        
        _uiState.value = _uiState.value.copy(
            momoCountryCode = code,
            momoCountryName = country?.name ?: "Rwanda",
            momoCurrency = country?.currency ?: "RWF",
            momoProviderName = country?.providerDisplayName ?: "MTN MoMo",
            countryCode = code
        )
        appConfig.saveMomoCountryCode(code)
    }
    
    // Legacy method for compatibility
    fun updateCountryCode(code: String) {
        updateMomoCountryCode(code)

    private fun validateMomoIdentifier(identifier: String, isCode: Boolean, country: CountryConfig?): Boolean {
        return if (isCode) {
            identifier.length in 4..10 && identifier.all { it.isDigit() }
        } else {
            country?.isValidPhoneLength(identifier) ?: true
        }
    }

    private fun loadCountries() {
        viewModelScope.launch {
            countryRepository.fetchCountries()
            countryRepository.countries.collect { countries ->
                _uiState.update { it.copy(availableCountries = countries) }
            }
        }
    }

    private fun checkBiometricAvailability() {
        _uiState.update { it.copy(isBiometricAvailable = biometricHelper.isBiometricAvailable()) }
    }

    fun setUseMomoCode(useCode: Boolean) {
        _uiState.update { 
            it.copy(
                useMomoCode = useCode,
                momoIdentifier = "",
                isMomoIdentifierValid = true
            )
        }
    }

    fun updateMomoCountry(countryCode: String) {
        val country = countryRepository.getByCode(countryCode) ?: return
        _uiState.update {
            it.copy(
                momoCountryCode = countryCode,
                momoPhonePlaceholder = "X".repeat(country.phoneLength),
                momoProviderName = country.providerName,
                isMomoIdentifierValid = it.momoIdentifier.isBlank() || validateMomoIdentifier(it.momoIdentifier, it.useMomoCode, country)
            )
        }
    }

    fun updateMomoIdentifier(value: String) {
        val cleaned = value.filter { it.isDigit() }
        val country = countryRepository.getByCode(_uiState.value.momoCountryCode)
        val isCode = _uiState.value.useMomoCode
        _uiState.update {
            it.copy(
                momoIdentifier = cleaned,
                isMomoIdentifierValid = cleaned.isBlank() || validateMomoIdentifier(cleaned, isCode, country)
            )
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        if (enabled && !_uiState.value.isBiometricAvailable) return
        _uiState.update { it.copy(isBiometricEnabled = enabled) }
    }
    
    fun saveSettings(): Boolean {
        val state = _uiState.value
        
        // Validate phone
        if (state.merchantPhone.isBlank()) return false
        
        // Save mobile money configuration
        appConfig.saveMerchantConfig(
            merchantCode = state.merchantPhone,
            mobileMoneyNumber = state.merchantPhone,
            momoCountryCode = state.momoCountryCode
        )
        
        _uiState.value = _uiState.value.copy(
            isConfigured = true,
            showSaveSuccess = true
        )
        
        // Hide success message after delay

    fun toggleKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setKeepScreenOnEnabled(enabled)
            _uiState.update { it.copy(permissions = it.permissions.copy(keepScreenOnEnabled = enabled)) }
        }
    }
    
    fun isPhoneValid(): Boolean {
        return _uiState.value.merchantPhone.isNotBlank()
    }
    

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setVibrationEnabled(enabled)
            _uiState.update { it.copy(permissions = it.permissions.copy(vibrationEnabled = enabled)) }
        }
    }

    fun toggleSmsAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setSmsAutoSyncEnabled(enabled)
            _uiState.update { it.copy(smsAutoSyncEnabled = enabled) }
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            userPreferences.updateMomoConfig(
                momoCountryCode = state.momoCountryCode,
                momoIdentifier = state.momoIdentifier,
                useMomoCode = state.useMomoCode
            )
            userPreferences.updateBiometricEnabled(state.isBiometricEnabled)
            
            _uiState.update { it.copy(showSaveSuccess = true, isConfigured = state.momoIdentifier.isNotBlank()) }
            kotlinx.coroutines.delay(100)
            _uiState.update { it.copy(showSaveSuccess = false) }
        }
    }

    fun showLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun hideLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    fun logout() {
        viewModelScope.launch {
            // Clear user preferences
            userPreferences.clearAll()
            // App config should also be cleared
            appConfig.clearConfig()
            // Navigation will be handled by the screen
        }
        authRepository.logout()
    }

    fun isPhoneValid(): Boolean = _uiState.value.isMomoPhoneValid

    private fun formatPhoneDisplay(phone: String, prefix: String): String {
        return if (phone.isNotBlank()) "$prefix $phone" else ""
    }
}
