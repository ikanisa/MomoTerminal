package com.momoterminal.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.security.BiometricHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication screens (Login, Register).
 * Handles login state, registration state, error handling, and loading states.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val biometricHelper: BiometricHelper,
    private val sessionManager: SessionManager
) : ViewModel() {

    /**
     * UI state for authentication screens.
     */
    data class AuthUiState(
        val isLoading: Boolean = false,
        val isAuthenticated: Boolean = false,
        val error: String? = null,
        val phoneNumber: String = "",
        val pin: String = "",
        val confirmPin: String = "",
        val merchantName: String = "",
        val acceptedTerms: Boolean = false,
        val otpCode: String = "",
        val isOtpSent: Boolean = false,
        val isOtpVerified: Boolean = false,
        val isBiometricAvailable: Boolean = false,
        val registrationStep: RegistrationStep = RegistrationStep.PHONE_ENTRY,
        val pinAttempts: Int = 0,
        val isLockedOut: Boolean = false
    )

    /**
     * Registration flow steps.
     */
    enum class RegistrationStep {
        PHONE_ENTRY,
        OTP_VERIFICATION,
        PIN_CREATION,
        MERCHANT_INFO,
        TERMS_ACCEPTANCE
    }

    /**
     * Authentication events for one-time UI actions.
     */
    sealed class AuthEvent {
        data object NavigateToHome : AuthEvent()
        data object NavigateToLogin : AuthEvent()
        data object NavigateToRegister : AuthEvent()
        data class ShowError(val message: String) : AuthEvent()
        data object ShowBiometricPrompt : AuthEvent()
        data object SessionExpired : AuthEvent()
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<AuthEvent?>(null)
    val events: StateFlow<AuthEvent?> = _events.asStateFlow()

    companion object {
        const val MAX_PIN_ATTEMPTS = 3
        const val PIN_LENGTH = 6
    }

    init {
        checkBiometricAvailability()
        checkExistingSession()
    }

    private fun checkBiometricAvailability() {
        _uiState.value = _uiState.value.copy(
            isBiometricAvailable = biometricHelper.isBiometricAvailable()
        )
    }

    private fun checkExistingSession() {
        if (authRepository.isAuthenticated() && sessionManager.validateSession()) {
            _uiState.value = _uiState.value.copy(isAuthenticated = true)
            _events.value = AuthEvent.NavigateToHome
        }
    }

    // Input update functions
    fun updatePhoneNumber(phone: String) {
        _uiState.value = _uiState.value.copy(
            phoneNumber = phone,
            error = null
        )
    }

    fun updatePin(pin: String) {
        if (pin.length <= PIN_LENGTH && pin.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(
                pin = pin,
                error = null
            )
        }
    }

    fun updateConfirmPin(pin: String) {
        if (pin.length <= PIN_LENGTH && pin.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(
                confirmPin = pin,
                error = null
            )
        }
    }

    fun updateMerchantName(name: String) {
        _uiState.value = _uiState.value.copy(
            merchantName = name,
            error = null
        )
    }

    fun updateOtpCode(otp: String) {
        if (otp.length <= 6 && otp.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(
                otpCode = otp,
                error = null
            )
        }
    }

    fun updateTermsAcceptance(accepted: Boolean) {
        _uiState.value = _uiState.value.copy(acceptedTerms = accepted)
    }

    /**
     * Attempt login with phone number and PIN.
     */
    fun login() {
        val state = _uiState.value
        
        // Validate inputs
        if (state.phoneNumber.isBlank()) {
            _uiState.value = state.copy(error = "Please enter your phone number")
            return
        }
        
        if (state.pin.length != PIN_LENGTH) {
            _uiState.value = state.copy(error = "PIN must be $PIN_LENGTH digits")
            return
        }

        if (state.isLockedOut) {
            _uiState.value = state.copy(error = "Too many attempts. Please try again later.")
            return
        }

        viewModelScope.launch {
            authRepository.login(state.phoneNumber, state.pin)
                .catch { e ->
                    handleLoginFailure(e.message ?: "Login failed")
                }
                .collect { result ->
                    when (result) {
                        is AuthRepository.AuthResult.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                        }
                        is AuthRepository.AuthResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isAuthenticated = true,
                                error = null,
                                pinAttempts = 0
                            )
                            _events.value = AuthEvent.NavigateToHome
                        }
                        is AuthRepository.AuthResult.Error -> {
                            handleLoginFailure(result.message)
                        }
                    }
                }
        }
    }

    private fun handleLoginFailure(message: String) {
        val newAttempts = _uiState.value.pinAttempts + 1
        val isLockedOut = newAttempts >= MAX_PIN_ATTEMPTS
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = if (isLockedOut) "Account locked. Please try again later." else message,
            pinAttempts = newAttempts,
            isLockedOut = isLockedOut,
            pin = ""
        )
    }

    /**
     * Request OTP for phone number verification.
     */
    fun requestOtp() {
        val phoneNumber = _uiState.value.phoneNumber
        
        if (phoneNumber.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your phone number")
            return
        }

        viewModelScope.launch {
            authRepository.requestOtp(phoneNumber)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to send OTP"
                    )
                }
                .collect { result ->
                    when (result) {
                        is AuthRepository.AuthResult.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                        }
                        is AuthRepository.AuthResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isOtpSent = true,
                                error = null,
                                registrationStep = RegistrationStep.OTP_VERIFICATION
                            )
                        }
                        is AuthRepository.AuthResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
        }
    }

    /**
     * Verify OTP code.
     */
    fun verifyOtp() {
        val state = _uiState.value
        
        if (state.otpCode.length != 6) {
            _uiState.value = state.copy(error = "Please enter the 6-digit OTP")
            return
        }

        viewModelScope.launch {
            authRepository.verifyOtp(state.phoneNumber, state.otpCode)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "OTP verification failed"
                    )
                }
                .collect { result ->
                    when (result) {
                        is AuthRepository.AuthResult.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                        }
                        is AuthRepository.AuthResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isOtpVerified = true,
                                error = null,
                                registrationStep = RegistrationStep.PIN_CREATION
                            )
                        }
                        is AuthRepository.AuthResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
        }
    }

    /**
     * Complete registration with all collected information.
     */
    fun register() {
        val state = _uiState.value
        
        // Validate PIN
        if (state.pin.length != PIN_LENGTH) {
            _uiState.value = state.copy(error = "PIN must be $PIN_LENGTH digits")
            return
        }
        
        if (state.pin != state.confirmPin) {
            _uiState.value = state.copy(error = "PINs do not match")
            return
        }
        
        if (state.merchantName.isBlank()) {
            _uiState.value = state.copy(error = "Please enter your merchant/business name")
            return
        }
        
        if (!state.acceptedTerms) {
            _uiState.value = state.copy(error = "Please accept the terms and conditions")
            return
        }

        viewModelScope.launch {
            authRepository.register(
                phoneNumber = state.phoneNumber,
                pin = state.pin,
                merchantName = state.merchantName,
                acceptedTerms = state.acceptedTerms
            )
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Registration failed"
                    )
                }
                .collect { result ->
                    when (result) {
                        is AuthRepository.AuthResult.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                        }
                        is AuthRepository.AuthResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isAuthenticated = true,
                                error = null
                            )
                            _events.value = AuthEvent.NavigateToHome
                        }
                        is AuthRepository.AuthResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
        }
    }

    /**
     * Move to next registration step.
     */
    fun nextRegistrationStep() {
        val currentStep = _uiState.value.registrationStep
        val nextStep = when (currentStep) {
            RegistrationStep.PHONE_ENTRY -> RegistrationStep.OTP_VERIFICATION
            RegistrationStep.OTP_VERIFICATION -> RegistrationStep.PIN_CREATION
            RegistrationStep.PIN_CREATION -> RegistrationStep.MERCHANT_INFO
            RegistrationStep.MERCHANT_INFO -> RegistrationStep.TERMS_ACCEPTANCE
            RegistrationStep.TERMS_ACCEPTANCE -> RegistrationStep.TERMS_ACCEPTANCE
        }
        _uiState.value = _uiState.value.copy(registrationStep = nextStep)
    }

    /**
     * Move to previous registration step.
     */
    fun previousRegistrationStep() {
        val currentStep = _uiState.value.registrationStep
        val previousStep = when (currentStep) {
            RegistrationStep.PHONE_ENTRY -> RegistrationStep.PHONE_ENTRY
            RegistrationStep.OTP_VERIFICATION -> RegistrationStep.PHONE_ENTRY
            RegistrationStep.PIN_CREATION -> RegistrationStep.OTP_VERIFICATION
            RegistrationStep.MERCHANT_INFO -> RegistrationStep.PIN_CREATION
            RegistrationStep.TERMS_ACCEPTANCE -> RegistrationStep.MERCHANT_INFO
        }
        _uiState.value = _uiState.value.copy(registrationStep = previousStep)
    }

    /**
     * Trigger biometric authentication.
     */
    fun triggerBiometricAuth() {
        if (_uiState.value.isBiometricAvailable) {
            _events.value = AuthEvent.ShowBiometricPrompt
        }
    }

    /**
     * Handle successful biometric authentication.
     */
    fun onBiometricSuccess() {
        if (authRepository.isAuthenticated()) {
            sessionManager.startSession()
            _uiState.value = _uiState.value.copy(isAuthenticated = true)
            _events.value = AuthEvent.NavigateToHome
        }
    }

    /**
     * Handle biometric authentication failure.
     */
    fun onBiometricFailure(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    /**
     * Logout the user.
     */
    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState(isBiometricAvailable = biometricHelper.isBiometricAvailable())
        _events.value = AuthEvent.NavigateToLogin
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear consumed event.
     */
    fun clearEvent() {
        _events.value = null
    }

    /**
     * Reset to initial state.
     */
    fun resetState() {
        _uiState.value = AuthUiState(isBiometricAvailable = biometricHelper.isBiometricAvailable())
    }
}
