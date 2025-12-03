package com.momoterminal.presentation.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.auth.SessionManager
import com.momoterminal.core.error.MomoError
import com.momoterminal.core.ui.UiState
import com.momoterminal.data.repository.SmsRepository
import com.momoterminal.domain.model.TokenTransaction
import com.momoterminal.domain.model.TokenWallet
import com.momoterminal.domain.usecase.ApplyTokenTransactionUseCase
import com.momoterminal.domain.usecase.GetTokenBalanceUseCase
import com.momoterminal.domain.usecase.GetTokenHistoryUseCase
import com.momoterminal.offline.DataFreshness
import com.momoterminal.offline.FreshnessStatus
import com.momoterminal.offline.OfflineFirstManager
import com.momoterminal.offline.SyncState
import com.momoterminal.sms.SmsWalletIntegrationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val getTokenBalanceUseCase: GetTokenBalanceUseCase,
    private val getTokenHistoryUseCase: GetTokenHistoryUseCase,
    private val applyTokenTransactionUseCase: ApplyTokenTransactionUseCase,
    private val smsWalletIntegrationService: SmsWalletIntegrationService,
    private val smsRepository: SmsRepository,
    private val sessionManager: SessionManager,
    private val offlineFirstManager: OfflineFirstManager,
    private val dataFreshness: DataFreshness
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()
    
    // Expose sync state for UI
    val syncState: StateFlow<SyncState> = offlineFirstManager.syncState
    
    // Wallet freshness status
    val freshnessStatus: StateFlow<FreshnessStatus> = dataFreshness
        .getFreshnessStatus(DataFreshness.DataType.WALLET)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FreshnessStatus.FRESH)

    private val userId: String?
        get() = sessionManager.currentUserId

    init {
        loadWallet()
    }

    private fun loadWallet() {
        val uid = userId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(walletState = UiState.Loading) }
            
            // Observe wallet (offline-first: always available from local DB)
            getTokenBalanceUseCase.observeWallet(uid)
                .filterNotNull()
                .onEach { wallet ->
                    _uiState.update { 
                        it.copy(
                            walletState = UiState.Success(wallet),
                            wallet = wallet
                        ) 
                    }
                    loadTransactions(wallet.id)
                    dataFreshness.markFresh(DataFreshness.DataType.WALLET)
                }
                .catch { e ->
                    _uiState.update { 
                        it.copy(walletState = UiState.Error(MomoError.DatabaseError(e.message ?: "Unknown", e))) 
                    }
                }
                .launchIn(viewModelScope)
            
            // Observe unsynced count
            smsRepository.observeUnsyncedCount()
                .onEach { count ->
                    _uiState.update { it.copy(pendingSyncCount = count) }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun loadTransactions(walletId: String) {
        viewModelScope.launch {
            getTokenHistoryUseCase.observeTransactions(walletId)
                .onEach { transactions ->
                    _uiState.update { it.copy(transactions = transactions) }
                }
                .launchIn(viewModelScope)
        }
    }

    fun processUncreditedSms() {
        val uid = userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val count = smsWalletIntegrationService.processUncreditedTransactions(uid)
            _uiState.update { 
                it.copy(
                    isProcessing = false,
                    message = if (count > 0) "Credited $count transactions" else "No pending credits"
                )
            }
        }
    }

    fun triggerSync() {
        offlineFirstManager.triggerImmediateSync()
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            dataFreshness.invalidate(DataFreshness.DataType.WALLET)
        }
        loadWallet()
        triggerSync()
    }
}

data class WalletUiState(
    val wallet: TokenWallet? = null,
    val walletState: UiState<TokenWallet> = UiState.Loading,
    val transactions: List<TokenTransaction> = emptyList(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val pendingSyncCount: Int = 0,
    val message: String? = null,
    val error: String? = null
)
