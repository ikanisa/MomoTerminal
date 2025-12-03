package com.momoterminal.presentation.components.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.momoterminal.presentation.theme.MomoAnimation
import com.momoterminal.presentation.theme.PaymentTypography

/**
 * Animated counter that smoothly transitions between numbers.
 * Each digit animates independently for a premium slot-machine effect.
 */
@Composable
fun AnimatedCounter(
    count: String,
    modifier: Modifier = Modifier,
    style: TextStyle = PaymentTypography.amountLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    prefix: String = "",
    suffix: String = ""
) {
    Row(modifier = modifier) {
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                style = style.copy(fontWeight = FontWeight.Normal),
                color = color.copy(alpha = 0.7f)
            )
        }
        
        count.forEach { char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    val direction = if (targetState > initialState) -1 else 1
                    (slideInVertically(
                        initialOffsetY = { direction * it },
                        animationSpec = tween(MomoAnimation.DURATION_MEDIUM, easing = MomoAnimation.EaseOutExpo)
                    ) + fadeIn(tween(MomoAnimation.DURATION_FAST))).togetherWith(
                        slideOutVertically(
                            targetOffsetY = { -direction * it },
                            animationSpec = tween(MomoAnimation.DURATION_FAST)
                        ) + fadeOut(tween(MomoAnimation.DURATION_FAST))
                    )
                },
                label = "digit_$char"
            ) { digit ->
                Text(
                    text = digit.toString(),
                    style = style,
                    color = color
                )
            }
        }
        
        if (suffix.isNotEmpty()) {
            Text(
                text = suffix,
                style = style.copy(fontWeight = FontWeight.Normal),
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Animated amount display with currency prefix.
 */
@Composable
fun AnimatedAmount(
    amount: String,
    currency: String,
    modifier: Modifier = Modifier,
    style: TextStyle = PaymentTypography.amountLarge,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val formattedAmount = formatWithCommas(amount)
    
    AnimatedCounter(
        count = formattedAmount,
        modifier = modifier,
        style = style,
        color = color,
        prefix = "$currency "
    )
}

private fun formatWithCommas(amount: String): String {
    val cleaned = amount.replace(",", "").replace(" ", "")
    if (cleaned.isEmpty() || cleaned == "0") return "0"
    
    return try {
        val value = cleaned.toLong()
        String.format("%,d", value)
    } catch (_: NumberFormatException) {
        amount
    }
}
