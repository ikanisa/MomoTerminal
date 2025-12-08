package com.momoterminal.feature.vending.ui.detail

import androidx.compose.foundation.layout.*
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
fun MachineDetailScreen(
    onNavigateBack: () -> Unit,
    onPayClick: (machineId: String, amount: Long) -> Unit,
    onTopUpClick: () -> Unit,
    viewModel: MachineDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val walletBalance by viewModel.walletBalance.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        MomoTopAppBar(title = "Machine Details", onNavigateBack = onNavigateBack)
        when (val state = uiState) {
            is MachineDetailUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is MachineDetailUiState.Error -> EmptyState(
                title = "Error",
                modifier = Modifier.fillMaxSize(),
                description = state.message,
                icon = Icons.Default.Error,
                action = {
                    PrimaryActionButton("Retry", { viewModel.refresh() })
                }
            )
            is MachineDetailUiState.Success -> {
                val machine = state.machine
                val canPurchase = machine.status == MachineStatus.AVAILABLE
                val hasBalance = walletBalance != null && walletBalance!!.totalTokens >= machine.price
                
                Column(Modifier.fillMaxSize().padding(MomoTheme.spacing.md), Arrangement.spacedBy(MomoTheme.spacing.md)) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(MomoTheme.spacing.md), Arrangement.spacedBy(MomoTheme.spacing.sm)) {
                            Text(machine.name, style = MaterialTheme.typography.headlineMedium)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.LocalDrink, null, Modifier.size(24.dp), MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(machine.productName, style = MaterialTheme.typography.titleLarge)
                                    Text("Serving size: ${machine.productSizeML}ml", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            DetailRow(Icons.Default.AttachMoney, "Price", "${machine.formattedPrice()} ${machine.currency}")
                            DetailRow(Icons.Default.LocationOn, "Location", machine.location)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Status", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
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
                    }
                    
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(MomoTheme.spacing.md), Arrangement.spacedBy(MomoTheme.spacing.sm)) {
                            Text("How It Works", style = MaterialTheme.typography.titleMedium)
                            HowItWorksStep(1, "Pay from your wallet")
                            HowItWorksStep(2, "Receive 4-digit code")
                            HowItWorksStep(3, "Enter code at machine")
                            HowItWorksStep(4, "Pour your ${machine.productSizeML}ml drink")
                        }
                    }
                    
                    Spacer(Modifier.weight(1f))
                    
                    if (!hasBalance && canPurchase) {
                        OutlinedButton(onTopUpClick, Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Top Up Wallet")
                        }
                    }
                    
                    PrimaryActionButton(
                        text = if (hasBalance) "Pay ${machine.formattedPrice()}" else "Insufficient Balance",
                        onClick = { onPayClick(machine.id, machine.price) },
                        enabled = canPurchase && hasBalance,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun HowItWorksStep(step: Int, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(step.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
