package com.momoterminal.feature.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.core.designsystem.component.BalanceHeader
import com.momoterminal.core.designsystem.component.GlassCard
import com.momoterminal.core.designsystem.theme.MomoTheme
import com.momoterminal.feature.wallet.WalletUiState
import com.momoterminal.feature.wallet.WalletViewModel

@Composable
fun WalletScreen(
    viewModel: WalletViewModel = hiltViewModel(),
    onNavigateToTransactions: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val balance by viewModel.walletBalance.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MomoTheme.spacing.md)
    ) {
        // Balance Header
        balance?.let {
            BalanceHeader(
                balance = it.formatAmount(),
                currencySymbol = it.currency,
                label = "Wallet Balance",
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(MomoTheme.spacing.lg))

        // Content based on state
        when (val state = uiState) {
            is WalletUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is WalletUiState.Empty -> {
                EmptyWalletView()
            }
            is WalletUiState.Success -> {
                WalletContent(
                    balance = state.balance,
                    onNavigateToTransactions = onNavigateToTransactions
                )
            }
            is WalletUiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = { viewModel.refresh() }
                )
            }
        }
    }
}

@Composable
private fun EmptyWalletView() {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(MomoTheme.spacing.xl)
        ) {
            Text(
                text = "No tokens yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MomoTheme.spacing.sm))
            Text(
                text = "Receive payments via SMS or NFC to add tokens to your wallet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WalletContent(
    balance: com.momoterminal.feature.wallet.domain.model.WalletBalance,
    onNavigateToTransactions: () -> Unit
) {
    Column {
        // Quick Stats
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Active Tokens",
                    value = balance.activeTokenCount.toString()
                )
                StatItem(
                    label = "Currency",
                    value = balance.currency
                )
            }
        }

        Spacer(modifier = Modifier.height(MomoTheme.spacing.md))

        // Actions
        Button(
            onClick = onNavigateToTransactions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Transaction History")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(MomoTheme.spacing.lg)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(MomoTheme.spacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(MomoTheme.spacing.md))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
