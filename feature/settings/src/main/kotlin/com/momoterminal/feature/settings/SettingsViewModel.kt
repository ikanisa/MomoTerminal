package com.momoterminal.feature.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.feature.auth.AuthRepository
import com.momoterminal.core.common.model.CountryConfig
import com.momoterminal.data.preferences.UserPreferences
import com.momoterminal.core.data.repository.CountryRepository
import com.momoterminal.security.BiometricHelper
import com.momoterminal.core.common.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
    private val biometricHelper: BiometricHelper,
    private val countryRepository: CountryRepository,
    private val localeManager: LocaleManager
) : ViewModel() {

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
        val authPhone: String = "",
        val profileCountryCode: String = "RW",
        val profileCountryName: String = "Rwanda",
        val momoCountryCode: String = "RW",
        val momoCountryName: String = "Rwanda",
        val momoCurrency: String = "RWF",
        val momoPhonePlaceholder: String = "78XXXXXXX",
        val momoProviderName: String = "MTN MoMo",
        val useMomoCode: Boolean = false,
        val momoIdentifier: String = "",
        val merchantPhone: String = "",
        val isMomoIdentifierValid: Boolean = true,
        val isMomoPhoneValid: Boolean = true,
        val availableCountries: List<CountryConfig> = emptyList(),
        val isBiometricEnabled: Boolean = false,
        val isBiometricAvailable: Boolean = false,
        val smsAutoSyncEnabled: Boolean = true,
        val appVersion: String = "1.0.0",
        val isConfigured: Boolean = false,
        val showSaveSuccess: Boolean = false,
        val showLogoutDialog: Boolean = false,
        val permissions: PermissionState = PermissionState(),
        val currentLanguage: String = "en"
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadCountries()
        checkBiometricAvailability()
        loadAppVersion()
        refreshPermissionStates()
        loadLanguage()
    }

    private fun loadLanguage() {
        viewModelScope.launch {
            userPreferences.languageFlow.collect { lang ->
                _uiState.update { it.copy(currentLanguage = lang.ifEmpty { "en" }) }
            }
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            localeManager.setLanguage(languageCode)
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
                        authPhone = prefs.phoneNumber,
                        whatsappNumber = formatPhoneDisplay(prefs.phoneNumber, profileCountry.phonePrefix),
                        profileCountryCode = prefs.countryCode.ifEmpty { "RW" },
                        profileCountryName = profileCountry.name,
                        momoCountryCode = prefs.momoCountryCode.ifEmpty { prefs.countryCode },
                        momoCountryName = momoCountry.name,
                        momoCurrency = momoCountry.currency,
                        momoIdentifier = prefs.merchantPhone,
                        merchantPhone = prefs.merchantPhone,
                        useMomoCode = prefs.useMomoCode,
                        momoPhonePlaceholder = "X".repeat(momoCountry.phoneLength),
                        momoProviderName = momoCountry.providerName,
                        isBiometricEnabled = prefs.biometricEnabled,
                        isConfigured = prefs.merchantPhone.isNotBlank(),
                        isMomoIdentifierValid = prefs.merchantPhone.isBlank() || validateMomoIdentifier(prefs.merchantPhone, prefs.useMomoCode, momoCountry),
                        isMomoPhoneValid = prefs.merchantPhone.isBlank() || validateMomoIdentifier(prefs.merchantPhone, prefs.useMomoCode, momoCountry)
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
        val isValid = _uiState.value.momoIdentifier.isBlank() || validateMomoIdentifier(_uiState.value.momoIdentifier, _uiState.value.useMomoCode, country)
        _uiState.update {
            it.copy(
                momoCountryCode = countryCode,
                momoCountryName = country.name,
                momoCurrency = country.currency,
                momoPhonePlaceholder = "X".repeat(country.phoneLength),
                momoProviderName = country.providerName,
                isMomoIdentifierValid = isValid,
                isMomoPhoneValid = isValid
            )
        }
    }

    fun updateMomoIdentifier(value: String) {
        val cleaned = value.filter { it.isDigit() }
        val country = countryRepository.getByCode(_uiState.value.momoCountryCode)
        val isCode = _uiState.value.useMomoCode
        val isValid = cleaned.isBlank() || validateMomoIdentifier(cleaned, isCode, country)
        _uiState.update {
            it.copy(
                momoIdentifier = cleaned,
                merchantPhone = cleaned,
                isMomoIdentifierValid = isValid,
                isMomoPhoneValid = isValid
            )
        }
    }

    // Alias for backward compatibility
    fun updateMerchantPhone(value: String) = updateMomoIdentifier(value)
    
    // Alias for backward compatibility
    fun updateMomoCountryCode(code: String) = updateMomoCountry(code)

    fun toggleBiometric(enabled: Boolean) {
        if (enabled && !_uiState.value.isBiometricAvailable) return
        _uiState.update { it.copy(isBiometricEnabled = enabled) }
    }

    fun toggleKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setKeepScreenOnEnabled(enabled)
            _uiState.update { it.copy(permissions = it.permissions.copy(keepScreenOnEnabled = enabled)) }
        }
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
            userPreferences.clearAll()
        }
        authRepository.logout()
    }

    fun isPhoneValid(): Boolean = _uiState.value.isMomoIdentifierValid && _uiState.value.momoIdentifier.isNotBlank()

    private fun formatPhoneDisplay(phone: String, prefix: String): String {
        return if (phone.isNotBlank()) "$prefix $phone" else ""
    }
}
