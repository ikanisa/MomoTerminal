package com.momoterminal.presentation.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.momoterminal.domain.model.Transaction
import com.momoterminal.domain.model.TransactionFilter
import com.momoterminal.domain.repository.TransactionRepository
import com.momoterminal.sync.SyncManager
import com.momoterminal.sync.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import com.momoterminal.data.local.MomoDatabase
import com.momoterminal.data.local.entity.TransactionEntity
import com.momoterminal.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Transactions screen.
 * Handles pagination, filtering, and sync operations.
 */
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val syncManager: SyncManager
) : ViewModel() {
    
    // Filter state
    private val _filter = MutableStateFlow(TransactionFilter())
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()
    
    // Search query (separate from filter for real-time updates)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Whether the filter sheet is shown
    private val _showFilterSheet = MutableStateFlow(false)
    val showFilterSheet: StateFlow<Boolean> = _showFilterSheet.asStateFlow()
    
    // Sync state from SyncManager
    val syncState: StateFlow<SyncState> = syncManager.syncState
    
    // Pending transaction count
    val pendingCount: StateFlow<Int> = transactionRepository.getPendingCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    // Transaction count matching current filter
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredTransactionCount: StateFlow<Int> = _filter
        .flatMapLatest { filter ->
            transactionRepository.getFilteredTransactionCount(filter)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    // Paginated transactions - reacts to filter changes
    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: Flow<PagingData<Transaction>> = _filter
        .flatMapLatest { filter ->
            transactionRepository.getTransactionsPaged(filter)
        }
        .cachedIn(viewModelScope)
    
    /**
     * Update the search query.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _filter.value = _filter.value.withSearchQuery(query.takeIf { it.isNotBlank() })
    }
    
    /**
     * Update the filter.
     */
    fun updateFilter(newFilter: TransactionFilter) {
        _filter.value = newFilter
    }
    
    /**
     * Clear all filters.
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _filter.value = TransactionFilter()
    }
    
    /**
     * Show the filter sheet.
     */
    fun showFilterSheet() {
        _showFilterSheet.value = true
    }
    
    /**
     * Hide the filter sheet.
     */
    fun hideFilterSheet() {
        _showFilterSheet.value = false
    }
    
    /**
     * Trigger a manual sync.
     */
    fun triggerSync() {
        syncManager.triggerManualSync()
    }
    
    /**
     * Check if device is online.
     */
    fun isOnline(): Boolean = syncManager.isOnline()
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
