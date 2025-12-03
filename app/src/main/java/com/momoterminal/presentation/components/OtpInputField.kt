package com.momoterminal.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.theme.MomoAnimation
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MomoYellow

/**
 * Premium OTP input field with individual animated digit boxes.
 * Features smooth focus transitions, haptic feedback, and error states.
 */
@Composable
fun OtpInputField(
    value: String,
    onValueChange: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    digitCount: Int = 6,
    isError: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    
    var textFieldValue by remember(value) {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }
    
    var previousLength by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val filtered = newValue.text.filter { it.isDigit() }.take(digitCount)
            
            // Haptic feedback on digit entry
            if (filtered.length > previousLength) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            previousLength = filtered.length
            
            textFieldValue = newValue.copy(
                text = filtered,
                selection = TextRange(filtered.length)
            )
            onValueChange(filtered)
            
            // Auto-dismiss keyboard when complete
            if (filtered.length == digitCount) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                focusManager.clearFocus()
            }
        },
        modifier = modifier.focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() }
        ),
        cursorBrush = SolidColor(Color.Transparent),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(digitCount) { index ->
                    OtpDigitBox(
                        digit = value.getOrNull(index)?.toString() ?: "",
                        isFocused = value.length == index,
                        isFilled = index < value.length,
                        isError = isError,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

/**
 * Individual OTP digit box with premium animations.
 */
@Composable
private fun OtpDigitBox(
    digit: String,
    isFocused: Boolean,
    isFilled: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    // Animated border color
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            isFocused -> MomoYellow
            isFilled -> MomoYellow.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "borderColor"
    )
    
    // Animated background
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            isFocused -> MomoYellow.copy(alpha = 0.08f)
            isFilled -> MaterialTheme.colorScheme.surface
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "backgroundColor"
    )
    
    // Animated border width
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 1.5.dp,
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "borderWidth"
    )
    
    // Scale animation for filled state
    val scale by animateFloatAsState(
        targetValue = if (isFilled) 1f else 0.95f,
        animationSpec = tween(MomoAnimation.DURATION_FAST, easing = MomoAnimation.EaseOutBack),
        label = "scale"
    )
    
    // Shadow for focused state
    val elevation by animateDpAsState(
        targetValue = if (isFocused) 4.dp else 0.dp,
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "elevation"
    )
    
    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .scale(scale)
            .shadow(elevation, RoundedCornerShape(14.dp))
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = when {
                isError -> MaterialTheme.colorScheme.error
                digit.isNotEmpty() -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center
        )
        
        // Cursor indicator for focused empty box
        if (isFocused && digit.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .background(
                        MomoYellow,
                        RoundedCornerShape(1.dp)
                    )
                    .align(Alignment.BottomCenter)
            ) {
                // Blinking cursor effect handled by system
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OtpInputFieldPreview() {
    MomoTerminalTheme {
        OtpInputField(
            value = "123",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OtpInputFieldFilledPreview() {
    MomoTerminalTheme {
        OtpInputField(
            value = "123456",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OtpInputFieldErrorPreview() {
    MomoTerminalTheme {
        OtpInputField(
            value = "123456",
            isError = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
