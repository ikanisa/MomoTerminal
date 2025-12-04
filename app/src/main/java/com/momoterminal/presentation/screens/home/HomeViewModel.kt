package com.momoterminal.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.core.database.dao.TransactionDao
import com.momoterminal.core.common.preferences.UserPreferences
import com.momoterminal.data.repository.CountryRepository
import com.momoterminal.data.repository.WalletRepository
import com.momoterminal.nfc.NfcManager
import com.momoterminal.nfc.NfcPaymentData
import com.momoterminal.nfc.NfcState
import com.momoterminal.offline.OfflineFirstManager
import com.momoterminal.offline.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val nfcManager: NfcManager,
    private val userPreferences: UserPreferences,
    private val countryRepository: CountryRepository,
    private val offlineFirstManager: OfflineFirstManager,
    private val transactionDao: TransactionDao,
    private val walletRepository: WalletRepository
) : ViewModel() {

    data class HomeUiState(
        val amount: String = "",
        val currency: String = "RWF",
        val currencySymbol: String = "FRw",
        val providerName: String = "MTN MoMo",
        val providerCode: String = "MTN",
        val countryCode: String = "RW",
        val merchantPhone: String = "",
        val isConfigured: Boolean = false,
        val isNfcEnabled: Boolean = true,
        val isNfcAvailable: Boolean = false,
        val walletBalance: Long = 0,
        val recentTransactionCount: Int = 0
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val nfcState: StateFlow<NfcState> = nfcManager.nfcState
    val syncState: StateFlow<SyncState> = offlineFirstManager.syncState
    val pendingCount: StateFlow<Int> = offlineFirstManager.pendingCount

    init {
        loadUserConfig()
        checkNfcAvailability()
        observeLocalData()
    }

    private fun loadUserConfig() {
        viewModelScope.launch {
            userPreferences.userPreferencesFlow.collect { prefs ->
                val country = countryRepository.getByCode(prefs.momoCountryCode)
                    ?: countryRepository.getCurrentCountry()

                _uiState.update {
                    it.copy(
                        merchantPhone = prefs.merchantPhone,
                        countryCode = prefs.momoCountryCode.ifEmpty { country.code },
                        currency = country.currency,
                        currencySymbol = country.currencySymbol,
                        providerName = country.providerName,
                        providerCode = country.providerCode,
                        isConfigured = prefs.merchantPhone.isNotBlank()
                    )
                }
            }
        }
    }

    private fun checkNfcAvailability() {
        _uiState.update { it.copy(isNfcAvailable = nfcManager.isNfcAvailable()) }
    }

    private fun observeLocalData() {
        // Observe wallet balance (offline-first: always available)
        viewModelScope.launch {
            userPreferences.userPreferencesFlow.flatMapLatest { prefs ->
                val userId = prefs.merchantPhone.ifBlank { prefs.phoneNumber.ifBlank { "default" } }
                if (userId.isNotBlank()) {
                    walletRepository.observeWallet(userId).map { it?.balance ?: 0 }
                } else {
                    flowOf(0L)
                }
            }.collect { balance ->
                _uiState.update { it.copy(walletBalance = balance) }
            }
        }

        // Observe recent transaction count
        viewModelScope.launch {
            transactionDao.getRecentTransactions(10).collect { transactions ->
                _uiState.update { it.copy(recentTransactionCount = transactions.size) }
            }
        }
    }

    fun onDigitClick(digit: String) {
        val currentAmount = _uiState.value.amount
        if (currentAmount.length < 10) {
            _uiState.update { it.copy(amount = currentAmount + digit) }
        }
    }

    fun onBackspaceClick() {
        val currentAmount = _uiState.value.amount
        if (currentAmount.isNotEmpty()) {
            _uiState.update { it.copy(amount = currentAmount.dropLast(1)) }
        }
    }

    fun onClearClick() {
        _uiState.update { it.copy(amount = "") }
    }

    fun toggleNfcEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isNfcEnabled = enabled) }
    }

    fun activatePayment() {
        val state = _uiState.value
        if (!isAmountValid() || !state.isNfcEnabled) return

        val amountValue = state.amount.toDoubleOrNull() ?: return

        val paymentData = NfcPaymentData.fromAmount(
            merchantPhone = state.merchantPhone,
            amount = amountValue,
            currency = state.currency,
            countryCode = state.countryCode,
            provider = NfcPaymentData.Provider.fromString(state.providerCode)
        )

        nfcManager.activatePayment(paymentData)
    }

    fun cancelPayment() {
        nfcManager.cancelPayment()
        _uiState.update { it.copy(amount = "") }
    }

    fun triggerSync() {
        offlineFirstManager.triggerImmediateSync()
    }

    fun isAmountValid(): Boolean {
        val amount = _uiState.value.amount.toDoubleOrNull() ?: return false
        return amount > 0
    }

    fun isNfcAvailable(): Boolean = _uiState.value.isNfcAvailable
}
