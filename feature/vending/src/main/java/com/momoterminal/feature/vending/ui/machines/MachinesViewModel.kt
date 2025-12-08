package com.momoterminal.feature.vending.ui.machines
import androidx.lifecycle.*
import com.momoterminal.feature.vending.domain.model.VendingMachine
import com.momoterminal.feature.vending.domain.usecase.GetMachinesUseCase
import com.momoterminal.feature.wallet.domain.usecase.GetWalletBalanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MachinesViewModel @Inject constructor(
    private val getMachinesUseCase: GetMachinesUseCase,
    getWalletBalanceUseCase: GetWalletBalanceUseCase
) : ViewModel() {
    val walletBalance = getWalletBalanceUseCase().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _uiState = MutableStateFlow<MachinesUiState>(MachinesUiState.Loading)
    val uiState: StateFlow<MachinesUiState> = _uiState.asStateFlow()
    
    init { loadMachines() }
    
    fun loadMachines(lat: Double? = null, lng: Double? = null, radius: Int? = null) {
        viewModelScope.launch {
            _uiState.value = MachinesUiState.Loading
            getMachinesUseCase(lat, lng, radius).collect { result ->
                result.fold(
                    onSuccess = { _uiState.value = if (it.isEmpty()) MachinesUiState.Empty else MachinesUiState.Success(it) },
                    onFailure = { _uiState.value = MachinesUiState.Error(it.message ?: "Failed to load") }
                )
            }
        }
    }
    fun refresh() = loadMachines()
}

sealed class MachinesUiState {
    data object Loading : MachinesUiState()
    data object Empty : MachinesUiState()
    data class Success(val machines: List<VendingMachine>) : MachinesUiState()
    data class Error(val message: String) : MachinesUiState()
}
