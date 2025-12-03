package com.momoterminal.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.config.AppConfig
import com.momoterminal.data.local.MomoDatabase
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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val nfcManager: NfcManager,
    private val database: MomoDatabase,
    private val appConfig: AppConfig
) : ViewModel() {

    companion object {
        private const val MAX_AMOUNT_LENGTH = 10
    }

    data class HomeUiState(
        val isConfigured: Boolean = false,
        val merchantPhone: String = "",
        val pendingCount: Int = 0,
        val todayRevenue: Double = 0.0,
        val todayTransactionCount: Int = 0,
        // Terminal state
        val amount: String = "",
        val currency: String = "RWF",
        val countryCode: String = "RW",
        val selectedProvider: NfcPaymentData.Provider = NfcPaymentData.Provider.MTN,
        val isPaymentActive: Boolean = false
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val nfcState: StateFlow<NfcState> = nfcManager.nfcState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NfcState.Ready
        )

    init {
        loadConfig()
        observePendingCount()
        loadTodayStats()
    }

    private fun loadConfig() {
        _uiState.value = _uiState.value.copy(
            isConfigured = appConfig.isConfigured(),
            merchantPhone = appConfig.getMerchantPhone(),
            currency = appConfig.getCurrency(),
            countryCode = appConfig.getCountryCode()
        )
    }

    private fun observePendingCount() {
        viewModelScope.launch {
            database.transactionDao().getPendingCount().collect { count ->
                _uiState.value = _uiState.value.copy(pendingCount = count)
            }
        }
    }

    private fun loadTodayStats() {
        viewModelScope.launch {
            val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % 86400000)
            val transactions = database.transactionDao()
                .getTransactionsByDateRange(todayStart, System.currentTimeMillis())
            val successful = transactions.filter { it.status == "completed" || it.status == "success" }

            _uiState.value = _uiState.value.copy(
                todayRevenue = successful.sumOf { (it.amount ?: 0.0).toLong() }.toDouble(),
                todayTransactionCount = transactions.size
            )
        }
    }

    // Terminal functions
    fun onDigitClick(digit: String) {
        val current = _uiState.value.amount
        if (digit == "0" && current.isEmpty()) return
        if (current.length >= MAX_AMOUNT_LENGTH) return
        _uiState.value = _uiState.value.copy(amount = current + digit)
    }

    fun onBackspaceClick() {
        val current = _uiState.value.amount
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(amount = current.dropLast(1))
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
            currency = state.currency,
            countryCode = state.countryCode,
            provider = state.selectedProvider
        )

        nfcManager.activatePayment(paymentData)
        _uiState.value = _uiState.value.copy(isPaymentActive = true)
    }

    fun cancelPayment() {
        nfcManager.cancelPayment()
        _uiState.value = _uiState.value.copy(isPaymentActive = false, amount = "")
    }

    fun isNfcAvailable(): Boolean = nfcManager.isNfcAvailable()

    fun isAmountValid(): Boolean {
        val amount = _uiState.value.amount
        return amount.isNotEmpty() && _uiState.value.isConfigured &&
                amount.toDoubleOrNull()?.let { it > 0 } ?: false
    }
}
