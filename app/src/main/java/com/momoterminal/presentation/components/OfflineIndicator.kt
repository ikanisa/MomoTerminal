package com.momoterminal.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momoterminal.offline.SyncState
import com.momoterminal.util.NetworkState

/**
 * Banner that shows when device is offline.
 * Designed for low-connectivity environments.
 */
@Composable
fun OfflineBanner(
    networkState: NetworkState,
    syncState: SyncState,
    pendingCount: Int,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = networkState == NetworkState.Unavailable || pendingCount > 0,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        val backgroundColor = when {
            networkState == NetworkState.Unavailable -> MaterialTheme.colorScheme.errorContainer
            syncState is SyncState.Error -> MaterialTheme.colorScheme.errorContainer
            pendingCount > 0 -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
        
        val contentColor = when {
            networkState == NetworkState.Unavailable -> MaterialTheme.colorScheme.onErrorContainer
            syncState is SyncState.Error -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onTertiaryContainer
        }
        
        Surface(
            color = backgroundColor,
            contentColor = contentColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (networkState == NetworkState.Unavailable) 
                            Icons.Default.CloudOff else Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Text(
                        text = when {
                            networkState == NetworkState.Unavailable -> "You're offline"
                            syncState is SyncState.Error -> "Sync failed"
                            pendingCount > 0 -> "$pendingCount pending"
                            else -> "Syncing..."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (networkState == NetworkState.Available && pendingCount > 0) {
                    TextButton(
                        onClick = onSyncClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
                    ) {
                        Text("Sync Now")
                    }
                }
            }
        }
    }
}

/**
 * Compact offline indicator dot.
 */
@Composable
fun OfflineDot(
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    MaterialTheme.colorScheme.error,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
    }
}
