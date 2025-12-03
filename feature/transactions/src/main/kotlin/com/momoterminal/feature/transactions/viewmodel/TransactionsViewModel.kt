package com.momoterminal.feature.transactions.viewmodel

import androidx.lifecycle.viewModelScope
import com.momoterminal.core.common.Result
import com.momoterminal.core.common.UiEffect
import com.momoterminal.core.common.UiEvent
import com.momoterminal.core.common.UiState
import com.momoterminal.core.domain.model.Transaction
import com.momoterminal.core.domain.repository.TransactionRepository
import com.momoterminal.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : BaseViewModel<TransactionsState, TransactionsEvent, TransactionsEffect>(TransactionsState()) {

    init {
        loadTransactions()
    }

    override fun onEvent(event: TransactionsEvent) {
        when (event) {
            TransactionsEvent.Refresh -> loadTransactions(forceRefresh = true)
            is TransactionsEvent.LoadMore -> loadMore()
        }
    }

    private fun loadTransactions(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            transactionRepository.getTransactions(0, 20).collect { result ->
                when (result) {
                    is Result.Loading -> updateState { copy(isLoading = true) }
                    is Result.Success -> updateState {
                        copy(
                            isLoading = false,
                            transactions = result.data.items,
                            error = null
                        )
                    }
                    is Result.Error -> updateState {
                        copy(isLoading = false, error = result.exception.message)
                    }
                }
            }
        }
    }

    private fun loadMore() {
        // Pagination logic
    }
}

data class TransactionsState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) : UiState

sealed class TransactionsEvent : UiEvent {
    data object Refresh : TransactionsEvent()
    data object LoadMore : TransactionsEvent()
}

sealed class TransactionsEffect : UiEffect
