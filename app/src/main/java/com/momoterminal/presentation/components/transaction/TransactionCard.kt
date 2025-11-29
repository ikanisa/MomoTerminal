package com.momoterminal.presentation.components.transaction

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.data.local.entity.TransactionEntity
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.StatusFailed
import com.momoterminal.presentation.theme.StatusPending
import com.momoterminal.presentation.theme.StatusSent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Card displaying a single transaction.
 */
@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReceived = transaction.body.contains("received", ignoreCase = true)
    val statusColor = getStatusColor(transaction.status)
    
    // Extract amount from body (simplified parsing)
    val amount = extractAmount(transaction.body)
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transaction direction icon
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (isReceived) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Icon(
                    imageVector = if (isReceived) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    contentDescription = if (isReceived) "Received" else "Sent",
                    modifier = Modifier.padding(8.dp),
                    tint = if (isReceived) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Transaction details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.sender,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatusBadge(
                        status = transaction.status,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
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
                        Text(
                            text = "GHS $amount",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isReceived) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Status badge showing transaction sync status.
 */
@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, icon) = when (status.uppercase()) {
        "SENT" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Filled.CheckCircle
        )
        "PENDING" -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Filled.Schedule
        )
        "FAILED" -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Filled.Error
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Filled.Schedule
        )
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = textColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}

/**
 * Loading placeholder for transaction card.
 */
@Composable
fun TransactionCardPlaceholder(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
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
        "([\\d,]+\\.?\\d*)\\s*GH[SC]?".toRegex(RegexOption.IGNORE_CASE)
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
private fun TransactionCardFailedPreview() {
    MomoTerminalTheme {
        TransactionCard(
            transaction = TransactionEntity(
                id = 3,
                sender = "AirtelTigo Money",
                body = "Transaction failed: GHS 250.00",
                timestamp = System.currentTimeMillis() - 86_400_000,
                status = "FAILED"
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
