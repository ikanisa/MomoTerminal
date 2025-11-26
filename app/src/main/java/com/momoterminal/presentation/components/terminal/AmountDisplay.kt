package com.momoterminal.presentation.components.terminal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MomoYellow
import com.momoterminal.presentation.theme.PaymentShapes
import com.momoterminal.presentation.theme.PaymentTypography

/**
 * Display component for showing the current payment amount.
 */
@Composable
fun AmountDisplay(
    amount: String,
    currency: String = "GHS",
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    label: String? = null
) {
    val displayAmount = if (amount.isEmpty()) "0" else amount
    
    // Animate scale when active
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    
    // Animate background color
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "background"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = PaymentShapes.amountDisplay,
        color = backgroundColor,
        tonalElevation = if (isActive) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = currency,
                    style = PaymentTypography.currency,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                )
                
                Text(
                    text = formatAmount(displayAmount),
                    style = PaymentTypography.amountLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Format amount with thousand separators.
 */
private fun formatAmount(amount: String): String {
    val numericAmount = amount.replace(",", "").replace(".", "")
    if (numericAmount.isEmpty()) return "0"
    
    return try {
        val value = numericAmount.toLong()
        String.format("%,d", value)
    } catch (_: NumberFormatException) {
        amount
    }
}

/**
 * Compact amount display for lists and cards.
 */
@Composable
fun AmountDisplayCompact(
    amount: String,
    currency: String = "GHS",
    modifier: Modifier = Modifier,
    isPositive: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isPositive) "+" else "-",
            style = MaterialTheme.typography.titleMedium,
            color = if (isPositive) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
        Text(
            text = "$currency ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatAmount(amount),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = if (isPositive) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AmountDisplayPreview() {
    MomoTerminalTheme {
        AmountDisplay(
            amount = "50000",
            modifier = Modifier.padding(16.dp),
            label = "Amount to Pay"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AmountDisplayActivePreview() {
    MomoTerminalTheme {
        AmountDisplay(
            amount = "25000",
            modifier = Modifier.padding(16.dp),
            isActive = true,
            label = "Ready for NFC"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AmountDisplayCompactPreview() {
    MomoTerminalTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AmountDisplayCompact(amount = "50000", isPositive = true)
            Spacer(modifier = Modifier.height(8.dp))
            AmountDisplayCompact(amount = "25000", isPositive = false)
        }
    }
}
