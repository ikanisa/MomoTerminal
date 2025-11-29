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
 * Format amount in pesewas for screen reader in a speakable format.
 * E.g., 10050 pesewas becomes "100 cedis and 50 pesewas"
 */
fun formatAmountInPesewasForScreenReader(amountInPesewas: Long, currencyCode: String = "GHS"): String {
    val wholePart = amountInPesewas / 100
    val decimalPart = (amountInPesewas % 100).toInt()
    
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
            val amount = amountInPesewas / 100.0
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
 * Format amount for screen reader in a speakable format.
 * E.g., "GHS 100.50" becomes "100 cedis and 50 pesewas"
 * 
 * @deprecated Use formatAmountInPesewasForScreenReader for Long amounts
 */
fun formatAmountForScreenReader(amount: Double, currencyCode: String = "GHS"): String {
    // Convert to pesewas to avoid floating-point precision issues
    val amountInPesewas = (amount * 100).toLong()
    return formatAmountInPesewasForScreenReader(amountInPesewas, currencyCode)
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
 * Format transaction description for screen reader using pesewas.
 */
@Composable
fun formatTransactionForScreenReader(
    amountInPesewas: Long?,
    currencyCode: String,
    sender: String,
    timestamp: Long,
    status: String
): String {
    val amountStr = amountInPesewas?.let { formatAmountInPesewasForScreenReader(it, currencyCode) } ?: "unknown amount"
    val timeStr = formatRelativeTimeForScreenReader(timestamp)
    val statusStr = formatSyncStatusForScreenReader(status)
    
    return "Transaction from $sender, $amountStr, $timeStr, $statusStr"
}

/**
 * Format transaction description for screen reader (Double version for backward compatibility).
 * @deprecated Use the Long/pesewas version instead
 */
@Composable
fun formatTransactionForScreenReader(
    amount: Double?,
    currencyCode: String,
    sender: String,
    timestamp: Long,
    status: String
): String {
    val amountInPesewas = amount?.let { (it * 100).toLong() }
    return formatTransactionForScreenReader(amountInPesewas, currencyCode, sender, timestamp, status)
}
