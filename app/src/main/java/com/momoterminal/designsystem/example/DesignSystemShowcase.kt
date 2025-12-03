package com.momoterminal.designsystem.example

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.momoterminal.designsystem.component.*
import com.momoterminal.designsystem.theme.MomoTerminalTheme
import com.momoterminal.designsystem.theme.MomoTheme

@Composable
fun DesignSystemShowcase() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(MomoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.xl)
    ) {
        // Section: Balance Header
        ShowcaseSection("Balance Header") {
            BalanceHeader(
                balance = "125,000",
                currencySymbol = "FRw",
                label = "Available Balance"
            )
            Spacer(modifier = Modifier.height(MomoTheme.spacing.md))
            BalanceHeaderCompact(
                balance = "50,000",
                currencySymbol = "FRw",
                label = "Pending"
            )
        }

        // Section: Glass Cards
        ShowcaseSection("Glass Cards") {
            GlassCard { Text("Standard Glass Card", style = MaterialTheme.typography.bodyLarge) }
            Spacer(modifier = Modifier.height(MomoTheme.spacing.md))
            GlassCard(elevated = true) { Text("Elevated Glass Card", style = MaterialTheme.typography.bodyLarge) }
            Spacer(modifier = Modifier.height(MomoTheme.spacing.md))
            GlassCardGradient { Text("Gradient Glass Card", style = MaterialTheme.typography.bodyLarge, color = androidx.compose.ui.graphics.Color.White) }
        }

        // Section: Token Chips
        ShowcaseSection("Token Chips") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)
            ) {
                TokenChip(label = "Bonus", value = "500", type = TokenType.BONUS)
                TokenChip(label = "Points", value = "1,250", type = TokenType.POINTS)
            }
            Spacer(modifier = Modifier.height(MomoTheme.spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)
            ) {
                TokenChip(label = "Voucher", value = "2,000", type = TokenType.VOUCHER)
                TokenChip(label = "Cashback", value = "350", type = TokenType.CASHBACK)
            }
        }

        // Section: Transaction Rows
        ShowcaseSection("Transaction Rows") {
            TransactionRow(
                amount = "15,000",
                currencySymbol = "FRw",
                direction = TransactionDirection.CREDIT,
                source = TransactionSource.NFC,
                title = "Payment Received",
                subtitle = "From: +250 78 123 4567",
                timestamp = "10:30 AM",
                transactionId = "TXN-2024120301"
            )
            HorizontalDivider()
            TransactionRow(
                amount = "5,000",
                currencySymbol = "FRw",
                direction = TransactionDirection.DEBIT,
                source = TransactionSource.SMS,
                title = "Transfer Sent",
                subtitle = "To: Jean Baptiste",
                timestamp = "09:15 AM"
            )
        }

        // Section: NFC Cards
        ShowcaseSection("NFC Info Cards") {
            NfcInfoCard(
                status = NfcStatus.IDLE,
                title = "NFC Terminal Ready",
                subtitle = "Tap customer phone to receive payment"
            )
            Spacer(modifier = Modifier.height(MomoTheme.spacing.md))
            NfcInfoCard(
                status = NfcStatus.SCANNING,
                title = "Scanning...",
                subtitle = "Hold phone near terminal"
            )
            Spacer(modifier = Modifier.height(MomoTheme.spacing.md))
            NfcInfoCard(
                status = NfcStatus.SUCCESS,
                title = "Payment Received",
                subtitle = "Transaction complete",
                amount = "15,000",
                currencySymbol = "FRw"
            )
        }

        // Section: Status Indicators
        ShowcaseSection("Status Indicators") {
            Row(horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.lg)) {
                StatusIndicator(type = StatusType.SUCCESS, label = "Completed")
                StatusIndicator(type = StatusType.PENDING, label = "Pending")
                StatusIndicator(type = StatusType.ERROR, label = "Failed")
            }
            Spacer(modifier = Modifier.height(MomoTheme.spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)) {
                StatusBadge(type = StatusType.SUCCESS, label = "Synced")
                StatusBadge(type = StatusType.WARNING, label = "Offline")
                StatusBadge(type = StatusType.INFO, label = "New")
            }
        }

        // Section: Action Buttons
        ShowcaseSection("Action Buttons") {
            PrimaryActionButton(
                text = "Activate NFC Terminal",
                icon = Icons.Default.Nfc,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(MomoTheme.spacing.md))
            SecondaryActionButton(
                text = "Sync SMS Messages",
                icon = Icons.Default.Sms,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(MomoTheme.spacing.md))
            PrimaryActionButton(
                text = "Loading...",
                onClick = {},
                loading = true
            )
        }

        // Section: Quick Actions
        ShowcaseSection("Quick Actions") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickActionButton("Scan NFC", Icons.Default.Nfc, onClick = {})
                    QuickActionButton("Sync SMS", Icons.Default.Sms, onClick = {})
                }
            }
        }

        Spacer(modifier = Modifier.height(MomoTheme.spacing.xxxl))
    }
}

@Composable
private fun ShowcaseSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(MomoTheme.spacing.md))
        content()
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun ShowcaseLightPreview() {
    MomoTerminalTheme(darkTheme = false) { DesignSystemShowcase() }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun ShowcaseDarkPreview() {
    MomoTerminalTheme(darkTheme = true) { DesignSystemShowcase() }
}
