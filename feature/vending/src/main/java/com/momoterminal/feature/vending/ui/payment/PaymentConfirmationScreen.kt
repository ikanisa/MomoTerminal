package com.momoterminal.feature.vending.ui.payment

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

@Composable
fun PaymentConfirmationScreen(
    onNavigateBack: () -> Unit,
    onPaymentSuccess: (orderId: String) -> Unit,
    onTopUpClick: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val walletBalance by viewModel.walletBalance.collectAsStateWithLifecycle()
    
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is PaymentUiState.Success -> onPaymentSuccess(state.order.id)
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MomoTopAppBar(title = "Confirm Payment", onNavigateBack = onNavigateBack)
        Column(Modifier.fillMaxSize().padding(MomoTheme.spacing.md), Arrangement.spacedBy(MomoTheme.spacing.md)) {
            when (val state = uiState) {
                is PaymentUiState.Idle -> PaymentConfirmationContent(walletBalance, { viewModel.confirmPayment() }, onNavigateBack)
                is PaymentUiState.Processing -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator()
                        Text("Processing payment...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                is PaymentUiState.InsufficientBalance -> InsufficientBalanceContent(state.currentBalance, state.requiredAmount, onTopUpClick, onNavigateBack)
                is PaymentUiState.Error -> EmptyState(
                    title = "Payment Failed",
                    modifier = Modifier.fillMaxSize(),
                    description = state.message,
                    icon = Icons.Default.Error,
                    action = {
                        PrimaryActionButton("Try Again", { viewModel.resetState() })
                    }
                )
                is PaymentUiState.Success -> {}
            }
        }
    }
}

@Composable
private fun PaymentConfirmationContent(walletBalance: com.momoterminal.feature.wallet.domain.model.WalletBalance?, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(MomoTheme.spacing.md)) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(MomoTheme.spacing.md), Arrangement.spacedBy(MomoTheme.spacing.sm)) {
                Text("Payment Summary", style = MaterialTheme.typography.titleLarge)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                walletBalance?.let {
                    PaymentRow("Current Balance", it.formatAmount())
                    PaymentRow("Payment Method", "Wallet")
                }
            }
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.padding(MomoTheme.spacing.md), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Your wallet will be debited immediately and you'll receive a time-limited code.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)) {
            PrimaryActionButton("Confirm Payment", onConfirm, Modifier.fillMaxWidth())
            OutlinedButton(onCancel, Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable
private fun InsufficientBalanceContent(currentBalance: Long, requiredAmount: Long, onTopUpClick: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(MomoTheme.spacing.md)) {
        EmptyState(
            title = "Insufficient Balance",
            modifier = Modifier.weight(1f),
            description = "You don't have enough balance.",
            icon = Icons.Default.AccountBalanceWallet
        )
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(MomoTheme.spacing.md), Arrangement.spacedBy(8.dp)) {
                PaymentRow("Current Balance", String.format("%,d.%02d", currentBalance / 100, currentBalance % 100))
                PaymentRow("Required Amount", String.format("%,d.%02d", requiredAmount / 100, requiredAmount % 100))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)) {
            PrimaryActionButton("Top Up Wallet", onTopUpClick, Modifier.fillMaxWidth())
            OutlinedButton(onCancel, Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable
private fun PaymentRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
