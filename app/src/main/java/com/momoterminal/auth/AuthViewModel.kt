package com.momoterminal.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.security.BiometricHelper
import com.momoterminal.util.PhoneNumberValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val sessionManager: SessionManager,
    private val phoneNumberValidator: PhoneNumberValidator
) : ViewModel() {

    /**
     * UI state for authentication screens.
     */
    data class AuthUiState(
        val isLoading: Boolean = false,
        val isAuthenticated: Boolean = false,
        val error: String? = null,
        val phoneNumber: String = "",
        val formattedPhoneNumber: String = "",
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
        val isLockedOut: Boolean = false,
        val otpExpiresAt: Long = 0L,
        val canResendOtpAt: Long = 0L,
        val otpResendCountdown: Int = 0
        // OTP timer fields
        val otpExpiresAt: Long = 0L,
        val otpExpiryCountdown: Int = 0,
        val canResendOtpAt: Long = 0L,
        val resendCountdown: Int = 0,
        val canResendOtp: Boolean = false,
        val phoneNumberError: String? = null
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
    
    private var resendCountdownJob: Job? = null
    private var otpExpiryCountdownJob: Job? = null

    companion object {
        const val MAX_PIN_ATTEMPTS = 3
        const val PIN_LENGTH = 6
        const val OTP_RESEND_COOLDOWN_SECONDS = 60
        const val OTP_EXPIRY_SECONDS = 300 // 5 minutes
        const val OTP_LENGTH = 6
        const val OTP_EXPIRY_SECONDS = 300 // 5 minutes
        const val RESEND_COOLDOWN_SECONDS = 60 // 60 seconds
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
        val validationResult = phoneNumberValidator.validate(phone)
        val formattedNumber = when (validationResult) {
            is PhoneNumberValidator.ValidationResult.Valid -> validationResult.formattedNumber
            is PhoneNumberValidator.ValidationResult.Invalid -> ""
        }
        val phoneError = if (phone.isNotBlank() && formattedNumber.isEmpty()) {
            when (validationResult) {
                is PhoneNumberValidator.ValidationResult.Invalid -> validationResult.reason
                else -> null
            }
        } else null
        
        _uiState.value = _uiState.value.copy(
            phoneNumber = phone,
            formattedPhoneNumber = formattedNumber,
            phoneNumberError = phoneError,
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
        if (otp.length <= OTP_LENGTH && otp.all { it.isDigit() }) {
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
     * Attempt login with phone number and OTP code.
     */
    fun login() {
        val state = _uiState.value
        
        // Validate inputs
        if (state.phoneNumber.isBlank()) {
            _uiState.value = state.copy(error = "Please enter your phone number")
            return
        }
        
        // Validate phone number format
        if (state.formattedPhoneNumber.isEmpty()) {
            _uiState.value = state.copy(error = "Please enter a valid phone number")
            return
        }
        
        if (state.otpCode.length != OTP_LENGTH) {
            _uiState.value = state.copy(error = "Please enter the 6-digit OTP code")
            return
        }

        if (state.isLockedOut) {
            _uiState.value = state.copy(error = "Too many attempts. Please try again later.")
            return
        }
        
        // Check if OTP has expired
        if (isOtpExpired()) {
            _uiState.value = state.copy(error = "OTP has expired. Please request a new one.")
            return
        }
        
        // Use formatted phone number if available
        val phoneToUse = state.formattedPhoneNumber.ifBlank { state.phoneNumber }

        viewModelScope.launch {
            authRepository.login(phoneToUse, state.otpCode)
            authRepository.login(state.formattedPhoneNumber, state.otpCode)
                .catch { e ->
                    handleLoginFailure(e.message ?: "Login failed")
                }
                .collect { result ->
                    when (result) {
                        is AuthRepository.AuthResult.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                        }
                        is AuthRepository.AuthResult.Success -> {
                            resendCountdownJob?.cancel()
                            stopCountdownTimers()
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
            pin = "",
            otpCode = "" // Also clear OTP code on failure
        )
    }

    /**
     * Request OTP for phone number verification.
     * Includes phone validation and rate limiting.
     */
    fun requestOtp() {
        val state = _uiState.value
        val phoneNumber = state.phoneNumber
        
        // Validate phone number
        val validationResult = PhoneNumberValidator.validate(phoneNumber)
        if (!validationResult.isValid) {
            _uiState.value = _uiState.value.copy(
                error = validationResult.errorMessage ?: "Please enter a valid phone number"
            )
            return
        }
        
        // Check rate limiting
        val currentTime = System.currentTimeMillis()
        if (currentTime < _uiState.value.canResendOtpAt) {
            val remainingSeconds = ((_uiState.value.canResendOtpAt - currentTime) / 1000).toInt()
            _uiState.value = _uiState.value.copy(
                error = "Please wait $remainingSeconds seconds before requesting another OTP"
            )
        if (phoneNumber.isBlank()) {
            _uiState.value = state.copy(error = "Please enter your phone number")
            return
        }
        
        // Validate phone number format
        if (state.formattedPhoneNumber.isEmpty()) {
            _uiState.value = state.copy(error = state.phoneNumberError ?: "Please enter a valid phone number")
            return
        }
        
        // Check if resend is allowed
        if (!state.canResendOtp && state.isOtpSent) {
            _uiState.value = state.copy(error = "Please wait ${state.resendCountdown} seconds before resending")
            return
        }
        
        val formattedPhone = validationResult.formattedNumber ?: phoneNumber

        viewModelScope.launch {
            authRepository.requestOtp(formattedPhone)
            authRepository.requestOtp(state.formattedPhoneNumber)
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
                            val now = System.currentTimeMillis()
                            val expiresInSeconds = result.data.expiresInSeconds
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isOtpSent = true,
                                error = null,
                                formattedPhoneNumber = formattedPhone,
                                otpExpiresAt = now + (expiresInSeconds * 1000L),
                                canResendOtpAt = now + (OTP_RESEND_COOLDOWN_SECONDS * 1000L),
                                registrationStep = RegistrationStep.OTP_VERIFICATION
                            )
                                registrationStep = RegistrationStep.OTP_VERIFICATION,
                                otpExpiresAt = now + (OTP_EXPIRY_SECONDS * 1000L),
                                otpExpiryCountdown = OTP_EXPIRY_SECONDS,
                                canResendOtpAt = now + (RESEND_COOLDOWN_SECONDS * 1000L),
                                resendCountdown = RESEND_COOLDOWN_SECONDS,
                                canResendOtp = false,
                                otpCode = "" // Clear any existing OTP code
                            )
                            startOtpExpiryCountdown()
                            startResendCountdown()
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
     * Starts the countdown timer for OTP resend button.
     * Start the OTP expiry countdown timer.
     */
    private fun startOtpExpiryCountdown() {
        otpExpiryCountdownJob?.cancel()
        otpExpiryCountdownJob = viewModelScope.launch {
            while (_uiState.value.otpExpiryCountdown > 0) {
                delay(1000L)
                val newCountdown = _uiState.value.otpExpiryCountdown - 1
                _uiState.value = _uiState.value.copy(otpExpiryCountdown = newCountdown)
            }
            // OTP has expired
            _uiState.value = _uiState.value.copy(
                error = "OTP has expired. Please request a new one."
            )
        }
    }
    
    /**
     * Start the resend cooldown timer.
     */
    private fun startResendCountdown() {
        resendCountdownJob?.cancel()
        resendCountdownJob = viewModelScope.launch {
            for (i in OTP_RESEND_COOLDOWN_SECONDS downTo 0) {
                _uiState.value = _uiState.value.copy(otpResendCountdown = i)
                if (i > 0) {
                    delay(1000)
                }
            }
        }
    }
    
    /**
     * Checks if OTP has expired.
     * @return true if OTP is expired, false otherwise
     */
    fun isOtpExpired(): Boolean {
        val expiresAt = _uiState.value.otpExpiresAt
        return expiresAt > 0 && System.currentTimeMillis() > expiresAt
    }
    
    /**
     * Gets the remaining time for OTP expiry in seconds.
     * @return remaining seconds, or 0 if expired
     */
    fun getOtpRemainingSeconds(): Int {
        val expiresAt = _uiState.value.otpExpiresAt
        if (expiresAt <= 0) return 0
        val remaining = (expiresAt - System.currentTimeMillis()) / 1000
        return maxOf(0, remaining.toInt())
            while (_uiState.value.resendCountdown > 0) {
                delay(1000L)
                val newCountdown = _uiState.value.resendCountdown - 1
                _uiState.value = _uiState.value.copy(
                    resendCountdown = newCountdown,
                    canResendOtp = newCountdown <= 0
                )
            }
        }
    }
    
    /**
     * Stop all countdown timers.
     */
    private fun stopCountdownTimers() {
        resendCountdownJob?.cancel()
        otpExpiryCountdownJob?.cancel()
    }
    
    /**
     * Go back to phone entry step and allow changing phone number.
     */
    fun changePhoneNumber() {
        stopCountdownTimers()
        _uiState.value = _uiState.value.copy(
            registrationStep = RegistrationStep.PHONE_ENTRY,
            isOtpSent = false,
            otpCode = "",
            otpExpiresAt = 0L,
            otpExpiryCountdown = 0,
            canResendOtpAt = 0L,
            resendCountdown = 0,
            canResendOtp = false,
            error = null
        )
    }

    /**
     * Verify OTP code.
     */
    fun verifyOtp() {
        val state = _uiState.value
        
        if (state.otpCode.length != OTP_LENGTH) {
            _uiState.value = state.copy(error = "Please enter the 6-digit OTP")
            return
        }
        
        // Check if OTP has expired
        if (state.otpExpiryCountdown <= 0) {
            _uiState.value = state.copy(error = "OTP has expired. Please request a new one.")
            return
        }

        viewModelScope.launch {
            authRepository.verifyOtp(state.formattedPhoneNumber, state.otpCode)
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
                            stopCountdownTimers()
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
                phoneNumber = state.formattedPhoneNumber,
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
        
        // Stop timers if going back to phone entry
        if (previousStep == RegistrationStep.PHONE_ENTRY) {
            stopCountdownTimers()
            _uiState.value = _uiState.value.copy(
                registrationStep = previousStep,
                isOtpSent = false,
                otpCode = "",
                otpExpiresAt = 0L,
                otpExpiryCountdown = 0,
                canResendOtpAt = 0L,
                resendCountdown = 0,
                canResendOtp = false
            )
        } else {
            _uiState.value = _uiState.value.copy(registrationStep = previousStep)
        }
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
        stopCountdownTimers()
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
        stopCountdownTimers()
        _uiState.value = AuthUiState(isBiometricAvailable = biometricHelper.isBiometricAvailable())
    }
    
    override fun onCleared() {
        super.onCleared()
        stopCountdownTimers()
    }
}
