package com.momoterminal.presentation.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.config.AppConfig
import com.momoterminal.nfc.NfcManager
import com.momoterminal.nfc.NfcPaymentData
import com.momoterminal.nfc.NfcState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Terminal screen.
 */
@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val nfcManager: NfcManager,
    private val appConfig: AppConfig
) : ViewModel() {
    
    companion object {
        private const val MAX_AMOUNT_LENGTH = 10
    }
    
    /**
     * UI state for the Terminal screen.
     */
    data class TerminalUiState(
        val amount: String = "",
        val selectedProvider: NfcPaymentData.Provider = NfcPaymentData.Provider.MTN,
        val merchantPhone: String = "",
        val isConfigured: Boolean = false,
        val isPaymentActive: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()
    
    // NFC state from manager
    val nfcState: StateFlow<NfcState> = nfcManager.nfcState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NfcState.Ready
        )
    
    init {
        loadConfig()
    }
    
    private fun loadConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                merchantPhone = appConfig.getMerchantPhone(),
                isConfigured = appConfig.isConfigured()
            )
        }
    }
    
    fun onDigitClick(digit: String) {
        val currentAmount = _uiState.value.amount
        
        // Prevent leading zeros
        if (digit == "0" && currentAmount.isEmpty()) return
        
        // Limit length
        if (currentAmount.length >= MAX_AMOUNT_LENGTH) return
        
        _uiState.value = _uiState.value.copy(
            amount = currentAmount + digit
        )
    }
    
    fun onBackspaceClick() {
        val currentAmount = _uiState.value.amount
        if (currentAmount.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                amount = currentAmount.dropLast(1)
            )
        }
    }
    
    fun onClearClick() {
        _uiState.value = _uiState.value.copy(amount = "")
    }
    
    fun onProviderSelected(provider: NfcPaymentData.Provider) {
        _uiState.value = _uiState.value.copy(selectedProvider = provider)
    }
    
    fun activatePayment() {
        val state = _uiState.value
        
        if (state.amount.isEmpty() || state.merchantPhone.isEmpty()) return
        
        val amountDouble = state.amount.toDoubleOrNull() ?: return
        val amountInMinorUnits = (amountDouble * 100).toLong()
        
        val paymentData = NfcPaymentData(
            merchantPhone = state.merchantPhone,
            amountInMinorUnits = amountInMinorUnits,
            currency = appConfig.getCurrency(),
            provider = state.selectedProvider
        )
        
        nfcManager.activatePayment(paymentData)
        _uiState.value = _uiState.value.copy(isPaymentActive = true)
    }
    
    fun cancelPayment() {
        nfcManager.cancelPayment()
        _uiState.value = _uiState.value.copy(isPaymentActive = false)
    }
    
    fun isNfcAvailable(): Boolean {
        return nfcManager.isNfcAvailable()
    }
    
    fun isAmountValid(): Boolean {
        val amount = _uiState.value.amount
        return amount.isNotEmpty() && amount.toDoubleOrNull()?.let { it > 0 } ?: false
    }
}
