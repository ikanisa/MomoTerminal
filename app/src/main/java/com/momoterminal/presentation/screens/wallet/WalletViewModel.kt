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

    data class TopUpResult(
        val success: Boolean,
        val ussdCode: String? = null,
        val errorMessage: String? = null
    )

    fun validateAndInitiateTopUp(amount: Long): TopUpResult {
        val state = _uiState.value
        
        // Validate mobile money number is set
        if (state.merchantPhone.isBlank()) {
            _uiState.update { it.copy(error = "Mobile Money number not set. Please add it in Settings.") }
            return TopUpResult(
                success = false,
                errorMessage = "Mobile Money number not set. Please add it in Settings."
            )
        }
        
        // Validate amount range
        if (amount !in 100..4000) {
            _uiState.update { it.copy(error = "Amount must be between 100 and 4,000 FRW") }
            return TopUpResult(
                success = false,
                errorMessage = "Amount must be between 100 and 4,000 FRW"
            )
        }
        
        viewModelScope.launch {
            try {
                Timber.d("Initiating top-up for amount: $amount to ${state.merchantPhone}")
                // Clear any previous errors
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initiate top-up")
                _uiState.update { it.copy(error = e.message) }
            }
        }
        
        val ussdCode = generateTopUpUssd(amount)
        return TopUpResult(success = true, ussdCode = ussdCode)
    }

    private fun generateTopUpUssd(amount: Long): String {
        val state = _uiState.value
        val phone = state.merchantPhone
        // MTN Rwanda top-up USSD: *182*8*1*merchantPhone*amount#
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
