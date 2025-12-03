package com.momoterminal.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.config.AppConfig
import com.momoterminal.config.SupportedCountries
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
        val countryName: String = "Rwanda",
        val providerName: String = "MTN",
        val providerDisplayName: String = "MTN MoMo",
        val isPaymentActive: Boolean = false,
        // NFC Writer toggle
        val isNfcWriterEnabled: Boolean = true
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
        val countryCode = appConfig.getCountryCode()
        val country = SupportedCountries.getByCode(countryCode)
        val providerName = appConfig.getMomoProvider()
        
        _uiState.value = _uiState.value.copy(
            isConfigured = appConfig.isConfigured(),
            merchantPhone = appConfig.getMerchantPhone(),
            currency = appConfig.getCurrency(),
            countryCode = countryCode,
            countryName = country?.name ?: "Rwanda",
            providerName = providerName,
            providerDisplayName = getProviderDisplayName(providerName),
            isNfcWriterEnabled = appConfig.isNfcWriterEnabled()
        )
    }
    
    private fun getProviderDisplayName(providerCode: String): String {
        return when (providerCode.uppercase()) {
            "MTN" -> "MTN MoMo"
            "AIRTEL" -> "Airtel Money"
            "VODACOM" -> "M-Pesa"
            "VODAFONE" -> "Vodafone Cash"
            "ORANGE" -> "Orange Money"
            "TIGO" -> "Tigo Pesa"
            "WAVE" -> "Wave"
            "MOOV" -> "Moov Money"
            "ECOCASH" -> "EcoCash"
            "TMONEY" -> "T-Money"
            "MVOLA" -> "MVola"
            else -> providerCode
        }
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
        android.util.Log.d("HomeViewModel", "onDigitClick: $digit")
        val current = _uiState.value.amount
        if (digit == "0" && current.isEmpty()) return
        if (current.length >= MAX_AMOUNT_LENGTH) return
        _uiState.value = _uiState.value.copy(amount = current + digit)
        android.util.Log.d("HomeViewModel", "Amount now: ${_uiState.value.amount}")
    }

    fun onBackspaceClick() {
        android.util.Log.d("HomeViewModel", "onBackspaceClick")
        val current = _uiState.value.amount
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(amount = current.dropLast(1))
        }
    }

    fun onClearClick() {
        android.util.Log.d("HomeViewModel", "onClearClick")
        _uiState.value = _uiState.value.copy(amount = "")
    }

    fun toggleNfcWriter(enabled: Boolean) {
        appConfig.setNfcWriterEnabled(enabled)
        _uiState.value = _uiState.value.copy(isNfcWriterEnabled = enabled)
    }

    fun activatePayment() {
        val state = _uiState.value
        if (state.amount.isEmpty() || state.merchantPhone.isEmpty()) return
        if (!state.isNfcWriterEnabled) return

        val amountDouble = state.amount.toDoubleOrNull() ?: return
        val amountInMinorUnits = (amountDouble * 100).toLong()
        
        // Get provider from country configuration
        val provider = NfcPaymentData.Provider.fromString(state.providerName)

        val paymentData = NfcPaymentData(
            merchantPhone = state.merchantPhone,
            amountInMinorUnits = amountInMinorUnits,
            currency = state.currency,
            countryCode = state.countryCode,
            provider = provider
        )

        nfcManager.activatePayment(paymentData)
        _uiState.value = _uiState.value.copy(isPaymentActive = true)
    }

    fun cancelPayment() {
        nfcManager.cancelPayment()
        _uiState.value = _uiState.value.copy(isPaymentActive = false, amount = "")
    }

    fun isNfcAvailable(): Boolean = nfcManager.isNfcAvailable() && _uiState.value.isNfcWriterEnabled

    fun isAmountValid(): Boolean {
        val amount = _uiState.value.amount
        return amount.isNotEmpty() && _uiState.value.isConfigured &&
                amount.toDoubleOrNull()?.let { it > 0 } ?: false
    }
}
