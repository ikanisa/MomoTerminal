package com.momoterminal.core.designsystem.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.component.BalanceHeader
import com.momoterminal.core.designsystem.component.GlassCard
import com.momoterminal.core.designsystem.component.PrimaryActionButton
import com.momoterminal.core.designsystem.component.QuickActionButton
import com.momoterminal.core.designsystem.component.SecondaryActionButton
import com.momoterminal.core.designsystem.component.SectionHeader
import com.momoterminal.core.designsystem.component.SectionScaffold
import com.momoterminal.core.designsystem.component.TokenChip
import com.momoterminal.core.designsystem.component.TokenType
import com.momoterminal.core.designsystem.component.TransactionDirection
import com.momoterminal.core.designsystem.component.TransactionRow
import com.momoterminal.core.designsystem.component.TransactionSource
import com.momoterminal.core.designsystem.theme.MomoTerminalTheme
import com.momoterminal.core.designsystem.theme.MomoTheme



data class TokenBalance(val type: TokenType, val label: String, val value: String)

data class Transaction(
    val id: String,
    val amount: String,
    val direction: TransactionDirection,
    val source: TransactionSource,
    val title: String,
    val subtitle: String,
    val timestamp: String
)

@Composable
fun WalletScreen(
    balance: String = "125,000",
    currencySymbol: String = "FRw",
    tokens: List<TokenBalance> = sampleTokens,
    transactions: List<Transaction> = sampleTransactions,
    onNfcScan: () -> Unit = {},
    onSmsSync: () -> Unit = {},
    onTopUp: () -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {},
    onViewAllTransactions: () -> Unit = {}
) {
    SectionScaffold(
        topContent = {
            BalanceHeader(
                balance = balance,
                currencySymbol = currencySymbol,
                label = "Available Balance",
                icon = Icons.Default.Wallet
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = MomoTheme.spacing.xxxl)
        ) {
            // Token chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = MomoTheme.spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)
                ) {
                    items(tokens) { token ->
                        TokenChip(
                            label = token.label,
                            value = token.value,
                            type = token.type,
                            icon = when (token.type) {
                                TokenType.BONUS -> Icons.Default.CardGiftcard
                                TokenType.POINTS -> Icons.Default.Star
                                else -> null
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(MomoTheme.spacing.lg))
            }

            // Quick actions
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MomoTheme.spacing.lg)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionButton("Scan NFC", Icons.Default.Nfc, onNfcScan)
                        QuickActionButton("Sync SMS", Icons.Default.Sms, onSmsSync)
                        QuickActionButton("Top Up", Icons.Default.QrCodeScanner, onTopUp)
                    }
                }
                Spacer(modifier = Modifier.height(MomoTheme.spacing.xl))
            }

            // Transactions header
            item {
                SectionHeader(
                    title = "Recent Transactions",
                    action = { TextButton(onClick = onViewAllTransactions) { Text("View All") } }
                )
            }

            // Transaction list
            items(transactions) { txn ->
                TransactionRow(
                    amount = txn.amount,
                    currencySymbol = currencySymbol,
                    direction = txn.direction,
                    source = txn.source,
                    title = txn.title,
                    subtitle = txn.subtitle,
                    timestamp = txn.timestamp,
                    transactionId = txn.id,
                    onClick = { onTransactionClick(txn) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 76.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // Bottom actions
            item {
                Spacer(modifier = Modifier.height(MomoTheme.spacing.xl))
                Column(
                    modifier = Modifier.padding(horizontal = MomoTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.md)
                ) {
                    PrimaryActionButton("Activate NFC Terminal", onNfcScan, icon = Icons.Default.Nfc)
                    SecondaryActionButton("Sync SMS Messages", onSmsSync, icon = Icons.Default.Sms)
                }
            }
        }
    }
}

private val sampleTokens = listOf(
    TokenBalance(TokenType.BONUS, "Bonus", "500"),
    TokenBalance(TokenType.POINTS, "Points", "1,250"),
    TokenBalance(TokenType.VOUCHER, "Voucher", "2,000"),
    TokenBalance(TokenType.CASHBACK, "Cashback", "350")
)

private val sampleTransactions = listOf(
    Transaction("TXN-001", "15,000", TransactionDirection.CREDIT, TransactionSource.NFC, "Payment Received", "From: +250 78 123 4567", "10:30 AM"),
    Transaction("TXN-002", "5,000", TransactionDirection.DEBIT, TransactionSource.SMS, "Transfer Sent", "To: Jean Baptiste", "09:15 AM"),
    Transaction("TXN-003", "25,000", TransactionDirection.CREDIT, TransactionSource.SMS, "Merchant Payment", "Shop: Kigali Store", "Yesterday"),
    Transaction("TXN-004", "2,500", TransactionDirection.DEBIT, TransactionSource.MANUAL, "Airtime Purchase", "MTN Rwanda", "Yesterday")
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun WalletScreenLightPreview() {
    MomoTerminalTheme(darkTheme = false) {
        WalletScreen()
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WalletScreenDarkPreview() {
    MomoTerminalTheme(darkTheme = true) {
        WalletScreen()
    }
}
