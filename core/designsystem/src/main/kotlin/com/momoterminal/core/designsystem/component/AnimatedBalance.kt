package com.momoterminal.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.momoterminal.core.designsystem.motion.MomoHaptic
import com.momoterminal.core.designsystem.motion.MotionTokens
import com.momoterminal.core.designsystem.motion.performMomoHaptic
import java.text.NumberFormat
import java.util.Locale

/**
 * AnimatedBalance - Smooth number-tweening animation for balance display.
 * 
 * Features:
 * - Animates from old value to new value
 * - Color flash on credit (green) vs debit (red)
 * - Haptic feedback on change
 * - Respects reduced motion settings
 */
@Composable
fun AnimatedBalance(
    balance: Double,
    currencySymbol: String = "",
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    creditColor: Color = Color(0xFF2E7D32),
    debitColor: Color = Color(0xFFC62828),
    defaultColor: Color = MaterialTheme.colorScheme.onSurface,
    showSign: Boolean = false,
    triggerHaptic: Boolean = true
) {
    val view = LocalView.current
    var previousBalance by remember { mutableDoubleStateOf(balance) }
    var changeDirection by remember { mutableStateOf<ChangeDirection?>(null) }
    
    // Animate the numeric value
    val animatedBalance by animateFloatAsState(
        targetValue = balance.toFloat(),
        animationSpec = tween(
            durationMillis = MotionTokens.FINANCIAL,
            easing = MotionTokens.EaseFinancial
        ),
        label = "balance"
    )
    
    // Color animation for credit/debit flash
    val flashColor by animateColorAsState(
        targetValue = when (changeDirection) {
            ChangeDirection.Credit -> creditColor
            ChangeDirection.Debit -> debitColor
            null -> defaultColor
        },
        animationSpec = tween(MotionTokens.FINANCIAL),
        label = "color",
        finishedListener = { changeDirection = null }
    )
    
    // Detect balance changes
    LaunchedEffect(balance) {
        if (balance != previousBalance) {
            changeDirection = if (balance > previousBalance) ChangeDirection.Credit else ChangeDirection.Debit
            if (triggerHaptic) {
                view.performMomoHaptic(MomoHaptic.BalanceUpdate)
            }
            previousBalance = balance
        }
    }
    
    val formattedBalance = formatBalance(animatedBalance.toDouble())
    val prefix = when {
        showSign && balance > 0 -> "+"
        showSign && balance < 0 -> "-"
        else -> ""
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$prefix$currencySymbol$formattedBalance",
            style = style,
            color = flashColor
        )
    }
}

/**
 * Simplified version for transaction amounts
 */
@Composable
fun AnimatedAmount(
    amount: Double,
    currencySymbol: String = "",
    isCredit: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    creditColor: Color = Color(0xFF2E7D32),
    debitColor: Color = Color(0xFFC62828)
) {
    val color = if (isCredit) creditColor else debitColor
    val prefix = if (isCredit) "+" else "-"
    
    val animatedAmount by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOut),
        label = "amount"
    )
    
    Text(
        text = "$prefix$currencySymbol${formatBalance(animatedAmount.toDouble())}",
        style = style,
        color = color,
        modifier = modifier
    )
}

private enum class ChangeDirection { Credit, Debit }

private fun formatBalance(value: Double): String {
    return NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }.format(kotlin.math.abs(value))
}
