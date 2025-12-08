package com.momoterminal.feature.vending.ui.payment
import androidx.lifecycle.*
import com.momoterminal.feature.vending.domain.model.VendingOrder
import com.momoterminal.feature.vending.domain.usecase.*
import com.momoterminal.feature.wallet.domain.usecase.GetWalletBalanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createVendingOrderUseCase: CreateVendingOrderUseCase,
    getWalletBalanceUseCase: GetWalletBalanceUseCase
) : ViewModel() {
    private val machineId: String = checkNotNull(savedStateHandle["machineId"])
    private val quantity: Int = savedStateHandle.get<String>("quantity")?.toIntOrNull() ?: 1
    val walletBalance = getWalletBalanceUseCase().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()
    
    fun confirmPayment() {
        viewModelScope.launch {
            _uiState.value = PaymentUiState.Processing
            createVendingOrderUseCase(machineId, quantity).fold(
                onSuccess = { _uiState.value = PaymentUiState.Success(it) },
                onFailure = {
                    _uiState.value = when (it) {
                        is InsufficientBalanceException -> PaymentUiState.InsufficientBalance(it.currentBalance, it.requiredAmount)
                        else -> PaymentUiState.Error(it.message ?: "Failed")
                    }
                }
            )
        }
    }
    fun resetState() { _uiState.value = PaymentUiState.Idle }
}

sealed class PaymentUiState {
    data object Idle : PaymentUiState()
    data object Processing : PaymentUiState()
    data class Success(val order: VendingOrder) : PaymentUiState()
    data class InsufficientBalance(val currentBalance: Long, val requiredAmount: Long) : PaymentUiState()
    data class Error(val message: String) : PaymentUiState()
}
