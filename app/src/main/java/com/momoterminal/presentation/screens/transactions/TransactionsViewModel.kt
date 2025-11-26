package com.momoterminal.presentation.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.data.local.MomoDatabase
import com.momoterminal.data.local.entity.TransactionEntity
import com.momoterminal.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Transactions screen.
 */
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val database: MomoDatabase,
    private val syncManager: SyncManager
) : ViewModel() {
    
    /**
     * Filter options for transactions.
     */
    enum class TransactionFilter {
        ALL, PENDING, SENT, FAILED
    }
    
    /**
     * UI state for the Transactions screen.
     */
    data class TransactionsUiState(
        val filter: TransactionFilter = TransactionFilter.ALL,
        val isRefreshing: Boolean = false,
        val pendingCount: Int = 0
    )
    
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()
    
    // All transactions from database
    val transactions: StateFlow<List<TransactionEntity>> = database.transactionDao()
        .getRecentTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        observePendingCount()
    }
    
    private fun observePendingCount() {
        viewModelScope.launch {
            database.transactionDao().getPendingCount().collect { count ->
                _uiState.value = _uiState.value.copy(pendingCount = count)
            }
        }
    }
    
    fun setFilter(filter: TransactionFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            // Trigger sync
            syncManager.enqueueSyncNow()
            
            // Small delay to show refresh indicator
            kotlinx.coroutines.delay(1000)
            
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }
    
    fun getFilteredTransactions(
        transactions: List<TransactionEntity>,
        filter: TransactionFilter
    ): List<TransactionEntity> {
        return when (filter) {
            TransactionFilter.ALL -> transactions
            TransactionFilter.PENDING -> transactions.filter { it.status == "PENDING" }
            TransactionFilter.SENT -> transactions.filter { it.status == "SENT" }
            TransactionFilter.FAILED -> transactions.filter { it.status == "FAILED" }
        }
    }
}
