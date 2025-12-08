package com.momoterminal.feature.vending.ui.code
import androidx.lifecycle.*
import com.momoterminal.feature.vending.domain.model.VendingOrder
import com.momoterminal.feature.vending.domain.usecase.RefreshOrderStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CodeDisplayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val refreshOrderStatusUseCase: RefreshOrderStatusUseCase
) : ViewModel() {
    private val orderId: String = checkNotNull(savedStateHandle["orderId"])
    private val _uiState = MutableStateFlow<CodeDisplayUiState>(CodeDisplayUiState.Loading)
    val uiState: StateFlow<CodeDisplayUiState> = _uiState.asStateFlow()
    private val _countdown = MutableStateFlow(0L)
    val countdown: StateFlow<Long> = _countdown.asStateFlow()
    
    init { loadOrder() }
    
    private fun loadOrder() {
        viewModelScope.launch {
            _uiState.value = CodeDisplayUiState.Loading
            refreshOrderStatusUseCase(orderId).fold(
                onSuccess = {
                    if (it.code != null) {
                        _uiState.value = CodeDisplayUiState.Success(it)
                        startCountdown(it)
                    } else _uiState.value = CodeDisplayUiState.Error("Code not available")
                },
                onFailure = { _uiState.value = CodeDisplayUiState.Error(it.message ?: "Failed") }
            )
        }
    }
    
    private fun startCountdown(order: VendingOrder) {
        viewModelScope.launch {
            while (true) {
                val remaining = order.code?.remainingSeconds() ?: 0
                _countdown.value = remaining
                if (remaining <= 0) { refreshStatus(); break }
                delay(1000)
            }
        }
    }
    fun refreshStatus() = loadOrder()
}

sealed class CodeDisplayUiState {
    data object Loading : CodeDisplayUiState()
    data class Success(val order: VendingOrder) : CodeDisplayUiState()
    data class Error(val message: String) : CodeDisplayUiState()
}
