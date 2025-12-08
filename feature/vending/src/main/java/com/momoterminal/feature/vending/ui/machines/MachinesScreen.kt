package com.momoterminal.feature.vending.ui.machines

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
fun MachinesScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEventOrder: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MachinesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val walletBalance by viewModel.walletBalance.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        MomoTopAppBar(
            title = "Juice Vending",
            onNavigateBack = onNavigateBack,
            actions = {
                IconButton(onClick = onNavigateToHistory) {
                    Icon(Icons.Default.History, contentDescription = "Order History")
                }
                IconButton(onClick = onNavigateToHelp) {
                    Icon(Icons.Default.Help, contentDescription = "Help")
                }
            }
        )
        Column(modifier = Modifier.fillMaxSize()) {
            walletBalance?.let { balance ->
                BalanceHeader(
                    balance = balance.formatAmount(),
                    currencySymbol = balance.currency,
                    label = "Wallet Balance",
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.padding(MomoTheme.spacing.md)
                )
            }

            when (val state = uiState) {
                is MachinesUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is MachinesUiState.Empty -> {
                    EmptyState(
                        title = "No Machines Available",
                        modifier = Modifier.fillMaxSize(),
                        description = "No vending machines found nearby.",
                        icon = Icons.Default.LocalDrink
                    )
                }
                is MachinesUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(MomoTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)
                    ) {
                        items(state.machines) { machine ->
                            MachineCard(machine, onClick = { onNavigateToDetail(machine.id) })
                        }
                    }
                }
                is MachinesUiState.Error -> {
                    EmptyState(
                        title = "Error",
                        modifier = Modifier.fillMaxSize(),
                        description = state.message,
                        icon = Icons.Default.Error,
                        action = {
                            PrimaryActionButton(
                                text = "Retry",
                                onClick = { viewModel.refresh() }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MachineCard(machine: VendingMachine, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PressableCard(
        onClick = onClick,
        enabled = machine.status == MachineStatus.AVAILABLE,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MomoTheme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(machine.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalDrink, null, Modifier.size(16.dp), MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("${machine.productName} (${machine.servingSizeML}ml)", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(machine.location, style = MaterialTheme.typography.bodySmall)
                    machine.formattedDistance()?.let { Text(" • $it", style = MaterialTheme.typography.bodySmall) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(
                        status = when (machine.status) {
                            MachineStatus.AVAILABLE -> StatusType.SUCCESS
                            MachineStatus.OFFLINE -> StatusType.ERROR
                            MachineStatus.MAINTENANCE -> StatusType.WARNING
                        },
                        label = when (machine.status) {
                            MachineStatus.AVAILABLE -> "Available"
                            MachineStatus.OFFLINE -> "Offline"
                            MachineStatus.MAINTENANCE -> "Maintenance"
                        }
                    )
                }
            }
            Spacer(Modifier.width(MomoTheme.spacing.md))
            Column(horizontalAlignment = Alignment.End) {
                Text(machine.formattedPrice(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(machine.currency, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
