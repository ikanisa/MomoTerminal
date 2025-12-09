package com.momoterminal.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Reusable empty state component for lists and screens.
 * Shows when there's no data to display.
 */
@Composable
fun EmptyStateView(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Inbox,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    showIcon: Boolean = true
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)) + scaleIn(tween(300)),
        exit = fadeOut(tween(300)) + scaleOut(tween(300))
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showIcon) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                MomoButton(
                    text = actionText,
                    onClick = onAction,
                    type = ButtonType.OUTLINE,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
            }
        }
    }
}

/**
 * Predefined empty states for common scenarios.
 */
object EmptyStates {
    @Composable
    fun NoTransactions(
        onAddTransaction: (() -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        EmptyStateView(
            title = "No Transactions Yet",
            description = "Your transaction history will appear here once you start making payments.",
            icon = Icons.Filled.ReceiptLong,
            actionText = if (onAddTransaction != null) "Make Payment" else null,
            onAction = onAddTransaction,
            modifier = modifier
        )
    }
    
    @Composable
    fun NoVendingMachines(
        onRefresh: (() -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        EmptyStateView(
            title = "No Machines Available",
            description = "There are no vending machines nearby at the moment.",
            icon = Icons.Filled.LocalCafe,
            actionText = if (onRefresh != null) "Refresh" else null,
            onAction = onRefresh,
            modifier = modifier
        )
    }
    
    @Composable
    fun NoWalletHistory(
        modifier: Modifier = Modifier
    ) {
        EmptyStateView(
            title = "No Wallet Activity",
            description = "Your wallet transaction history is empty. Top up your wallet to get started!",
            icon = Icons.Filled.AccountBalanceWallet,
            modifier = modifier
        )
    }
}
