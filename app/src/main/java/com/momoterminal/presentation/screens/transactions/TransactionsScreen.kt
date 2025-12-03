package com.momoterminal.presentation.screens.transactions

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.momoterminal.R
import com.momoterminal.presentation.components.common.MomoTopAppBar
import com.momoterminal.presentation.components.status.SyncStatusBadge
import com.momoterminal.presentation.components.transaction.TransactionList
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MomoYellow


/**
 * Transactions screen showing transaction history.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    
    // Check SMS permission
    val smsPermissionState = rememberPermissionState(Manifest.permission.RECEIVE_SMS)
    
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
                onNavigationClick = onNavigateBack,
                actions = {
                    SyncStatusBadge(
                        pendingCount = uiState.pendingCount,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // SMS Permission Warning
            AnimatedVisibility(visible = !smsPermissionState.status.isGranted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sms_access_required),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.sms_access_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { smsPermissionState.launchPermissionRequest() }
                        ) {
                            Text(
                                stringResource(R.string.grant_permission),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Filter chips
            FilterChipsRow(
                selectedFilter = uiState.filter,
                onFilterSelected = viewModel::setFilter,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Date Range Filter Chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val startDate = uiState.dateRangeStart
                val endDate = uiState.dateRangeEnd
                
                if (startDate != null && endDate != null) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.clearDateRange() },
                        label = {
                            Text(
                                "${formatDate(startDate)} - ${formatDate(endDate)}"
                            )
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
                        onClick = { viewModel.showDatePicker() },
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
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = filter == selectedFilter,
                    borderColor = if (filter == selectedFilter) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }
    }
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


private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
