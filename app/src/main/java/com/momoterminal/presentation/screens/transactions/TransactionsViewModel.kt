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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
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
    val filteredTransactionCount: StateFlow<Int> = transactionRepository
        .getFilteredTransactionCount(_filter.value)
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
}
