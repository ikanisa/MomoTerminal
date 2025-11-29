package com.momoterminal.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * OTP input field component with individual digit boxes.
 * Supports 6-digit OTP codes with auto-focus and paste functionality.
 *
 * @param value Current OTP value
 * @param onValueChange Callback when OTP value changes
 * @param modifier Modifier for the component
 * @param digitCount Number of OTP digits (default: 6)
 * @param isError Whether to show error state
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
    
    // Text field value with selection at the end
    var textFieldValue by remember(value) {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            // Only allow digits and limit to digitCount
            val filtered = newValue.text.filter { it.isDigit() }.take(digitCount)
            textFieldValue = newValue.copy(
                text = filtered,
                selection = TextRange(filtered.length)
            )
            onValueChange(filtered)
            
            // Hide keyboard when complete
            if (filtered.length == digitCount) {
                focusManager.clearFocus()
            }
        },
        modifier = modifier.focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
            }
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(digitCount) { index ->
                    OtpDigitBox(
                        digit = value.getOrNull(index)?.toString() ?: "",
                        isFocused = value.length == index,
                        isError = isError,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

/**
 * Individual OTP digit box.
 */
@Composable
private fun OtpDigitBox(
    digit: String,
    isFocused: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        digit.isNotEmpty() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    val backgroundColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center
        )
    }
}
