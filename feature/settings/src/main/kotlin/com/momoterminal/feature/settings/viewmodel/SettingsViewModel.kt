package com.momoterminal.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.core.domain.model.settings.*
import com.momoterminal.core.domain.usecase.settings.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getMerchantSettings: GetMerchantSettingsUseCase,
    private val updateProfile: UpdateMerchantProfileUseCase,
    private val updateBusinessDetails: UpdateBusinessDetailsUseCase,
    private val updateNotificationPrefs: UpdateNotificationPreferencesUseCase,
    private val updateTransactionLimits: UpdateTransactionLimitsUseCase,
    private val updateFeatureFlags: UpdateFeatureFlagsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun loadSettings(userId: String) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            
            getMerchantSettings(userId)
                .onSuccess { settings ->
                    _uiState.value = SettingsUiState.Success(settings)
                }
                .onFailure { error ->
                    _uiState.value = SettingsUiState.Error(
                        error.message ?: "Failed to load settings"
                    )
                }
        }
    }

    fun updateBusinessName(userId: String, name: String) {
        viewModelScope.launch {
            val currentState = _uiState.value as? SettingsUiState.Success ?: return@launch
            
            updateProfile(userId, businessName = name)
                .onSuccess {
                    val updatedSettings = currentState.settings.copy(
                        profile = currentState.settings.profile.copy(businessName = name)
                    )
                    _uiState.value = SettingsUiState.Success(updatedSettings)
                }
                .onFailure { error ->
                    _uiState.value = SettingsUiState.Error(
                        error.message ?: "Failed to update business name"
                    )
                }
        }
    }

    fun updateBusinessDetailsData(userId: String, details: BusinessDetails) {
        viewModelScope.launch {
            val currentState = _uiState.value as? SettingsUiState.Success ?: return@launch
            
            updateBusinessDetails(userId, details)
                .onSuccess {
                    val updatedSettings = currentState.settings.copy(businessDetails = details)
                    _uiState.value = SettingsUiState.Success(updatedSettings)
                }
                .onFailure { error ->
                    _uiState.value = SettingsUiState.Error(
                        error.message ?: "Failed to update business details"
                    )
                }
        }
    }

    fun updateNotifications(userId: String, preferences: NotificationPreferences) {
        viewModelScope.launch {
            val currentState = _uiState.value as? SettingsUiState.Success ?: return@launch
            
            updateNotificationPrefs(userId, preferences)
                .onSuccess {
                    val updatedSettings = currentState.settings.copy(notificationPrefs = preferences)
                    _uiState.value = SettingsUiState.Success(updatedSettings)
                }
                .onFailure { error ->
                    _uiState.value = SettingsUiState.Error(
                        error.message ?: "Failed to update notifications"
                    )
                }
        }
    }

    fun updateLimits(userId: String, limits: TransactionLimits) {
        viewModelScope.launch {
            val currentState = _uiState.value as? SettingsUiState.Success ?: return@launch
            
            updateTransactionLimits(userId, limits)
                .onSuccess {
                    val updatedSettings = currentState.settings.copy(transactionLimits = limits)
                    _uiState.value = SettingsUiState.Success(updatedSettings)
                }
                .onFailure { error ->
                    _uiState.value = SettingsUiState.Error(
                        error.message ?: "Failed to update limits"
                    )
                }
        }
    }

    fun updateFlags(userId: String, flags: FeatureFlags) {
        viewModelScope.launch {
            val currentState = _uiState.value as? SettingsUiState.Success ?: return@launch
            
            updateFeatureFlags(userId, flags)
                .onSuccess {
                    val updatedSettings = currentState.settings.copy(featureFlags = flags)
                    _uiState.value = SettingsUiState.Success(updatedSettings)
                }
                .onFailure { error ->
                    _uiState.value = SettingsUiState.Error(
                        error.message ?: "Failed to update features"
                    )
                }
        }
    }

    fun toggleFeatureFlag(userId: String, flagName: String, enabled: Boolean) {
        viewModelScope.launch {
            val currentState = _uiState.value as? SettingsUiState.Success ?: return@launch
            val currentFlags = currentState.settings.featureFlags
            
            val updatedFlags = when (flagName) {
                "nfc" -> currentFlags.copy(nfcEnabled = enabled)
                "offline" -> currentFlags.copy(offlineMode = enabled)
                "autoSync" -> currentFlags.copy(autoSync = enabled)
                "biometric" -> currentFlags.copy(biometricRequired = enabled)
                "receipts" -> currentFlags.copy(receiptsEnabled = enabled)
                "multiCurrency" -> currentFlags.copy(multiCurrency = enabled)
                "analytics" -> currentFlags.copy(advancedAnalytics = enabled)
                "api" -> currentFlags.copy(apiAccess = enabled)
                else -> return@launch
            }
            
            updateFlags(userId, updatedFlags)
        }
    }
}

sealed interface SettingsUiState {
    object Loading : SettingsUiState
    data class Success(val settings: MerchantSettings) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}
