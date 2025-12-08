package com.momoterminal.feature.vending.ui.detail
import androidx.lifecycle.*
import com.momoterminal.feature.vending.domain.model.VendingMachine
import com.momoterminal.feature.vending.domain.usecase.GetMachineByIdUseCase
import com.momoterminal.feature.wallet.domain.usecase.GetWalletBalanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MachineDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMachineByIdUseCase: GetMachineByIdUseCase,
    getWalletBalanceUseCase: GetWalletBalanceUseCase
) : ViewModel() {
    private val machineId: String = checkNotNull(savedStateHandle["machineId"])
    val walletBalance = getWalletBalanceUseCase().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _uiState = MutableStateFlow<MachineDetailUiState>(MachineDetailUiState.Loading)
    val uiState: StateFlow<MachineDetailUiState> = _uiState.asStateFlow()
    
    init { loadMachine() }
    
    private fun loadMachine() {
        viewModelScope.launch {
            _uiState.value = MachineDetailUiState.Loading
            getMachineByIdUseCase(machineId).fold(
                onSuccess = { _uiState.value = MachineDetailUiState.Success(it) },
                onFailure = { _uiState.value = MachineDetailUiState.Error(it.message ?: "Failed") }
            )
        }
    }
    fun refresh() = loadMachine()
}

sealed class MachineDetailUiState {
    data object Loading : MachineDetailUiState()
    data class Success(val machine: VendingMachine) : MachineDetailUiState()
    data class Error(val message: String) : MachineDetailUiState()
}
