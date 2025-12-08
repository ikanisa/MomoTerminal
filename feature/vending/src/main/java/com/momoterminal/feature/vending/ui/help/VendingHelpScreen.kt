package com.momoterminal.feature.vending.ui.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.component.*
import com.momoterminal.core.designsystem.theme.MomoTheme

@Composable
fun VendingHelpScreen(onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        MomoTopAppBar(title = "How It Works", onNavigateBack = onNavigateBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(MomoTheme.spacing.md), Arrangement.spacedBy(MomoTheme.spacing.md)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(MomoTheme.spacing.md), Arrangement.spacedBy(MomoTheme.spacing.md)) {
                    Text("Getting Your Juice", style = MaterialTheme.typography.headlineSmall)
                    HelpStep(1, Icons.Default.Search, "Find a Machine", "Browse nearby machines and select your preferred location.")
                    HelpStep(2, Icons.Default.AccountBalanceWallet, "Pay from Wallet", "Use your in-app wallet balance.")
                    HelpStep(3, Icons.Default.QrCode, "Get Your Code", "Receive a 4-digit time-limited code immediately.")
                    HelpStep(4, Icons.Default.KeyboardAlt, "Enter Code", "Find the keypad and enter your code.")
                    HelpStep(5, Icons.Default.LocalDrink, "Pour Your Juice", "The door opens. Pour your 500ml drink!")
                }
            }
            
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(MomoTheme.spacing.md), Arrangement.spacedBy(MomoTheme.spacing.sm)) {
                    Text("Important Notes", style = MaterialTheme.typography.titleMedium)
                    InfoItem(Icons.Default.Timer, "Codes expire in 2-5 minutes. Use them quickly!")
                    InfoItem(Icons.Default.Lock, "Each code is single-use and machine-specific.")
                    InfoItem(Icons.Default.Refresh, "Expired unused codes are automatically refunded.")
                    InfoItem(Icons.Default.Warning, "Ensure sufficient wallet balance before ordering.")
                }
            }
            
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(MomoTheme.spacing.md), Arrangement.spacedBy(MomoTheme.spacing.sm)) {
                    Text("FAQs", style = MaterialTheme.typography.titleMedium)
                    FaqItem("What if my code expires?", "Don't worry! A refund will be processed automatically.")
                    FaqItem("Can I use one code at multiple machines?", "No, each code is tied to a specific machine.")
                    FaqItem("How do I top up my wallet?", "Tap 'Top Up Wallet' from machine details or wallet page.")
                }
            }
        }
    }
}

@Composable
private fun HelpStep(number: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(number.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(16.dp))
        }
        Column(Modifier.weight(1f), Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(20.dp), MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(20.dp), MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(question, style = MaterialTheme.typography.titleSmall)
        Text(answer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
