package com.momoterminal.core.designsystem.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.component.*
import com.momoterminal.core.designsystem.motion.MomoHaptic
import com.momoterminal.core.designsystem.motion.MotionTokens
import com.momoterminal.core.designsystem.motion.performMomoHaptic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class WalletTransaction(
    val id: String, val amount: Double, val isCredit: Boolean, val title: String,
    val subtitle: String, val timestamp: String, val status: StatusType = StatusType.SUCCESS, val isNew: Boolean = false
)

@Composable
fun InteractiveWalletScreen(
    balance: Double, currencySymbol: String = "GHS", transactions: List<WalletTransaction>,
    smsSyncState: SmsSyncState = SmsSyncState.Idle, onNfcScan: () -> Unit = {},
    onSmsSync: () -> Unit = {}, onTransactionClick: (WalletTransaction) -> Unit = {}, modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var highlightedIds by remember { mutableStateOf(setOf<String>()) }
    var selectedTransaction by remember { mutableStateOf<WalletTransaction?>(null) }
    
    LaunchedEffect(transactions) {
        val newIds = transactions.filter { it.isNew }.map { it.id }.toSet()
        if (newIds.isNotEmpty()) {
            highlightedIds = newIds
            scope.launch { listState.animateScrollToItem(0) }
            delay(2000)
            highlightedIds = emptySet()
        }
    }
    
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                PressableCard({}, Modifier.fillMaxWidth().padding(16.dp), containerColor = MaterialTheme.colorScheme.primaryContainer, defaultElevation = 4.dp) {
                    Column(Modifier.fillMaxWidth().padding(24.dp)) {
                        Row(
                            modifier = Modifier,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Wallet, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Available Balance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                        Spacer(Modifier.height(8.dp))
                        AnimatedBalance(balance, "$currencySymbol ", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), defaultColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(Triple("NFC Scan", Icons.Default.Nfc, onNfcScan), Triple("QR Code", Icons.Default.QrCodeScanner, {}), Triple("History", Icons.Default.History, {})).forEach { (label, icon, action) ->
                        PressableCard(action, Modifier.weight(1f), haptic = MomoHaptic.ButtonPress) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text(label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            item { SmsSyncIndicator(smsSyncState, onSmsSync, Modifier.padding(horizontal = 16.dp)); Spacer(Modifier.height(24.dp)) }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton({}) { Text("View All") }
                }
            }
            items(transactions, { it.id }) { txn ->
                val highlightAlpha by animateFloatAsState(
                    if (txn.id in highlightedIds) 0.15f else 0f,
                    if (txn.id in highlightedIds) infiniteRepeatable(tween(500), RepeatMode.Reverse) else tween(MotionTokens.STANDARD),
                    label = "highlight"
                )
                val highlightColor = if (txn.isCredit) Color(0xFF2E7D32) else Color(0xFFC62828)
                PressableRow({ view.performMomoHaptic(MomoHaptic.Tap); selectedTransaction = txn }, Modifier.animateItem()) {
                    Row(Modifier.fillMaxWidth().background(highlightColor.copy(alpha = highlightAlpha)).padding(horizontal = 16.dp, vertical = 12.dp), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background((if (txn.isCredit) Color(0xFF2E7D32) else Color(0xFFC62828)).copy(alpha = 0.12f), MaterialTheme.shapes.small), Alignment.Center) {
                            Icon(if (txn.isCredit) Icons.Default.CallReceived else Icons.Default.CallMade, null, tint = if (txn.isCredit) Color(0xFF2E7D32) else Color(0xFFC62828))
                        }
                        Column(Modifier.weight(1f)) { Text(txn.title, style = MaterialTheme.typography.titleSmall); Text(txn.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Column(horizontalAlignment = Alignment.End) { AnimatedAmount(txn.amount, currencySymbol, txn.isCredit, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)); Spacer(Modifier.height(4.dp)); StatusDot(txn.status) }
                    }
                }
                HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
        selectedTransaction?.let { txn ->
            TransactionDetailSheet(true, { selectedTransaction = null }, txn.id, txn.amount.toInt().toString(), currencySymbol, txn.isCredit, txn.title, txn.subtitle, txn.timestamp, txn.status)
        }
    }
}

val sampleWalletTransactions = listOf(
    WalletTransaction("TXN-001", 15000.0, true, "Payment Received", "From: +233 24 123 4567", "10:30 AM"),
    WalletTransaction("TXN-002", 5000.0, false, "Transfer Sent", "To: Kofi Mensah", "09:15 AM"),
    WalletTransaction("TXN-003", 25000.0, true, "Merchant Payment", "Shop: Accra Store", "Yesterday"),
    WalletTransaction("TXN-004", 2500.0, false, "Airtime Purchase", "MTN Ghana", "Yesterday", StatusType.PENDING)
)
