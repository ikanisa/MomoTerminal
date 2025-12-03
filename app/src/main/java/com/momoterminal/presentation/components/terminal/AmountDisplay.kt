package com.momoterminal.presentation.components.terminal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.theme.MomoAnimation
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MomoYellow
import com.momoterminal.presentation.theme.PaymentShapes
import com.momoterminal.presentation.theme.PaymentTypography

/**
 * Premium amount display with animated digit transitions.
 * Features smooth scale animation and gradient background when active.
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
    
    // Smooth scale animation
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.02f else 1f,
        animationSpec = tween(MomoAnimation.DURATION_MEDIUM, easing = MomoAnimation.EaseOutExpo),
        label = "scale"
    )
    
    // Animated elevation
    val elevation by animateDpAsState(
        targetValue = if (isActive) 8.dp else 0.dp,
        animationSpec = tween(MomoAnimation.DURATION_MEDIUM),
        label = "elevation"
    )
    
    // Background color transition
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(MomoAnimation.DURATION_MEDIUM),
        label = "background"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = PaymentShapes.amountDisplay,
        color = backgroundColor,
        tonalElevation = elevation,
        shadowElevation = if (isActive) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Label with fade animation
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Amount with animated digits
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                // Currency
                Text(
                    text = currency,
                    style = PaymentTypography.currency,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(end = 8.dp, bottom = 10.dp)
                )
                
                // Animated amount digits
                AnimatedAmountText(
                    amount = formatAmount(displayAmount),
                    isActive = isActive
                )
            }
        }
    }
}

/**
 * Animated text that transitions each digit independently.
 */
@Composable
private fun AnimatedAmountText(
    amount: String,
    isActive: Boolean
) {
    val textColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(MomoAnimation.DURATION_MEDIUM),
        label = "textColor"
    )
    
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        amount.forEach { char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    val direction = if (targetState > initialState) -1 else 1
                    (slideInVertically(
                        initialOffsetY = { direction * it / 2 },
                        animationSpec = tween(MomoAnimation.DURATION_MEDIUM, easing = MomoAnimation.EaseOutExpo)
                    ) + fadeIn(tween(MomoAnimation.DURATION_FAST))).togetherWith(
                        slideOutVertically(
                            targetOffsetY = { -direction * it / 2 },
                            animationSpec = tween(MomoAnimation.DURATION_FAST)
                        ) + fadeOut(tween(MomoAnimation.DURATION_FAST))
                    )
                },
                label = "digit"
            ) { digit ->
                Text(
                    text = digit.toString(),
                    style = PaymentTypography.amountLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = textColor
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
    val color = if (isPositive) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isPositive) "+" else "-",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = currency,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = formatAmount(amount),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AmountDisplayPreview() {
    MomoTerminalTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AmountDisplay(
                amount = "50000",
                label = "Amount to Pay"
            )
            Spacer(modifier = Modifier.height(16.dp))
            AmountDisplay(
                amount = "25000",
                isActive = true,
                label = "Ready for NFC"
            )
        }
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
