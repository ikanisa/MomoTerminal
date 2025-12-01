package com.momoterminal.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Network status banner that displays offline/syncing state.
 * 
 * Shows a prominent banner at the top of the screen when:
 * - Device is offline (no network connectivity)
 * - Device is syncing data after reconnection
 * 
 * Usage:
 * ```kotlin
 * Column {
 *     NetworkStatusBanner(
 *         isOnline = networkState.isConnected,
 *         isSyncing = syncState.isSyncing,
 *         pendingTransactions = syncState.queuedCount
 *     )
 *     // Rest of your screen content
 * }
 * ```
 */
@Composable
fun NetworkStatusBanner(
    isOnline: Boolean,
    isSyncing: Boolean = false,
    pendingTransactions: Int = 0,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isOnline || isSyncing,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        val backgroundColor = when {
            !isOnline -> MaterialTheme.colorScheme.errorContainer
            isSyncing -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
        
        val contentColor = when {
            !isOnline -> MaterialTheme.colorScheme.onErrorContainer
            isSyncing -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (!isOnline) Icons.Outlined.CloudOff else Icons.Outlined.Sync,
                contentDescription = if (!isOnline) "Offline" else "Syncing",
                tint = contentColor
            )
            
            Text(
                text = when {
                    !isOnline && pendingTransactions > 0 -> {
                        "Offline - $pendingTransactions transaction${if (pendingTransactions != 1) "s" else ""} queued"
                    }
                    !isOnline -> "Offline mode - Transactions will sync when connected"
                    isSyncing && pendingTransactions > 0 -> {
                        "Syncing $pendingTransactions transaction${if (pendingTransactions != 1) "s" else ""}..."
                    }
                    isSyncing -> "Syncing transactions..."
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

/**
 * Compact version of the network status indicator for use in smaller spaces.
 */
@Composable
fun NetworkStatusChip(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = "Offline",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(0.dp)
            )
            
            Text(
                text = "Offline",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
