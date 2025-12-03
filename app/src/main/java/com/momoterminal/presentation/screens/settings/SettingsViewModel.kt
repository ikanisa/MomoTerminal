package com.momoterminal.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.config.AppConfig
import com.momoterminal.config.SupportedCountries
import com.momoterminal.data.preferences.UserPreferences
import com.momoterminal.security.BiometricHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Supports separate profile country (from WhatsApp registration) and 
 * mobile money country (for transactions).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appConfig: AppConfig,
    private val biometricHelper: BiometricHelper,
    private val userPreferences: UserPreferences,
    private val application: android.app.Application
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
        val showSaveSuccess: Boolean = false,
        val showLogoutDialog: Boolean = false,
        val appVersion: String = ""
    )
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        observeBiometricPreference()
        loadAppVersion()
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
    
    private fun loadAppVersion() {
        try {
            val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            _uiState.value = _uiState.value.copy(
                appVersion = packageInfo.versionName ?: "1.0.0"
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(appVersion = "1.0.0")
        }
    }
    
    private fun observeBiometricPreference() {
        viewModelScope.launch {
            userPreferences.biometricEnabledFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(isBiometricEnabled = enabled)
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
    }

    fun toggleBiometric(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isBiometricEnabled = enabled)
        viewModelScope.launch {
            userPreferences.setBiometricEnabled(enabled)
        }
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
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _uiState.value = _uiState.value.copy(showSaveSuccess = false)
        }
        
        return true
    }
    
    fun isPhoneValid(): Boolean {
        return _uiState.value.merchantPhone.isNotBlank()
    }
    
    fun showLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = true)
    }
    
    fun hideLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = false)
    }
    
    fun logout() {
        viewModelScope.launch {
            // Clear user preferences
            userPreferences.clearAll()
            // App config should also be cleared
            appConfig.clearConfig()
            // Navigation will be handled by the screen
        }
    }
}
