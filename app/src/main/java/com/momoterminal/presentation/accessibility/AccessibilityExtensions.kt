package com.momoterminal.presentation.accessibility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

/**
 * Minimum touch target size for accessibility (48dp as per Material guidelines).
 */
val MinTouchTargetSize = 48.dp

/**
 * Extension function to ensure minimum touch target size for accessibility.
 */
fun Modifier.minTouchTarget(): Modifier = this.sizeIn(
    minWidth = MinTouchTargetSize,
    minHeight = MinTouchTargetSize
)

/**
 * Extension function to make an element accessible with a content description.
 */
fun Modifier.accessibleClickable(
    description: String,
    role: Role = Role.Button,
    onClick: () -> Unit
): Modifier = this
    .minTouchTarget()
    .semantics {
        contentDescription = description
        this.role = role
    }
    .clickable(onClick = onClick)

/**
 * Format amount for screen reader in a speakable format.
 * E.g., "GHS 100.50" becomes "100 cedis and 50 pesewas"
 */
fun formatAmountForScreenReader(amount: Double, currencyCode: String = "GHS"): String {
    val wholePart = amount.toLong()
    val decimalPart = ((amount - wholePart) * 100).toInt()
    
    return when (currencyCode.uppercase()) {
        "GHS" -> {
            when {
                decimalPart == 0 -> "$wholePart cedis"
                wholePart == 0L -> "$decimalPart pesewas"
                else -> "$wholePart cedis and $decimalPart pesewas"
            }
        }
        "USD" -> {
            when {
                decimalPart == 0 -> "$wholePart dollars"
                wholePart == 0L -> "$decimalPart cents"
                else -> "$wholePart dollars and $decimalPart cents"
            }
        }
        else -> {
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
            try {
                format.currency = Currency.getInstance(currencyCode)
            } catch (e: Exception) {
                // Use default
            }
            format.format(amount)
        }
    }
}

/**
 * Format date for screen reader in a speakable format.
 */
fun formatDateForScreenReader(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return format.format(date)
}

/**
 * Format relative time for screen reader.
 */
fun formatRelativeTimeForScreenReader(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> {
            val minutes = diff / 60_000
            if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
        }
        diff < 86_400_000 -> {
            val hours = diff / 3_600_000
            if (hours == 1L) "1 hour ago" else "$hours hours ago"
        }
        diff < 604_800_000 -> {
            val days = diff / 86_400_000
            if (days == 1L) "yesterday" else "$days days ago"
        }
        else -> formatDateForScreenReader(timestamp)
    }
}

/**
 * Format sync status for screen reader.
 */
fun formatSyncStatusForScreenReader(status: String): String {
    return when (status.uppercase()) {
        "PENDING" -> "waiting to sync"
        "SENT" -> "synced successfully"
        "FAILED" -> "sync failed"
        else -> status.lowercase()
    }
}

/**
 * Format transaction description for screen reader.
 */
@Composable
fun formatTransactionForScreenReader(
    amount: Double?,
    currencyCode: String,
    sender: String,
    timestamp: Long,
    status: String
): String {
    val amountStr = amount?.let { formatAmountForScreenReader(it, currencyCode) } ?: "unknown amount"
    val timeStr = formatRelativeTimeForScreenReader(timestamp)
    val statusStr = formatSyncStatusForScreenReader(status)
    
    return "Transaction from $sender, $amountStr, $timeStr, $statusStr"
}
