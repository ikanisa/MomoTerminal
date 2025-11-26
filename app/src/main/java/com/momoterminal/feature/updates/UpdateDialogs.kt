package com.momoterminal.feature.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Snackbar shown when a flexible update has been downloaded and is ready to install.
 */
@Composable
fun FlexibleUpdateDownloadedSnackbar(
    onInstallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        snackbarHostState.showSnackbar(
            message = "Update downloaded",
            actionLabel = "Install"
        )
    }
    
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            Snackbar(
                action = {
                    TextButton(onClick = onInstallClick) {
                        Text(
                            text = "Install",
                            color = MaterialTheme.colorScheme.inversePrimary
                        )
                    }
                }
            ) {
                Text(text = "An update is ready to install")
            }
        }
    )
}

/**
 * Dialog shown when an update is available.
 */
@Composable
fun UpdateAvailableDialog(
    updateState: UpdateState.UpdateAvailable,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {
            // Only allow dismiss for flexible updates
            if (!updateState.isImmediate) {
                onDismiss()
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (updateState.isImmediate) {
                    "Update Required"
                } else {
                    "Update Available"
                },
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (updateState.isImmediate) {
                        "A critical update is required to continue using this app. Please update now."
                    } else {
                        "A new version of MomoTerminal is available. Update now to get the latest features and improvements."
                    },
                    textAlign = TextAlign.Center
                )
                
                if (updateState.priority >= 3) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Priority: ${getPriorityLabel(updateState.priority)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpdateClick) {
                Text("Update Now")
            }
        },
        dismissButton = {
            if (!updateState.isImmediate) {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Progress indicator for update download.
 */
@Composable
fun UpdateProgressIndicator(
    downloadState: UpdateState.Downloading,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Downloading update...",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = { downloadState.progress },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatBytes(downloadState.bytesDownloaded),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "${(downloadState.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = formatBytes(downloadState.totalBytesToDownload),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Composable for handling update download completion.
 */
@Composable
fun UpdateDownloadedBanner(
    onInstallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Update ready",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Restart to apply the update",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Button(onClick = onInstallClick) {
            Text("Restart")
        }
    }
}

/**
 * Get a human-readable priority label.
 */
private fun getPriorityLabel(priority: Int): String {
    return when {
        priority >= 5 -> "Critical"
        priority >= 4 -> "High"
        priority >= 3 -> "Medium"
        priority >= 2 -> "Low"
        else -> "Optional"
    }
}

/**
 * Format bytes to a human-readable string.
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}
