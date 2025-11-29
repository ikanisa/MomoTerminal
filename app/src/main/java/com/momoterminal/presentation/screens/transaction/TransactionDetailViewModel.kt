package com.momoterminal.presentation.screens.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.data.local.dao.TransactionDao
import com.momoterminal.data.local.entity.TransactionEntity
import com.momoterminal.domain.model.Provider
import com.momoterminal.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Transaction Detail screen.
 */
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionDao: TransactionDao,
    private val syncManager: SyncManager
) : ViewModel() {
    
    private val transactionId: Long = savedStateHandle.get<Long>("transactionId") ?: 0L
    
    /**
     * UI state for the Transaction Detail screen.
     */
    data class TransactionDetailUiState(
        val isLoading: Boolean = true,
        val transaction: TransactionEntity? = null,
        val provider: Provider? = null,
        val isRawMessageExpanded: Boolean = false,
        val isSyncing: Boolean = false,
        val syncError: String? = null,
        val showCopiedMessage: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadTransaction()
    }
    
    private fun loadTransaction() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                transactionDao.getTransactionById(transactionId)?.let { transaction ->
                    val provider = Provider.fromSender(transaction.sender)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transaction = transaction,
                        provider = provider
                    )
                } ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transaction = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    syncError = "Failed to load transaction: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Toggle the raw message expanded state.
     */
    fun toggleRawMessageExpanded() {
        _uiState.value = _uiState.value.copy(
            isRawMessageExpanded = !_uiState.value.isRawMessageExpanded
        )
    }
    
    /**
     * Retry syncing a failed transaction.
     */
    fun retrySync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                syncError = null
            )
            
            try {
                syncManager.enqueueSyncNow()
                
                // Wait a bit for the sync to start
                kotlinx.coroutines.delay(500)
                
                // Reload to get updated status
                loadTransaction()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncError = "Sync failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Called when transaction ID is copied.
     */
    fun onTransactionIdCopied() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showCopiedMessage = true)
            kotlinx.coroutines.delay(2000)
            _uiState.value = _uiState.value.copy(showCopiedMessage = false)
        }
    }
    
    /**
     * Get a shareable text representation of the transaction.
     */
    fun getShareableText(): String {
        val transaction = _uiState.value.transaction ?: return ""
        val provider = _uiState.value.provider
        
        return buildString {
            appendLine("MomoTerminal Transaction Details")
            appendLine("================================")
            appendLine()
            transaction.amount?.let { amount ->
                val currency = transaction.currency ?: "GHS"
                appendLine("Amount: $currency ${"%.2f".format(amount)}")
            }
            appendLine("From: ${transaction.sender}")
            transaction.transactionId?.let { txId ->
                appendLine("Transaction ID: $txId")
            }
            appendLine("Status: ${transaction.status}")
            provider?.let {
                appendLine("Provider: ${it.displayName}")
            }
            appendLine("Time: ${formatTimestamp(transaction.timestamp)}")
            appendLine()
            appendLine("Message:")
            appendLine(transaction.body)
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
