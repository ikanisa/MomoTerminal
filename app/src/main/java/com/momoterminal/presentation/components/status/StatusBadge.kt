package com.momoterminal.presentation.components.status

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.NfcOutlined
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.PaymentShapes
import com.momoterminal.presentation.theme.StatusFailed
import com.momoterminal.presentation.theme.StatusPending
import com.momoterminal.presentation.theme.StatusSent
import com.momoterminal.presentation.theme.SuccessGreen

/**
 * Status badge for transaction status.
 */
@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    val (color, icon, label) = getStatusInfo(status)
    
    Surface(
        modifier = modifier,
        shape = PaymentShapes.statusBadge,
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = color
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = color
            )
        }
    }
}

/**
 * Get status info (color, icon, label) from status string.
 */
private fun getStatusInfo(status: String): Triple<Color, ImageVector, String> {
    return when (status.uppercase()) {
        "SENT" -> Triple(StatusSent, Icons.Filled.CheckCircle, "Sent")
        "PENDING" -> Triple(StatusPending, Icons.Filled.Schedule, "Pending")
        "FAILED" -> Triple(StatusFailed, Icons.Filled.Error, "Failed")
        "PROCESSING" -> Triple(StatusPending, Icons.Filled.Sync, "Processing")
        else -> Triple(StatusPending, Icons.Filled.Schedule, status)
    }
}

/**
 * NFC status indicator.
 */
@Composable
fun NfcStatusIndicator(
    isEnabled: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val color = when {
        !isEnabled -> StatusFailed
        isActive -> SuccessGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val icon = when {
        !isEnabled -> Icons.Filled.NfcOutlined
        isActive -> Icons.Filled.Nfc
        else -> Icons.Filled.NfcOutlined
    }
    
    val label = when {
        !isEnabled -> "NFC Off"
        isActive -> "Active"
        else -> "Ready"
    }
    
    Surface(
        modifier = modifier,
        shape = PaymentShapes.statusBadge,
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = color
            )
        }
    }
}

/**
 * Sync status badge showing pending upload count.
 */
@Composable
fun SyncStatusBadge(
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    val color = if (pendingCount > 0) StatusPending else SuccessGreen
    val label = if (pendingCount > 0) "$pendingCount pending" else "Synced"
    val icon = if (pendingCount > 0) Icons.Filled.Sync else Icons.Filled.CheckCircle
    
    Surface(
        modifier = modifier,
        shape = PaymentShapes.statusBadge,
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = color
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgePreview() {
    MomoTerminalTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            StatusBadge(status = "SENT")
            StatusBadge(status = "PENDING")
            StatusBadge(status = "FAILED")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NfcStatusIndicatorPreview() {
    MomoTerminalTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            NfcStatusIndicator(isEnabled = true, isActive = true)
            NfcStatusIndicator(isEnabled = true, isActive = false)
            NfcStatusIndicator(isEnabled = false, isActive = false)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncStatusBadgePreview() {
    MomoTerminalTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            SyncStatusBadge(pendingCount = 5)
            SyncStatusBadge(pendingCount = 0)
        }
    }
}
