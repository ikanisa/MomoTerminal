package com.momoterminal.presentation.screens.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.domain.model.TokenTransaction
import com.momoterminal.domain.model.TokenTransactionType
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message, uiState.error) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Token Wallet") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.pendingSyncCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("${uiState.pendingSyncCount}")
                        }
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Balance Card
                item {
                    BalanceCard(
                        balance = uiState.wallet?.balance ?: 0,
                        currency = uiState.wallet?.currency ?: "CREDITS"
                    )
                }

                // Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.processUncreditedSms() },
                            enabled = !uiState.isProcessing,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Process SMS")
                        }
                    }
                }

                // Transaction History Header
                item {
                    Text(
                        "Transaction History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Transactions
                if (uiState.transactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No transactions yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(uiState.transactions, key = { it.id }) { txn ->
                        TransactionItem(txn)
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: Long, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Available Balance", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                text = NumberFormat.getNumberInstance().format(balance),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(currency, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TransactionItem(txn: TokenTransaction) {
    val isCredit = txn.amount > 0
    val icon = when (txn.type) {
        TokenTransactionType.SMS_CREDIT -> Icons.Default.Sms
        TokenTransactionType.NFC_CREDIT, TokenTransactionType.NFC_DEBIT -> Icons.Default.Nfc
        TokenTransactionType.MANUAL_CREDIT, TokenTransactionType.MANUAL_DEBIT -> Icons.Default.Edit
        TokenTransactionType.TRANSFER_IN, TokenTransactionType.TRANSFER_OUT -> Icons.Default.SwapHoriz
        TokenTransactionType.EXPIRY -> Icons.Default.Timer
    }
    val color = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(txn.description ?: txn.type.name.replace("_", " "), fontWeight = FontWeight.Medium)
                Text(
                    txn.createdAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${if (isCredit) "+" else ""}${NumberFormat.getNumberInstance().format(txn.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
