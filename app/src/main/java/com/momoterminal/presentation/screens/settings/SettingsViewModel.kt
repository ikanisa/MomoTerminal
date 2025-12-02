package com.momoterminal.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.config.AppConfig
import com.momoterminal.data.preferences.UserPreferences
import com.momoterminal.security.BiometricHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appConfig: AppConfig,
    private val biometricHelper: BiometricHelper,
    private val userPreferences: UserPreferences,
    private val application: android.app.Application
) : ViewModel() {
    
    /**
     * Connection test result.
     */
    sealed class ConnectionTestResult {
        data object Idle : ConnectionTestResult()
        data object Testing : ConnectionTestResult()
        data object Success : ConnectionTestResult()
        data class Failed(val message: String) : ConnectionTestResult()
    }
    
    /**
     * UI state for the Settings screen.
     */
    data class SettingsUiState(
        val webhookUrl: String = "",
        val apiSecret: String = "",
        val merchantPhone: String = "",
        val isConfigured: Boolean = false,
        val isBiometricEnabled: Boolean = false,
        val isBiometricAvailable: Boolean = false,
        val smsAutoSyncEnabled: Boolean = true,
        val connectionTestResult: ConnectionTestResult = ConnectionTestResult.Idle,
        val showSaveSuccess: Boolean = false,
        val showLogoutDialog: Boolean = false,
        val appVersion: String = ""
    )
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    
    init {
        loadSettings()
        observeBiometricPreference()
        loadAppVersion()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            val biometricEnabled = userPreferences.biometricEnabledFlow.first()
            val smsAutoSync = userPreferences.smsAutoSyncEnabledFlow.first()
            _uiState.value = _uiState.value.copy(
                webhookUrl = appConfig.getGatewayUrl(),
                apiSecret = appConfig.getApiSecret(),
                merchantPhone = appConfig.getMerchantPhone(),
                isConfigured = appConfig.isConfigured(),
                isBiometricAvailable = biometricHelper.isBiometricAvailable(),
                isBiometricEnabled = biometricEnabled,
                smsAutoSyncEnabled = smsAutoSync
            )
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
    
    fun updateWebhookUrl(url: String) {
        _uiState.value = _uiState.value.copy(webhookUrl = url)
    }
    
    fun updateApiSecret(secret: String) {
        _uiState.value = _uiState.value.copy(apiSecret = secret)
    }
    
    fun updateMerchantPhone(phone: String) {
        _uiState.value = _uiState.value.copy(merchantPhone = phone)
    }
    
    fun toggleBiometric(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isBiometricEnabled = enabled)
        viewModelScope.launch {
            userPreferences.setBiometricEnabled(enabled)
        }
    }
    
    fun saveSettings(): Boolean {
        val state = _uiState.value
        
        // Validate
        if (state.webhookUrl.isBlank()) return false
        if (!state.webhookUrl.startsWith("http://") && !state.webhookUrl.startsWith("https://")) return false
        if (state.merchantPhone.isBlank()) return false
        
        // Save
        appConfig.saveConfig(
            url = state.webhookUrl,
            secret = state.apiSecret,
            phone = state.merchantPhone
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
    
    fun testConnection() {
        val state = _uiState.value
        
        if (state.webhookUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(
                connectionTestResult = ConnectionTestResult.Failed("URL is required")
            )
            return
        }
        
        _uiState.value = _uiState.value.copy(
            connectionTestResult = ConnectionTestResult.Testing
        )
        
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val json = JSONObject().apply {
                        put("test", true)
                        put("message", "MomoTerminal connection test")
                        put("timestamp", System.currentTimeMillis())
                    }
                    
                    val request = Request.Builder()
                        .url(state.webhookUrl)
                        .post(json.toString().toRequestBody("application/json".toMediaType()))
                        .addHeader("X-Api-Key", state.apiSecret)
                        .addHeader("Content-Type", "application/json")
                        .build()
                    
                    val response = httpClient.newCall(request).execute()
                    val success = response.isSuccessful
                    response.close()
                    
                    if (success) {
                        ConnectionTestResult.Success
                    } else {
                        ConnectionTestResult.Failed("Server returned error")
                    }
                } catch (e: Exception) {
                    ConnectionTestResult.Failed(e.message ?: "Connection failed")
                }
            }
            
            _uiState.value = _uiState.value.copy(connectionTestResult = result)
            
            // Reset after delay
            kotlinx.coroutines.delay(3000)
            _uiState.value = _uiState.value.copy(
                connectionTestResult = ConnectionTestResult.Idle
            )
        }
    }
    
    fun isUrlValid(): Boolean {
        val url = _uiState.value.webhookUrl
        return url.isNotBlank() && 
            (url.startsWith("http://") || url.startsWith("https://"))
    }
    
    fun isPhoneValid(): Boolean {
        return _uiState.value.merchantPhone.isNotBlank()
    }
    
    fun toggleSmsAutoSync(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(smsAutoSyncEnabled = enabled)
        viewModelScope.launch {
            userPreferences.setSmsAutoSyncEnabled(enabled)
        }
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
            // Navigation will be handled by the screen
        }
    }
}
