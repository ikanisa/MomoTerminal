package com.momoterminal.feature.auth.viewmodel

import androidx.lifecycle.viewModelScope
import com.momoterminal.core.ui.UiEffect
import com.momoterminal.core.ui.UiEvent
import com.momoterminal.core.ui.UiState
import com.momoterminal.core.domain.repository.AuthRepository
import com.momoterminal.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel<AuthState, AuthEvent, AuthEffect>(AuthState()) {

    override fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.SendOtp -> sendOtp(event.phone)
            is AuthEvent.VerifyOtp -> verifyOtp(event.phone, event.code)
            is AuthEvent.SignOut -> signOut()
        }
    }

    private fun sendOtp(phone: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            authRepository.sendOtp(phone).onSuccess {
                updateState { copy(isLoading = false, otpSent = true) }
                sendEffect(AuthEffect.OtpSent)
            }.onError { error ->
                updateState { copy(isLoading = false, error = error.message) }
            }
        }
    }

    private fun verifyOtp(phone: String, code: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            authRepository.verifyOtp(phone, code).onSuccess { user ->
                updateState { copy(isLoading = false, isAuthenticated = true) }
                sendEffect(AuthEffect.NavigateToHome)
            }.onError { error ->
                updateState { copy(isLoading = false, error = error.message) }
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            updateState { copy(isAuthenticated = false) }
        }
    }
}

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val otpSent: Boolean = false,
    val error: String? = null
) : UiState

sealed class AuthEvent : UiEvent {
    data class SendOtp(val phone: String) : AuthEvent()
    data class VerifyOtp(val phone: String, val code: String) : AuthEvent()
    data object SignOut : AuthEvent()
}

sealed class AuthEffect : UiEffect {
    data object OtpSent : AuthEffect()
    data object NavigateToHome : AuthEffect()
}
