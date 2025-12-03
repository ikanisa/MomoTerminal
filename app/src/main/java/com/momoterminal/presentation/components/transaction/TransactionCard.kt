package com.momoterminal.presentation.components.transaction

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.data.local.entity.TransactionEntity
import com.momoterminal.presentation.components.status.StatusBadge
import com.momoterminal.presentation.components.terminal.AmountDisplayCompact
import com.momoterminal.presentation.theme.MomoAnimation
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.PaymentShapes
import com.momoterminal.presentation.theme.StatusFailed
import com.momoterminal.presentation.theme.StatusPending
import com.momoterminal.presentation.theme.StatusSent
import com.momoterminal.presentation.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Premium transaction card with fluid press animations.
 * Features smooth scale transitions and elevated design.
 */
@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(MomoAnimation.DURATION_INSTANT),
        label = "scale"
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 1f else 3f,
        animationSpec = tween(MomoAnimation.DURATION_INSTANT),
        label = "elevation"
    )
    
    val isReceived = transaction.body.contains("received", ignoreCase = true)
    val amount = extractAmount(transaction.body)
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(elevation.dp, PaymentShapes.transactionCard),
        shape = PaymentShapes.transactionCard,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transaction direction icon with gradient background
            TransactionIcon(isReceived = isReceived)
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // Transaction details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.sender,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    StatusBadge(
                        status = transaction.status
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(transaction.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (amount != null) {
                        AmountDisplayCompact(
                            amount = amount,
                            isPositive = isReceived
                        )
                    }
                }
            }
        }
    }
}

/**
 * Circular icon indicating transaction direction.
 */
@Composable
private fun TransactionIcon(isReceived: Boolean) {
    val backgroundColor = if (isReceived) {
        SuccessGreen.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    
    val iconColor = if (isReceived) {
        SuccessGreen
    } else {
        MaterialTheme.colorScheme.primary
    }
    
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isReceived) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
            contentDescription = if (isReceived) "Received" else "Sent",
            modifier = Modifier.size(22.dp),
            tint = iconColor
        )
    }
}

/**
 * Get color for transaction status.
 */
@Composable
fun getStatusColor(status: String): Color {
    return when (status.uppercase()) {
        "SENT" -> StatusSent
        "PENDING" -> StatusPending
        "FAILED" -> StatusFailed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/**
 * Get icon for transaction status.
 */
fun getStatusIcon(status: String): ImageVector {
    return when (status.uppercase()) {
        "SENT" -> Icons.Filled.CheckCircle
        "PENDING" -> Icons.Filled.Schedule
        "FAILED" -> Icons.Filled.Error
        else -> Icons.Filled.Schedule
    }
}

/**
 * Extract amount from transaction body.
 */
private fun extractAmount(body: String): String? {
    val patterns = listOf(
        "GH[SC]?\\s*([\\d,]+\\.?\\d*)".toRegex(RegexOption.IGNORE_CASE),
        "([\\d,]+\\.?\\d*)\\s*GH[SC]?".toRegex(RegexOption.IGNORE_CASE),
        "RWF\\s*([\\d,]+\\.?\\d*)".toRegex(RegexOption.IGNORE_CASE),
        "([\\d,]+\\.?\\d*)\\s*RWF".toRegex(RegexOption.IGNORE_CASE)
    )
    
    for (pattern in patterns) {
        val match = pattern.find(body)
        if (match != null) {
            return match.groupValues.getOrNull(1)?.replace(",", "")
        }
    }
    return null
}

/**
 * Format timestamp to human-readable string.
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> SimpleDateFormat("EEE, HH:mm", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
    }
}

/**
 * Loading placeholder for transaction card with shimmer effect.
 */
@Composable
fun TransactionCardPlaceholder(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PaymentShapes.transactionCard,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon placeholder
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionCardReceivedPreview() {
    MomoTerminalTheme {
        TransactionCard(
            transaction = TransactionEntity(
                id = 1,
                sender = "MTN Mobile Money",
                body = "You have received GHS 500.00 from John Doe. Transaction ID: 123456",
                timestamp = System.currentTimeMillis() - 3_600_000,
                status = "SENT"
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionCardPendingPreview() {
    MomoTerminalTheme {
        TransactionCard(
            transaction = TransactionEntity(
                id = 2,
                sender = "Vodafone Cash",
                body = "You have sent GHS 1000.00 to Jane Doe",
                timestamp = System.currentTimeMillis() - 60_000,
                status = "PENDING"
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionCardPlaceholderPreview() {
    MomoTerminalTheme {
        TransactionCardPlaceholder(
            modifier = Modifier.padding(16.dp)
        )
    }
}
