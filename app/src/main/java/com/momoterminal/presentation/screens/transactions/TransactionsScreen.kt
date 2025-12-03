package com.momoterminal.presentation.screens.transactions

import android.Manifest
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.momoterminal.R
import com.momoterminal.presentation.components.common.MomoTopAppBar
import com.momoterminal.presentation.components.transaction.TransactionList
import com.momoterminal.presentation.theme.MomoAnimation
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MomoYellow


/**
 * Clean Transactions/History screen showing transaction history.
 * Features:
 * - Clean filter chips for transaction status
 * - Date range filtering
 * - SMS permission banner (optional, not required)
 * - Empty state with helpful message
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
    
    // Check SMS permission - note: SMS permission is OPTIONAL
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
                title = stringResource(R.string.nav_history),
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
            // Stats Summary Card
            if (transactions.isNotEmpty()) {
                TransactionSummaryCard(
                    totalCount = transactions.size,
                    pendingCount = uiState.pendingCount,
                    todayRevenue = uiState.todayRevenue,
                    currency = uiState.currency,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Filter Section
            FilterSection(
                selectedFilter = uiState.filter,
                onFilterSelected = viewModel::setFilter,
                dateRangeStart = uiState.dateRangeStart,
                dateRangeEnd = uiState.dateRangeEnd,
                onClearDateRange = viewModel::clearDateRange,
                onShowDatePicker = viewModel::showDatePicker,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // SMS Permission Info (optional, dismissable)
            AnimatedVisibility(
                visible = !smsPermissionState.status.isGranted && uiState.showSmsPermissionHint,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(MomoAnimation.DURATION_MEDIUM)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(MomoAnimation.DURATION_FAST)
                ) + fadeOut()
            ) {
                SmsPermissionInfoCard(
                    onRequestPermission = { smsPermissionState.launchPermissionRequest() },
                    onDismiss = viewModel::dismissSmsHint,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            
            // Transaction list or empty state
            if (filteredTransactions.isEmpty() && !uiState.isRefreshing) {
                EmptyTransactionsState(
                    hasFilters = uiState.filter != TransactionsViewModel.TransactionFilter.ALL ||
                            uiState.dateRangeStart != null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
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
}

/**
 * Summary card showing transaction statistics.
 */
@Composable
private fun TransactionSummaryCard(
    totalCount: Int,
    pendingCount: Int,
    todayRevenue: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Total transactions
            StatItem(
                value = totalCount.toString(),
                label = stringResource(R.string.transactions)
            )
            
            // Pending count
            if (pendingCount > 0) {
                StatItem(
                    value = pendingCount.toString(),
                    label = stringResource(R.string.pending),
                    valueColor = MomoYellow
                )
            }
            
            // Today's revenue
            if (todayRevenue > 0) {
                StatItem(
                    value = "${currency} ${formatAmount(todayRevenue)}",
                    label = stringResource(R.string.today)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Filter section with chips and date range.
 */
@Composable
private fun FilterSection(
    selectedFilter: TransactionsViewModel.TransactionFilter,
    onFilterSelected: (TransactionsViewModel.TransactionFilter) -> Unit,
    dateRangeStart: Long?,
    dateRangeEnd: Long?,
    onClearDateRange: () -> Unit,
    onShowDatePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
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
                            ),
                            style = MaterialTheme.typography.labelMedium
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
                        borderColor = if (filter == selectedFilter) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Date range chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            if (dateRangeStart != null && dateRangeEnd != null) {
                FilterChip(
                    selected = true,
                    onClick = onClearDateRange,
                    label = {
                        Text(
                            "${formatDate(dateRangeStart)} - ${formatDate(dateRangeEnd)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear_date_range),
                            modifier = Modifier.size(16.dp)
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
                    label = { 
                        Text(
                            stringResource(R.string.select_date_range),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

/**
 * SMS permission info card - optional, dismissable.
 */
@Composable
private fun SmsPermissionInfoCard(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.sms_permission_optional),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.sms_permission_optional_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.dismiss),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Empty state when no transactions match the filters.
 */
@Composable
private fun EmptyTransactionsState(
    hasFilters: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_transactions_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasFilters) {
                stringResource(R.string.no_transactions_with_filters)
            } else {
                stringResource(R.string.no_transactions_description)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

private fun formatAmount(amount: Double): String {
    return if (amount >= 1000000) {
        String.format("%.1fM", amount / 1000000)
    } else if (amount >= 1000) {
        String.format("%.1fK", amount / 1000)
    } else {
        String.format("%.0f", amount)
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
