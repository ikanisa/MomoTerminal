package com.momoterminal.presentation.components.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.components.animations.SyncLoadingAnimation
import com.momoterminal.sync.SyncManager
import com.momoterminal.sync.SyncState
import kotlinx.coroutines.flow.StateFlow

/**
 * Badge component showing sync state with pending count and sync button.
 */
@Composable
fun SyncStatusBadge(
    syncState: SyncState,
    pendingCount: Int,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accessibilityDescription = when (syncState) {
        is SyncState.Idle -> "Sync status: Up to date. $pendingCount pending transactions."
        is SyncState.Syncing -> "Syncing in progress"
        is SyncState.Success -> "Sync complete. ${syncState.syncedCount} transactions synced."
        is SyncState.Error -> "Sync error: ${syncState.message}. Tap to retry."
        is SyncState.Offline -> "Offline. $pendingCount pending transactions."
    }
    
    Surface(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                enabled = syncState !is SyncState.Syncing,
                onClick = onSyncClick
            )
            .semantics {
                contentDescription = accessibilityDescription
            },
        color = getSyncBackgroundColor(syncState),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Sync icon or animation
            when (syncState) {
                is SyncState.Syncing -> {
                    SyncLoadingAnimation(size = 24.dp)
                }
                else -> {
                    SyncIcon(syncState = syncState)
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Status text
            Text(
                text = getSyncStatusText(syncState),
                style = MaterialTheme.typography.labelMedium,
                color = getSyncTextColor(syncState)
            )
            
            // Pending count badge
            AnimatedVisibility(
                visible = pendingCount > 0 && syncState !is SyncState.Syncing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ) {
                    Text(
                        text = if (pendingCount > 99) "99+" else pendingCount.toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * Sync icon based on current state.
 */
@Composable
private fun SyncIcon(syncState: SyncState) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(getSyncIconBackgroundColor(syncState)),
        contentAlignment = Alignment.Center
    ) {
        // Using a simple circle indicator since we don't have vector icons defined
        // In production, you would use actual icons like Icons.Default.Sync
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(getSyncTextColor(syncState))
        )
    }
}

/**
 * Get background color based on sync state.
 */
@Composable
private fun getSyncBackgroundColor(syncState: SyncState): Color {
    return when (syncState) {
        is SyncState.Idle -> MaterialTheme.colorScheme.surfaceVariant
        is SyncState.Syncing -> MaterialTheme.colorScheme.primaryContainer
        is SyncState.Success -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        is SyncState.Error -> MaterialTheme.colorScheme.errorContainer
        is SyncState.Offline -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
}

/**
 * Get text color based on sync state.
 */
@Composable
private fun getSyncTextColor(syncState: SyncState): Color {
    return when (syncState) {
        is SyncState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
        is SyncState.Syncing -> MaterialTheme.colorScheme.onPrimaryContainer
        is SyncState.Success -> MaterialTheme.colorScheme.primary
        is SyncState.Error -> MaterialTheme.colorScheme.error
        is SyncState.Offline -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
}

/**
 * Get icon background color based on sync state.
 */
@Composable
private fun getSyncIconBackgroundColor(syncState: SyncState): Color {
    return when (syncState) {
        is SyncState.Idle -> Color.Transparent
        is SyncState.Syncing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        is SyncState.Success -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        is SyncState.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
        is SyncState.Offline -> Color.Gray.copy(alpha = 0.2f)
    }
}

/**
 * Get status text based on sync state.
 */
private fun getSyncStatusText(syncState: SyncState): String {
    return when (syncState) {
        is SyncState.Idle -> "Synced"
        is SyncState.Syncing -> "Syncing..."
        is SyncState.Success -> "Synced"
        is SyncState.Error -> "Retry"
        is SyncState.Offline -> "Offline"
    }
}

/**
 * Overload that accepts SyncManager directly.
 */
@Composable
fun SyncStatusBadge(
    syncManager: SyncManager,
    pendingCountFlow: StateFlow<Int>,
    modifier: Modifier = Modifier
) {
    val syncState by syncManager.syncState.collectAsState()
    val pendingCount by pendingCountFlow.collectAsState(initial = 0)
    
    SyncStatusBadge(
        syncState = syncState,
        pendingCount = pendingCount,
        onSyncClick = { syncManager.triggerManualSync() },
        modifier = modifier
    )
}
