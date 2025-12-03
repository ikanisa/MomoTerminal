package com.momoterminal.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.momoterminal.offline.SyncState

@Composable
fun SyncStatusIndicator(
    syncState: SyncState,
    pendingCount: Int,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = when (syncState) {
        SyncState.Idle -> Triple(Icons.Default.CloudDone, MaterialTheme.colorScheme.primary, "Synced")
        SyncState.Offline -> Triple(Icons.Default.CloudOff, MaterialTheme.colorScheme.outline, "Offline")
        SyncState.Syncing -> Triple(Icons.Default.Sync, MaterialTheme.colorScheme.tertiary, "Syncing...")
        SyncState.Synced -> Triple(Icons.Default.CloudDone, MaterialTheme.colorScheme.primary, "Synced")
        is SyncState.Error -> Triple(Icons.Default.CloudOff, MaterialTheme.colorScheme.error, "Sync failed")
    }
    
    val animatedColor by animateColorAsState(color, label = "syncColor")
    
    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = if (syncState == SyncState.Syncing) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onSyncClick,
            modifier = Modifier.scale(pulseScale)
        ) {
            Icon(icon, contentDescription = label, tint = animatedColor)
        }
        
        if (pendingCount > 0 && syncState != SyncState.Syncing) {
            Badge(containerColor = MaterialTheme.colorScheme.error) {
                Text("$pendingCount")
            }
        }
    }
}

@Composable
fun CompactSyncIndicator(
    syncState: SyncState,
    modifier: Modifier = Modifier
) {
    val color = when (syncState) {
        SyncState.Idle, SyncState.Synced -> MaterialTheme.colorScheme.primary
        SyncState.Offline -> MaterialTheme.colorScheme.outline
        SyncState.Syncing -> MaterialTheme.colorScheme.tertiary
        is SyncState.Error -> MaterialTheme.colorScheme.error
    }
    
    Box(
        modifier = modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}
