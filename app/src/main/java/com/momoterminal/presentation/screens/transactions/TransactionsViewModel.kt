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
 * Handles pagination, filtering, and sync operations.
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
        val pendingCount: Int = 0,
        val dateRangeStart: Long? = null,
        val dateRangeEnd: Long? = null,
        val showDatePicker: Boolean = false
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
    
    fun setDateRange(startMillis: Long?, endMillis: Long?) {
        _uiState.value = _uiState.value.copy(
            dateRangeStart = startMillis,
            dateRangeEnd = endMillis
        )
    }
    
    fun clearDateRange() {
        _uiState.value = _uiState.value.copy(
            dateRangeStart = null,
            dateRangeEnd = null
        )
    }
    
    fun showDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = true)
    }
    
    fun hideDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }
    
    fun getFilteredTransactions(
        transactions: List<TransactionEntity>,
        filter: TransactionFilter,
        dateRangeStart: Long? = _uiState.value.dateRangeStart,
        dateRangeEnd: Long? = _uiState.value.dateRangeEnd
    ): List<TransactionEntity> {
        var filtered = when (filter) {
            TransactionFilter.ALL -> transactions
            TransactionFilter.PENDING -> transactions.filter { it.status == "PENDING" }
            TransactionFilter.SENT -> transactions.filter { it.status == "SENT" }
            TransactionFilter.FAILED -> transactions.filter { it.status == "FAILED" }
        }
        
        // Apply date range filter
        if (dateRangeStart != null && dateRangeEnd != null) {
            filtered = filtered.filter { transaction ->
                transaction.timestamp >= dateRangeStart && transaction.timestamp <= dateRangeEnd
            }
        }
        
        return filtered
    }
}
