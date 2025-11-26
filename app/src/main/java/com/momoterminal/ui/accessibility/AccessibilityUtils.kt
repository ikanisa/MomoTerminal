package com.momoterminal.ui.accessibility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Minimum touch target size for accessibility (48dp per Material guidelines).
 */
val MinimumTouchTargetSize = 48.dp

/**
 * Format amount for accessibility in a speakable format.
 * Converts amounts like "100.50 GHS" to "100 cedis and 50 pesewas".
 *
 * @param amount The amount to format
 * @param currencyCode The currency code (e.g., "GHS", "USD")
 * @return Speakable string for screen readers
 */
fun formatAmountForAccessibility(amount: Double, currencyCode: String = "GHS"): String {
    val wholePart = amount.toLong()
    val decimalPart = kotlin.math.round((amount - wholePart) * 100).toInt()

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
        "EUR" -> {
            when {
                decimalPart == 0 -> "$wholePart euros"
                wholePart == 0L -> "$decimalPart cents"
                else -> "$wholePart euros and $decimalPart cents"
            }
        }
        "GBP" -> {
            when {
                decimalPart == 0 -> "$wholePart pounds"
                wholePart == 0L -> "$decimalPart pence"
                else -> "$wholePart pounds and $decimalPart pence"
            }
        }
        else -> {
            try {
                val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
                format.currency = Currency.getInstance(currencyCode)
                format.format(amount)
            } catch (e: Exception) {
                "$amount $currencyCode"
            }
        }
    }
}

/**
 * Modifier extension for financial amount semantics.
 * Provides proper content description with currency for screen readers.
 *
 * @param amount The amount to describe
 * @param currencyCode The currency code
 * @param isCredit Whether this is money received (affects announcement)
 */
fun Modifier.semanticsForAmount(
    amount: Double,
    currencyCode: String = "GHS",
    isCredit: Boolean? = null
): Modifier {
    val amountDescription = formatAmountForAccessibility(amount, currencyCode)
    val prefix = when (isCredit) {
        true -> "Received "
        false -> "Sent "
        null -> ""
    }
    return this.semantics {
        contentDescription = "$prefix$amountDescription"
    }
}

/**
 * Modifier extension for transaction item semantics.
 * Provides comprehensive content description for transaction items.
 *
 * @param amount The transaction amount
 * @param currencyCode The currency code
 * @param senderOrRecipient The sender or recipient name
 * @param status The transaction status
 * @param isCredit Whether this is money received
 * @param timestamp Formatted timestamp string
 */
fun Modifier.semanticsForTransaction(
    amount: Double,
    currencyCode: String,
    senderOrRecipient: String,
    status: String,
    isCredit: Boolean,
    timestamp: String
): Modifier {
    val amountDesc = formatAmountForAccessibility(amount, currencyCode)
    val direction = if (isCredit) "from" else "to"
    val transactionType = if (isCredit) "Received" else "Sent"
    val statusDesc = when (status.uppercase()) {
        "PENDING" -> "pending"
        "SENT", "COMPLETED", "SUCCESS" -> "completed"
        "FAILED" -> "failed"
        else -> status.lowercase()
    }

    return this.semantics {
        contentDescription = "$transactionType $amountDesc $direction $senderOrRecipient, $statusDesc, $timestamp"
    }
}

/**
 * Modifier extension for accessible clickable elements.
 * Ensures minimum touch target size and proper semantics.
 *
 * @param description Content description for screen readers
 * @param role The semantic role (Button, Checkbox, etc.)
 * @param stateDesc Optional state description
 * @param onClick Click handler
 */
fun Modifier.accessibleClickable(
    description: String,
    role: Role = Role.Button,
    stateDesc: String? = null,
    onClick: () -> Unit
): Modifier {
    return this
        .sizeIn(minWidth = MinimumTouchTargetSize, minHeight = MinimumTouchTargetSize)
        .semantics {
            contentDescription = description
            this.role = role
            stateDesc?.let { stateDescription = it }
        }
        .clickable(onClick = onClick)
}

/**
 * Accessible button with proper touch target size and semantics.
 *
 * @param onClick Click handler
 * @param modifier Modifier for the button
 * @param description Content description for screen readers
 * @param enabled Whether the button is enabled
 * @param content Button content
 */
@Composable
fun AccessibleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .sizeIn(minWidth = MinimumTouchTargetSize, minHeight = MinimumTouchTargetSize)
            .then(
                if (description != null) {
                    Modifier.semantics {
                        contentDescription = description
                        stateDescription = if (enabled) "enabled" else "disabled"
                    }
                } else {
                    Modifier
                }
            ),
        enabled = enabled
    ) {
        content()
    }
}

/**
 * Accessible card with proper touch target and semantics.
 *
 * @param onClick Click handler
 * @param modifier Modifier for the card
 * @param description Content description for screen readers
 * @param content Card content
 */
@Composable
fun AccessibleCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .sizeIn(minWidth = MinimumTouchTargetSize, minHeight = MinimumTouchTargetSize)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        content()
    }
}

/**
 * Wrapper that clears semantics from children and applies a single description.
 * Useful for grouping elements that should be announced as one.
 *
 * @param description The content description for the group
 * @param modifier Modifier for the box
 * @param content The grouped content
 */
@Composable
fun AccessibleGroup(
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = description
        }
    ) {
        content()
    }
}

/**
 * Check if high contrast mode is enabled.
 * This is a simplified check - in production, you would use AccessibilityManager.
 */
@Composable
fun isHighContrastEnabled(): Boolean {
    val context = LocalContext.current
    val accessibilityManager = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
    return accessibilityManager?.isEnabled == true
}

/**
 * Format relative time for accessibility.
 *
 * @param timestamp The timestamp in milliseconds
 * @return Speakable relative time string
 */
fun formatRelativeTimeForAccessibility(timestamp: Long): String {
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
        else -> {
            val date = java.util.Date(timestamp)
            java.text.SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
        }
    }
}

/**
 * Format transaction status for accessibility.
 *
 * @param status The status string
 * @return Speakable status description
 */
fun formatStatusForAccessibility(status: String): String {
    return when (status.uppercase()) {
        "PENDING" -> "transaction pending, waiting to be processed"
        "SENT", "COMPLETED", "SUCCESS" -> "transaction completed successfully"
        "FAILED" -> "transaction failed"
        "PROCESSING" -> "transaction is being processed"
        "CANCELLED" -> "transaction was cancelled"
        else -> status.lowercase()
    }
}
