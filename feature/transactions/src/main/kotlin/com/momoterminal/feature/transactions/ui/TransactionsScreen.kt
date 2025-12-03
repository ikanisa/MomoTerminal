package com.momoterminal.feature.transactions.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.core.domain.model.Transaction
import com.momoterminal.feature.transactions.viewmodel.TransactionsEvent
import com.momoterminal.feature.transactions.viewmodel.TransactionsViewModel

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading && state.transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.transactions) { transaction ->
                    TransactionItem(transaction)
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${transaction.amount} ${transaction.currency}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = transaction.reference,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = transaction.status.name,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
