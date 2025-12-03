package com.momoterminal.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.feature.wallet.domain.model.Token
import com.momoterminal.feature.wallet.domain.model.WalletBalance
import com.momoterminal.feature.wallet.domain.usecase.AddTokenUseCase
import com.momoterminal.feature.wallet.domain.usecase.GetWalletBalanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val getWalletBalanceUseCase: GetWalletBalanceUseCase,
    private val addTokenUseCase: AddTokenUseCase
) : ViewModel() {

    val walletBalance: StateFlow<WalletBalance?> = getWalletBalanceUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        loadWallet()
    }

    private fun loadWallet() {
        viewModelScope.launch {
            _uiState.value = WalletUiState.Loading
            walletBalance.collect { balance ->
                _uiState.value = if (balance != null) {
                    WalletUiState.Success(balance)
                } else {
                    WalletUiState.Empty
                }
            }
        }
    }

    fun addToken(token: Token) {
        viewModelScope.launch {
            addTokenUseCase(token).fold(
                onSuccess = {
                    // Token added successfully
                },
                onFailure = { error ->
                    _uiState.value = WalletUiState.Error(error.message ?: "Failed to add token")
                }
            )
        }
    }

    fun refresh() {
        loadWallet()
    }
}

sealed class WalletUiState {
    data object Loading : WalletUiState()
    data object Empty : WalletUiState()
    data class Success(val balance: WalletBalance) : WalletUiState()
    data class Error(val message: String) : WalletUiState()
}
