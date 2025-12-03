package com.momoterminal.presentation.screens.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.R
import com.momoterminal.presentation.components.common.MomoTopAppBar
import com.momoterminal.presentation.components.transaction.TransactionList
import com.momoterminal.presentation.theme.MomoAnimation
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MomoYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Transaction history screen.
 * Clean, focused UI without SMS permission prompts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    
    val filteredTransactions = viewModel.getFilteredTransactions(
        transactions = transactions,
        filter = uiState.filter,
        dateRangeStart = uiState.dateRangeStart,
        dateRangeEnd = uiState.dateRangeEnd
    )
    
    Scaffold(
        topBar = {
            MomoTopAppBar(
                title = stringResource(R.string.transactions_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            FilterChipsRow(
                selectedFilter = uiState.filter,
                onFilterSelected = viewModel::setFilter,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Date Range Filter
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(MomoAnimation.DURATION_MEDIUM)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(MomoAnimation.DURATION_FAST)
                ) + fadeOut()
            ) {
                DateRangeChip(
                    dateRangeStart = uiState.dateRangeStart,
                    dateRangeEnd = uiState.dateRangeEnd,
                    onClearDateRange = viewModel::clearDateRange,
                    onShowDatePicker = viewModel::showDatePicker,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            
            // Transaction list
            TransactionList(
                transactions = filteredTransactions,
                onTransactionClick = { transaction ->
                    onTransactionClick(transaction.id)
                },
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: TransactionsViewModel.TransactionFilter,
    onFilterSelected: (TransactionsViewModel.TransactionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransactionsViewModel.TransactionFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = stringResource(
                            when (filter) {
                                TransactionsViewModel.TransactionFilter.ALL -> R.string.filter_all
                                TransactionsViewModel.TransactionFilter.PENDING -> R.string.filter_pending
                                TransactionsViewModel.TransactionFilter.SENT -> R.string.filter_sent
                                TransactionsViewModel.TransactionFilter.FAILED -> R.string.filter_failed
                            }
                        )
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MomoYellow,
                    selectedLabelColor = Color.Black,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = filter == selectedFilter,
                    borderColor = if (filter == selectedFilter) 
                        Color.Transparent 
                    else 
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun DateRangeChip(
    dateRangeStart: Long?,
    dateRangeEnd: Long?,
    onClearDateRange: () -> Unit,
    onShowDatePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (dateRangeStart != null && dateRangeEnd != null) {
            FilterChip(
                selected = true,
                onClick = onClearDateRange,
                label = {
                    Text("${formatDate(dateRangeStart)} - ${formatDate(dateRangeEnd)}")
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear_date_range)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        } else {
            FilterChip(
                selected = false,
                onClick = onShowDatePicker,
                label = { Text(stringResource(R.string.select_date_range)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
private fun TransactionsScreenPreview() {
    MomoTerminalTheme {
        TransactionsScreen(
            onNavigateBack = {},
            onTransactionClick = {}
        )
    }
}
