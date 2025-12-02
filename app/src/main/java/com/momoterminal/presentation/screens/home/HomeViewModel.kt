package com.momoterminal.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.config.AppConfig
import com.momoterminal.data.local.MomoDatabase
import com.momoterminal.data.local.entity.TransactionEntity
import com.momoterminal.nfc.NfcManager
import com.momoterminal.nfc.NfcState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val nfcManager: NfcManager,
    private val database: MomoDatabase,
    private val appConfig: AppConfig
) : ViewModel() {
    
    /**
     * UI state for the Home screen.
     */
    data class HomeUiState(
        val isConfigured: Boolean = false,
        val merchantPhone: String = "",
        val recentTransactions: List<TransactionEntity> = emptyList(),
        val pendingCount: Int = 0,
        val nfcState: NfcState = NfcState.Ready,
        val isLoading: Boolean = true,
        // Analytics
        val todayRevenue: Double = 0.0,
        val todayTransactionCount: Int = 0,
        val weeklyRevenue: Double = 0.0,
        val successRate: Double = 0.0,
        val failedCount: Int = 0
    )
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // NFC state from manager
    val nfcState: StateFlow<NfcState> = nfcManager.nfcState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NfcState.Ready
        )
    
    init {
        loadData()
        observeTransactions()
        observePendingCount()
        loadAnalytics()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isConfigured = appConfig.isConfigured(),
                merchantPhone = appConfig.getMerchantPhone(),
                isLoading = false
            )
        }
    }
    
    private fun observeTransactions() {
        viewModelScope.launch {
            database.transactionDao().getRecentTransactions().collect { transactions ->
                _uiState.value = _uiState.value.copy(
                    recentTransactions = transactions
                )
            }
        }
    }
    
    private fun observePendingCount() {
        viewModelScope.launch {
            database.transactionDao().getPendingCount().collect { count ->
                _uiState.value = _uiState.value.copy(
                    pendingCount = count
                )
            }
        }
    }
    
    private fun loadAnalytics() {
        viewModelScope.launch {
            // Get today's start timestamp (midnight)
            val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % 86400000)
            
            // Get week start timestamp (7 days ago)
            val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            
            // Today's analytics
            val todayTransactions = database.transactionDao().getTransactionsByDateRange(todayStart, System.currentTimeMillis())
            val todaySuccessful = todayTransactions.filter { it.status == "completed" || it.status == "success" }
            val todayRevenue = todaySuccessful.sumOf { (it.amount ?: 0.0).toLong() }.toDouble()
            
            // Weekly analytics
            val weeklyTransactions = database.transactionDao().getTransactionsByDateRange(weekStart, System.currentTimeMillis())
            val weeklyRevenue = weeklyTransactions.filter { it.status == "completed" || it.status == "success" }.sumOf { (it.amount ?: 0.0).toLong() }.toDouble()
            
            // Success rate
            val totalTransactions = weeklyTransactions.size
            val successfulTransactions = weeklyTransactions.count { it.status == "completed" || it.status == "success" }
            val failedTransactions = weeklyTransactions.count { it.status == "failed" || it.status == "error" }
            val successRate = if (totalTransactions > 0) (successfulTransactions.toDouble() / totalTransactions) * 100 else 0.0
            
            _uiState.value = _uiState.value.copy(
                todayRevenue = todayRevenue,
                todayTransactionCount = todayTransactions.size,
                weeklyRevenue = weeklyRevenue,
                successRate = successRate,
                failedCount = failedTransactions
            )
        }
    }
    
    fun refreshAnalytics() {
        loadAnalytics()
    }
    
    fun refreshNfcState() {
        nfcManager.updateNfcState()
    }
    
    fun isNfcAvailable(): Boolean {
        return nfcManager.isNfcAvailable()
    }
    
    fun isNfcSupported(): Boolean {
        return nfcManager.isNfcSupported()
    }
}
