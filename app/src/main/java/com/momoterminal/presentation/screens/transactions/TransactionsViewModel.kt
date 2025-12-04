package com.momoterminal.presentation.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.momoterminal.core.common.config.AppConfig
import com.momoterminal.data.local.MomoDatabase
import com.momoterminal.core.database.entity.TransactionEntity
import com.momoterminal.data.repository.TransactionPagingRepository
import com.momoterminal.domain.model.TransactionFilter
import com.momoterminal.domain.model.SyncStatus
import com.momoterminal.offline.OfflineFirstManager
import com.momoterminal.offline.SyncState
import com.momoterminal.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Transactions/History screen.
 * Uses Paging 3 for efficient infinite scrolling of large SMS histories.
 */
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val database: MomoDatabase,
    private val syncManager: SyncManager,
    private val appConfig: AppConfig,
    private val pagingRepository: TransactionPagingRepository,
    private val offlineFirstManager: OfflineFirstManager
) : ViewModel() {
    
    /**
     * Filter options for transactions.
     */
    enum class FilterType {
        ALL, PENDING, SENT, FAILED
    }
    
    /**
     * UI state for the Transactions screen.
     */
    data class TransactionsUiState(
        val filterType: FilterType = FilterType.ALL,
        val searchQuery: String = "",
        val isRefreshing: Boolean = false,
        val pendingCount: Int = 0,
        val dateRangeStart: Long? = null,
        val dateRangeEnd: Long? = null,
        val showDatePicker: Boolean = false,
        val showSmsPermissionHint: Boolean = true,
        val todayRevenue: Double = 0.0,
        val currency: String = "RWF",
        val totalCount: Int = 0
    )
    
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()
    
    // Sync state from OfflineFirstManager
    val syncState: StateFlow<SyncState> = offlineFirstManager.syncState
    
    // Filter state for Paging
    private val _filter = MutableStateFlow(TransactionFilter())
    
    /**
     * Paginated transactions using Paging 3.
     * Automatically invalidates when filter changes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedTransactions: Flow<PagingData<TransactionEntity>> = _filter
        .flatMapLatest { filter ->
            pagingRepository.getFilteredTransactions(filter)
        }
        .cachedIn(viewModelScope)
    
    // Non-paged recent transactions for quick display
    val recentTransactions: StateFlow<List<TransactionEntity>> = database.transactionDao()
        .getRecentTransactions(50)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        loadCurrency()
        observePendingCount()
        observeFilteredCount()
        loadTodayStats()
    }
    
    private fun loadCurrency() {
        _uiState.value = _uiState.value.copy(
            currency = appConfig.getCurrency()
        )
    }
    
    private fun observePendingCount() {
        viewModelScope.launch {
            database.transactionDao().getPendingCount().collect { count ->
                _uiState.value = _uiState.value.copy(pendingCount = count)
            }
        }
    }
    
    private fun observeFilteredCount() {
        viewModelScope.launch {
            _filter.flatMapLatest { filter ->
                pagingRepository.getFilteredCount(filter)
            }.collect { count ->
                _uiState.value = _uiState.value.copy(totalCount = count)
            }
        }
    }
    
    private fun loadTodayStats() {
        viewModelScope.launch {
            val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % 86400000)
            val transactions = database.transactionDao()
                .getTransactionsByDateRange(todayStart, System.currentTimeMillis())
            val successful = transactions.filter { 
                it.status == "completed" || it.status == "success" || it.status == "SENT" 
            }

            _uiState.value = _uiState.value.copy(
                todayRevenue = successful.sumOf { (it.amount ?: 0.0).toLong() }.toDouble()
            )
        }
    }
    
    fun setFilter(filterType: FilterType) {
        _uiState.value = _uiState.value.copy(filterType = filterType)
        updateFilter()
    }
    
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        updateFilter()
    }
    
    fun setDateRange(startMillis: Long?, endMillis: Long?) {
        _uiState.value = _uiState.value.copy(
            dateRangeStart = startMillis,
            dateRangeEnd = endMillis
        )
        updateFilter()
    }
    
    private fun updateFilter() {
        val state = _uiState.value
        _filter.value = TransactionFilter(
            status = when (state.filterType) {
                FilterType.ALL -> null
                FilterType.PENDING -> SyncStatus.PENDING
                FilterType.SENT -> SyncStatus.SENT
                FilterType.FAILED -> SyncStatus.FAILED
            },
            searchQuery = state.searchQuery.takeIf { it.isNotBlank() },
            startDate = state.dateRangeStart?.let { java.util.Date(it) },
            endDate = state.dateRangeEnd?.let { java.util.Date(it) }
        )
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            // Trigger sync
            offlineFirstManager.triggerImmediateSync()
            syncManager.enqueueSyncNow()
            
            // Reload today stats
            loadTodayStats()
            
            // Small delay to show refresh indicator
            kotlinx.coroutines.delay(1000)
            
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }
    
    fun clearDateRange() {
        _uiState.value = _uiState.value.copy(
            dateRangeStart = null,
            dateRangeEnd = null
        )
        updateFilter()
    }
    
    fun showDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = true)
    }
    
    fun hideDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }
    
    fun dismissSmsHint() {
        _uiState.value = _uiState.value.copy(showSmsPermissionHint = false)
    }
    
    // Legacy method for non-paged filtering
    fun getFilteredTransactions(
        transactions: List<TransactionEntity>,
        filter: FilterType,
        dateRangeStart: Long? = _uiState.value.dateRangeStart,
        dateRangeEnd: Long? = _uiState.value.dateRangeEnd
    ): List<TransactionEntity> {
        var filtered = when (filter) {
            FilterType.ALL -> transactions
            FilterType.PENDING -> transactions.filter { it.status == "PENDING" }
            FilterType.SENT -> transactions.filter { it.status == "SENT" }
            FilterType.FAILED -> transactions.filter { it.status == "FAILED" }
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
