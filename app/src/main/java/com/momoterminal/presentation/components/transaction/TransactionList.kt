package com.momoterminal.presentation.components.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.R
import com.momoterminal.data.local.entity.TransactionEntity
import com.momoterminal.presentation.theme.MomoTerminalTheme

/**
 * List of transactions with pull-to-refresh support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionList(
    transactions: List<TransactionEntity>,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val pullToRefreshState = rememberPullToRefreshState()
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        state = pullToRefreshState
    ) {
        if (transactions.isEmpty()) {
            EmptyTransactionList(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = transactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) }
                    )
                }
            }
        }
    }
}

/**
 * Empty state for transaction list.
 */
@Composable
private fun EmptyTransactionList(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Inbox,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.no_transactions_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.no_transactions_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionListPreview() {
    val sampleTransactions = listOf(
        TransactionEntity(
            id = 1,
            sender = "MTN Mobile Money",
            body = "You have received GHS 500.00 from John Doe",
            timestamp = System.currentTimeMillis() - 3_600_000,
            status = "SENT"
        ),
        TransactionEntity(
            id = 2,
            sender = "Vodafone Cash",
            body = "You have sent GHS 1000.00 to Jane Doe",
            timestamp = System.currentTimeMillis() - 60_000,
            status = "PENDING"
        ),
        TransactionEntity(
            id = 3,
            sender = "AirtelTigo Money",
            body = "Transaction failed: GHS 250.00",
            timestamp = System.currentTimeMillis() - 86_400_000,
            status = "FAILED"
        )
    )
    
    MomoTerminalTheme {
        TransactionList(
            transactions = sampleTransactions,
            onTransactionClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyTransactionListPreview() {
    MomoTerminalTheme {
        TransactionList(
            transactions = emptyList(),
            onTransactionClick = {}
        )
    }
}
