package com.momoterminal.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.auth.AuthRepository
import com.momoterminal.auth.OtpResult
import com.momoterminal.auth.WhatsAppOtpService
import com.momoterminal.supabase.SessionData
import com.momoterminal.util.PhoneNumberValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Forgot PIN flow.
 */
@HiltViewModel
class ForgotPinViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val whatsAppOtpService: WhatsAppOtpService,
    private val phoneNumberValidator: PhoneNumberValidator
) : ViewModel() {
    
    /**
     * Forgot PIN flow steps.
     */
    enum class ForgotPinStep {
        PHONE_ENTRY,
        OTP_VERIFICATION,
        PIN_ENTRY,
        PIN_CONFIRM,
        SUCCESS
    }
    
    /**
     * UI state for Forgot PIN screen.
     */
    data class ForgotPinUiState(
        val step: ForgotPinStep = ForgotPinStep.PHONE_ENTRY,
        val isLoading: Boolean = false,
        val error: String? = null,
        val countryCode: String = "+250",
        val phoneNumber: String = "",
        val phoneNumberError: String? = null,
        val otpCode: String = "",
        val newPin: String = "",
        val confirmPin: String = "",
        val pinMismatch: Boolean = false,
        val userId: String? = null
    )
    
    private val _uiState = MutableStateFlow(ForgotPinUiState())
    val uiState: StateFlow<ForgotPinUiState> = _uiState.asStateFlow()
    
    fun updateCountryCode(code: String) {
        _uiState.value = _uiState.value.copy(countryCode = code)
    }
    
    fun updatePhoneNumber(phone: String) {
        _uiState.value = _uiState.value.copy(
            phoneNumber = phone,
            phoneNumberError = null
        )
    }
    
    fun updateOtpCode(otp: String) {
        if (otp.length <= 6 && otp.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(otpCode = otp)
        }
    }
    
    fun updateNewPin(pin: String) {
        if (pin.length <= 4 && pin.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(newPin = pin)
        }
    }
    
    fun updateConfirmPin(pin: String) {
        if (pin.length <= 4 && pin.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(
                confirmPin = pin,
                pinMismatch = false
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Send OTP to the entered phone number.
     */
    fun sendOtp() {
        val state = _uiState.value
        val fullPhoneNumber = "${state.countryCode}${state.phoneNumber}"
        
        // Validate phone number
        if (!phoneNumberValidator.isValid(fullPhoneNumber)) {
            _uiState.value = state.copy(
                phoneNumberError = "Please enter a valid phone number"
            )
            return
        }
        
        _uiState.value = state.copy(isLoading = true, phoneNumberError = null)
        
        viewModelScope.launch {
            try {
                // Send OTP via WhatsApp
                whatsAppOtpService.sendOtp(fullPhoneNumber)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    step = ForgotPinStep.OTP_VERIFICATION
                )
                
                Timber.d("OTP sent successfully to $fullPhoneNumber")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to send OTP. Please try again."
                )
                Timber.e(e, "Failed to send OTP")
            }
        }
    }
    
    /**
     * Resend OTP.
     */
    fun resendOtp() {
        sendOtp()
    }
    
    /**
     * Verify the entered OTP.
     */
    fun verifyOtp() {
        val state = _uiState.value
        
        if (state.otpCode.length != 6) {
            _uiState.value = state.copy(error = "Please enter the 6-digit code")
            return
        }
        
        _uiState.value = state.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                val fullPhoneNumber = "${state.countryCode}${state.phoneNumber}"
                
                // Verify OTP  
                when (val result = whatsAppOtpService.verifyOtp(fullPhoneNumber, state.otpCode)) {
                    is OtpResult.Success<*> -> {
                        val sessionData = result.data as SessionData
                        val userId = sessionData.user.id
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            step = ForgotPinStep.PIN_ENTRY,
                            userId = userId
                        )
                        Timber.d("OTP verified successfully")
                    }
                    is OtpResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Verification failed. Please try again."
                )
                Timber.e(e, "OTP verification failed")
            }
        }
    }
    
    /**
     * Continue to PIN confirmation step.
     */
    fun continueToConfirm() {
        if (_uiState.value.newPin.length == 4) {
            _uiState.value = _uiState.value.copy(step = ForgotPinStep.PIN_CONFIRM)
        }
    }
    
    /**
     * Reset the PIN.
     */
    fun resetPin() {
        val state = _uiState.value
        
        // Validate PIN match
        if (state.newPin != state.confirmPin) {
            _uiState.value = state.copy(
                pinMismatch = true,
                error = "PINs do not match"
            )
            return
        }
        
        if (state.userId == null) {
            _uiState.value = state.copy(error = "Session expired. Please start over.")
            return
        }
        
        _uiState.value = state.copy(isLoading = true, pinMismatch = false)
        
        viewModelScope.launch {
            try {
                // Reset PIN via repository
                authRepository.resetPin(state.userId, state.newPin)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    step = ForgotPinStep.SUCCESS
                )
                
                Timber.d("PIN reset successfully")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to reset PIN. Please try again."
                )
                Timber.e(e, "PIN reset failed")
            }
        }
    }
}
