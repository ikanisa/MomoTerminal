package com.momoterminal.presentation.components.terminal

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.theme.MomoAnimation
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MomoYellow
import com.momoterminal.presentation.theme.PaymentTypography

/**
 * Modern numeric keypad with fluid press animations.
 * Features haptic feedback and premium visual design.
 */
@Composable
fun AmountKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: 1, 2, 3
        KeypadRow(
            keys = listOf("1", "2", "3"),
            onKeyClick = { key ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick(key)
            },
            enabled = enabled
        )
        
        // Row 2: 4, 5, 6
        KeypadRow(
            keys = listOf("4", "5", "6"),
            onKeyClick = { key ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick(key)
            },
            enabled = enabled
        )
        
        // Row 3: 7, 8, 9
        KeypadRow(
            keys = listOf("7", "8", "9"),
            onKeyClick = { key ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick(key)
            },
            enabled = enabled
        )
        
        // Row 4: Clear, 0, Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clear button
            KeypadActionButton(
                label = "C",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClearClick()
                },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                isDestructive = true
            )
            
            // 0 button
            KeypadButton(
                key = "0",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDigitClick("0")
                },
                modifier = Modifier.weight(1f),
                enabled = enabled
            )
            
            // Backspace button
            KeypadIconButton(
                icon = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackspaceClick()
                },
                modifier = Modifier.weight(1f),
                enabled = enabled
            )
        }
    }
}

@Composable
private fun KeypadRow(
    keys: List<String>,
    onKeyClick: (String) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        keys.forEach { key ->
            KeypadButton(
                key = key,
                onClick = { onKeyClick(key) },
                modifier = Modifier.weight(1f),
                enabled = enabled
            )
        }
    }
}

@Composable
private fun KeypadButton(
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = tween(MomoAnimation.DURATION_INSTANT),
        label = "scale"
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0f else 2f,
        animationSpec = tween(MomoAnimation.DURATION_INSTANT),
        label = "elevation"
    )
    
    val backgroundColor = if (isPressed && enabled) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1.2f)
            .scale(scale)
            .shadow(elevation.dp, CircleShape)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key,
            style = PaymentTypography.keypadNumber.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        )
    }
}

@Composable
private fun KeypadActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDestructive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = tween(MomoAnimation.DURATION_INSTANT),
        label = "scale"
    )
    
    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isDestructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1.2f)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = PaymentTypography.keypadNumber.copy(
                fontWeight = FontWeight.Bold
            ),
            color = textColor
        )
    }
}

@Composable
private fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = tween(MomoAnimation.DURATION_INSTANT),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .aspectRatio(1.2f)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(28.dp),
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AmountKeypadPreview() {
    MomoTerminalTheme {
        AmountKeypad(
            onDigitClick = {},
            onBackspaceClick = {},
            onClearClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
