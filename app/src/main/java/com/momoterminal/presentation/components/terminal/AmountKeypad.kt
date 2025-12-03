package com.momoterminal.presentation.components.terminal

import android.util.Log
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.PaymentTypography

/**
 * Numeric keypad for entering payment amounts.
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: 1, 2, 3
        KeypadRow(
            keys = listOf("1", "2", "3"),
            onKeyClick = { key ->
                Log.d("AmountKeypad", "Key clicked: $key")
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick(key)
            },
            enabled = enabled
        )
        
        // Row 2: 4, 5, 6
        KeypadRow(
            keys = listOf("4", "5", "6"),
            onKeyClick = { key ->
                Log.d("AmountKeypad", "Key clicked: $key")
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick(key)
            },
            enabled = enabled
        )
        
        // Row 3: 7, 8, 9
        KeypadRow(
            keys = listOf("7", "8", "9"),
            onKeyClick = { key ->
                Log.d("AmountKeypad", "Key clicked: $key")
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick(key)
            },
            enabled = enabled
        )
        
        // Row 4: Clear, 0, Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Clear button
            Surface(
                onClick = {
                    Log.d("AmountKeypad", "Clear clicked")
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClearClick()
                },
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.8f),
                enabled = enabled,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "C",
                        style = PaymentTypography.keypadNumber,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // 0 button
            KeypadButton(
                key = "0",
                onClick = {
                    Log.d("AmountKeypad", "Key clicked: 0")
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDigitClick("0")
                },
                modifier = Modifier.weight(1f),
                enabled = enabled
            )
            
            // Backspace button
            Surface(
                onClick = {
                    Log.d("AmountKeypad", "Backspace clicked")
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackspaceClick()
                },
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.8f),
                enabled = enabled,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.8f),
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
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
