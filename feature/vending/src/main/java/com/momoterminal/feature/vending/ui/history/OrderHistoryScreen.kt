package com.momoterminal.feature.vending.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momoterminal.core.designsystem.component.*
import com.momoterminal.core.designsystem.theme.MomoTheme
import com.momoterminal.feature.vending.domain.model.*

@Composable
fun OrderHistoryScreen(
    onNavigateBack: () -> Unit,
    onOrderClick: (String) -> Unit,
    viewModel: OrderHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        MomoTopAppBar(
            title = "Order History",
            onNavigateBack = onNavigateBack,
            actions = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, "Refresh")
                }
            }
        )
        when (val state = uiState) {
            is OrderHistoryUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is OrderHistoryUiState.Empty -> EmptyState(
                title = "No Orders Yet",
                modifier = Modifier.fillMaxSize(),
                description = "Your vending orders will appear here",
                icon = Icons.Default.Receipt
            )
            is OrderHistoryUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(MomoTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)
                ) {
                    items(state.orders) { order -> OrderCard(order, { onOrderClick(order.id) }) }
                }
            }
            is OrderHistoryUiState.Error -> EmptyState(
                title = "Error",
                modifier = Modifier.fillMaxSize(),
                description = state.message,
                icon = Icons.Default.Error,
                action = {
                    PrimaryActionButton("Retry", { viewModel.refresh() })
                }
            )
        }
    }
}

@Composable
private fun OrderCard(order: VendingOrder, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PressableCard(onClick, modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(MomoTheme.spacing.md), Arrangement.spacedBy(MomoTheme.spacing.sm)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(order.machineName, style = MaterialTheme.typography.titleMedium)
                    Text(order.formattedDate(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(order.formattedAmount(), style = MaterialTheme.typography.titleMedium)
                    StatusPill(
                        status = when (order.status) {
                            OrderStatus.PENDING -> StatusType.PENDING
                            OrderStatus.CODE_GENERATED -> StatusType.INFO
                            OrderStatus.DISPENSED -> StatusType.SUCCESS
                            OrderStatus.EXPIRED, OrderStatus.REFUNDED, OrderStatus.FAILED -> StatusType.ERROR
                        },
                        label = when (order.status) {
                            OrderStatus.PENDING -> "Pending"
                            OrderStatus.CODE_GENERATED -> "Active"
                            OrderStatus.DISPENSED -> "Completed"
                            OrderStatus.EXPIRED -> "Expired"
                            OrderStatus.REFUNDED -> "Refunded"
                            OrderStatus.FAILED -> "Failed"
                        }
                    )
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocalDrink, null, Modifier.size(16.dp), MaterialTheme.colorScheme.primary)
                    Text("${order.productName} (${order.productSizeML}ml)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(order.machineLocation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
