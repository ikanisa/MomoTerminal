package com.momoterminal.presentation.components.status

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.theme.MomoAnimation
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.StatusFailed
import com.momoterminal.presentation.theme.StatusPending
import com.momoterminal.presentation.theme.StatusSent
import com.momoterminal.presentation.theme.SuccessGreen

/**
 * Compact status badge with animated indicator.
 * Shows transaction status with appropriate color and optional pulse animation.
 */
@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val statusInfo = getStatusInfo(status)
    
    // Animated background color
    val backgroundColor by animateColorAsState(
        targetValue = statusInfo.color.copy(alpha = 0.12f),
        animationSpec = tween(MomoAnimation.DURATION_MEDIUM),
        label = "backgroundColor"
    )
    
    // Pulse animation for pending status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    val isPending = status.uppercase() == "PENDING"
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .then(if (isPending) Modifier.alpha(pulseAlpha) else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusInfo.color)
            )
            
            if (showLabel) {
                Text(
                    text = statusInfo.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = statusInfo.color
                )
            }
        }
    }
}

/**
 * Larger status badge with icon for detail screens.
 */
@Composable
fun StatusBadgeLarge(
    status: String,
    modifier: Modifier = Modifier
) {
    val statusInfo = getStatusInfo(status)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(statusInfo.color.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = statusInfo.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = statusInfo.color
            )
            
            Text(
                text = statusInfo.label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = statusInfo.color
            )
        }
    }
}

/**
 * Status information data class.
 */
private data class StatusInfo(
    val label: String,
    val color: Color,
    val icon: ImageVector
)

/**
 * Get status information based on status string.
 */
private fun getStatusInfo(status: String): StatusInfo {
    return when (status.uppercase()) {
        "SENT", "SUCCESS", "COMPLETED" -> StatusInfo(
            label = "Sent",
            color = SuccessGreen,
            icon = Icons.Filled.CheckCircle
        )
        "PENDING", "PROCESSING" -> StatusInfo(
            label = "Pending",
            color = StatusPending,
            icon = Icons.Filled.Schedule
        )
        "FAILED", "ERROR" -> StatusInfo(
            label = "Failed",
            color = StatusFailed,
            icon = Icons.Filled.Error
        )
        else -> StatusInfo(
            label = status,
            color = StatusPending,
            icon = Icons.Filled.Schedule
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgePreview() {
    MomoTerminalTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusBadge(status = "SENT")
            StatusBadge(status = "PENDING")
            StatusBadge(status = "FAILED")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgeLargePreview() {
    MomoTerminalTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusBadgeLarge(status = "SENT")
            StatusBadgeLarge(status = "PENDING")
        }
    }
}
