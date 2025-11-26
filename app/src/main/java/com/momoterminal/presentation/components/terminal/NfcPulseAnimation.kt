package com.momoterminal.presentation.components.terminal

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MomoYellow
import com.momoterminal.presentation.theme.SuccessGreen

/**
 * Animated NFC pulse indicator showing the NFC is active.
 */
@Composable
fun NfcPulseAnimation(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    isSuccess: Boolean = false,
    message: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Pulse animation values
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )
    
    val pulse3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse3"
    )
    
    val pulseColor = if (isSuccess) SuccessGreen else MomoYellow
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isActive && !isSuccess) {
                // Draw pulse rings
                Canvas(modifier = Modifier.size(160.dp)) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val maxRadius = size.minDimension / 2
                    
                    // Draw three expanding rings
                    listOf(pulse1, pulse2, pulse3).forEach { pulse ->
                        val radius = maxRadius * pulse
                        val alpha = 1f - pulse
                        
                        drawCircle(
                            color = pulseColor.copy(alpha = alpha * 0.5f),
                            radius = radius,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }
            
            // Static ring for success
            if (isSuccess) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = size.minDimension / 2 - 4.dp.toPx()
                    
                    drawCircle(
                        color = pulseColor,
                        radius = radius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }
            
            // NFC icon in the center
            Icon(
                imageVector = Icons.Filled.Nfc,
                contentDescription = "NFC",
                modifier = Modifier.size(64.dp),
                tint = if (isActive || isSuccess) pulseColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (message != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * NFC status indicator with different states.
 */
@Composable
fun NfcStatusIndicator(
    isEnabled: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val color = when {
        !isEnabled -> MaterialTheme.colorScheme.error
        isActive -> SuccessGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val message = when {
        !isEnabled -> "NFC Disabled"
        isActive -> "Ready for Tap"
        else -> "NFC Ready"
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Nfc,
            contentDescription = "NFC Status",
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NfcPulseAnimationActivePreview() {
    MomoTerminalTheme {
        NfcPulseAnimation(
            isActive = true,
            message = "Hold device near NFC reader"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NfcPulseAnimationSuccessPreview() {
    MomoTerminalTheme {
        NfcPulseAnimation(
            isActive = false,
            isSuccess = true,
            message = "Payment Successful!"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NfcPulseAnimationInactivePreview() {
    MomoTerminalTheme {
        NfcPulseAnimation(
            isActive = false,
            message = "Enter an amount to start"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NfcStatusIndicatorPreview() {
    MomoTerminalTheme {
        Column {
            NfcStatusIndicator(isEnabled = true, isActive = true)
            Spacer(modifier = Modifier.height(16.dp))
            NfcStatusIndicator(isEnabled = true, isActive = false)
            Spacer(modifier = Modifier.height(16.dp))
            NfcStatusIndicator(isEnabled = false, isActive = false)
        }
    }
}
