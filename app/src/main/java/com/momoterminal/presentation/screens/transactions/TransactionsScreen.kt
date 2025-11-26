package com.momoterminal.presentation.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.momoterminal.domain.model.Transaction
import com.momoterminal.presentation.components.animations.EmptyStateAnimation
import com.momoterminal.presentation.components.error.ErrorCard
import com.momoterminal.presentation.components.status.SyncStatusBadge
import com.momoterminal.presentation.components.transaction.TransactionCard
import com.momoterminal.presentation.components.transaction.TransactionCardPlaceholder
import com.momoterminal.presentation.components.transaction.TransactionFilterSheet
import com.momoterminal.error.AppError
import com.momoterminal.error.ErrorHandler
import com.momoterminal.sync.SyncState

/**
 * Main transactions screen showing paginated transaction list.
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.presentation.components.common.MomoTopAppBar
import com.momoterminal.presentation.components.status.SyncStatusBadge
import com.momoterminal.presentation.components.transaction.TransactionList
import com.momoterminal.presentation.theme.MomoTerminalTheme

/**
 * Transactions screen showing transaction history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = hiltViewModel(),
    onTransactionClick: (Transaction) -> Unit = {}
) {
    val transactions = viewModel.transactions.collectAsLazyPagingItems()
    val filter by viewModel.filter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showFilterSheet by viewModel.showFilterSheet.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    
    val isRefreshing = transactions.loadState.refresh is LoadState.Loading
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                actions = {
                    // Sync status badge
                    SyncStatusBadge(
                        syncState = syncState,
                        pendingCount = pendingCount,
                        onSyncClick = { viewModel.triggerSync() }
                    )
                    
                    // Filter button
                    IconButton(
                        onClick = { viewModel.showFilterSheet() },
                        modifier = Modifier.semantics {
                            contentDescription = "Filter transactions. ${filter.activeFilterCount} filters active"
                        }
                    ) {
                        Box {
                            // Using text as placeholder for icon
                            Text("⚙", style = MaterialTheme.typography.titleLarge)
                            
                            if (filter.hasActiveFilters) {
                                Badge(
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(filter.activeFilterCount.toString())
                                }
                            }
                        }
                    }
    onNavigateBack: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    
    val filteredTransactions = viewModel.getFilteredTransactions(
        transactions = transactions,
        filter = uiState.filter
    )
    
    Scaffold(
        topBar = {
            MomoTopAppBar(
                title = "Transactions",
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
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search transactions...") },
                singleLine = true
            )
            
            // Active filter chips
            if (filter.hasActiveFilters) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filter.provider?.let { provider ->
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.updateFilter(filter.withProvider(null)) },
                            label = { Text(provider.displayName) }
                        )
                    }
                    
                    filter.status?.let { status ->
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.updateFilter(filter.withStatus(null)) },
                            label = { Text(status.value) }
                        )
                    }
                    
                    if (filter.startDate != null || filter.endDate != null) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.updateFilter(filter.withDateRange(null, null)) },
                            label = { Text("Date Range") }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Transaction list with pull-to-refresh
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { transactions.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                TransactionList(
                    transactions = transactions,
                    onTransactionClick = onTransactionClick
                )
            }
        }
    }
    
    // Filter sheet
    if (showFilterSheet) {
        TransactionFilterSheet(
            currentFilter = filter,
            onFilterChange = { viewModel.updateFilter(it) },
            onDismiss = { viewModel.hideFilterSheet() }
        )
    }
}

/**
 * Lazy column for displaying paginated transactions.
 */
@Composable
private fun TransactionList(
    transactions: LazyPagingItems<Transaction>,
    onTransactionClick: (Transaction) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Handle loading state
        when (val refreshState = transactions.loadState.refresh) {
            is LoadState.Loading -> {
                items(5) {
                    TransactionCardPlaceholder()
                }
            }
            
            is LoadState.Error -> {
                item {
                    ErrorCard(
                        error = AppError.Unknown(
                            message = refreshState.error.message ?: "Failed to load transactions"
                        ),
                        onRetry = { transactions.refresh() }
                    )
                }
            }
            
            is LoadState.NotLoading -> {
                if (transactions.itemCount == 0) {
                    item {
                        EmptyState()
                    }
                }
            }
        }
        
        // Transaction items
        items(
            count = transactions.itemCount,
            key = { index -> transactions[index]?.id ?: index }
        ) { index ->
            transactions[index]?.let { transaction ->
                TransactionCard(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction) }
                )
            }
        }
        
        // Loading more indicator
        if (transactions.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        // Error loading more
        if (transactions.loadState.append is LoadState.Error) {
            item {
                ErrorCard(
                    error = AppError.Unknown(
                        message = "Failed to load more transactions"
                    ),
                    onRetry = { transactions.retry() }
                )
            }
        }
    }
}

/**
 * Empty state when no transactions match the filter.
 */
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyStateAnimation(
            modifier = Modifier.size(200.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No transactions found",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Transactions will appear here when you receive payments",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
            // Filter chips
            FilterChipsRow(
                selectedFilter = uiState.filter,
                onFilterSelected = viewModel::setFilter,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
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
                        text = when (filter) {
                            TransactionsViewModel.TransactionFilter.ALL -> "All"
                            TransactionsViewModel.TransactionFilter.PENDING -> "Pending"
                            TransactionsViewModel.TransactionFilter.SENT -> "Sent"
                            TransactionsViewModel.TransactionFilter.FAILED -> "Failed"
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
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
