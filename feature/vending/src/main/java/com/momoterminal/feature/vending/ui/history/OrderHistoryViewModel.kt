package com.momoterminal.feature.vending.ui.history
import androidx.lifecycle.*
import com.momoterminal.feature.vending.domain.model.VendingOrder
import com.momoterminal.feature.vending.domain.usecase.GetOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val getOrdersUseCase: GetOrdersUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<OrderHistoryUiState>(OrderHistoryUiState.Loading)
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()
    
    init { loadOrders() }
    
    private fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = OrderHistoryUiState.Loading
            getOrdersUseCase().collect { result ->
                result.fold(
                    onSuccess = { _uiState.value = if (it.isEmpty()) OrderHistoryUiState.Empty else OrderHistoryUiState.Success(it.sortedByDescending { o -> o.createdAt }) },
                    onFailure = { _uiState.value = OrderHistoryUiState.Error(it.message ?: "Failed") }
                )
            }
        }
    }
    fun refresh() = loadOrders()
}

sealed class OrderHistoryUiState {
    data object Loading : OrderHistoryUiState()
    data object Empty : OrderHistoryUiState()
    data class Success(val orders: List<VendingOrder>) : OrderHistoryUiState()
    data class Error(val message: String) : OrderHistoryUiState()
}
