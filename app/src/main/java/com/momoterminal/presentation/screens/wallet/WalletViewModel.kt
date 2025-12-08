package com.momoterminal.presentation.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.core.common.preferences.UserPreferences
import com.momoterminal.core.database.dao.TransactionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val transactionDao: TransactionDao
) : ViewModel() {

    data class WalletUiState(
        val balance: Long = 0,
        val currency: String = "FRW",
        val merchantPhone: String = "",
        val recentTransactions: List<WalletTransaction> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        loadWalletData()
    }

    private fun loadWalletData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                userPreferences.userPreferencesFlow.collect { prefs ->
                    _uiState.update { 
                        it.copy(
                            merchantPhone = prefs.merchantPhone,
                            currency = "FRW"
                        )
                    }
                }
                
                loadWalletBalance()
                loadRecentTransactions()
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to load wallet data")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private fun loadWalletBalance() {
        viewModelScope.launch {
            _uiState.update { it.copy(balance = 0, isLoading = false) }
        }
    }

    private fun loadRecentTransactions() {
        viewModelScope.launch {
            val mockTransactions = emptyList<WalletTransaction>()
            _uiState.update { it.copy(recentTransactions = mockTransactions) }
        }
    }

    fun initiateTopUp(amount: Long) {
        viewModelScope.launch {
            try {
                Timber.d("Initiating top-up for amount: $amount")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initiate top-up")
            }
        }
    }

    fun generateTopUpUssd(amount: Long): String {
        val state = _uiState.value
        val phone = state.merchantPhone.ifEmpty { "250782123456" }
        val ussdCode = "*182*8*1*$phone*$amount#"
        Timber.d("Generated USSD: $ussdCode")
        return ussdCode
    }

    fun refresh() {
        loadWalletData()
    }
}

data class WalletTransaction(
    val id: String,
    val type: TransactionType,
    val description: String,
    val amount: Long,
    val timestamp: String,
    val status: String = "COMPLETED"
)

enum class TransactionType {
    TOP_UP, PAYMENT
}
