package com.momoterminal.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.theme.MomoShapes
import com.momoterminal.designsystem.theme.MomoTheme

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    var isFocused by remember { mutableStateOf(false) }
    val colors = MomoTheme.colors
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> colors.glassBorder
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clip(MomoShapes.medium)
            .background(colors.surfaceGlass)
            .border(1.dp, borderColor, MomoShapes.medium)
            .padding(horizontal = MomoTheme.spacing.md, vertical = MomoTheme.spacing.sm),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(MomoTheme.spacing.sm))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
                trailingIcon?.invoke()
            }
        }
    )
}

@Composable
fun AmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String = "GH₵",
    placeholder: String = "0.00"
) {
    val colors = MomoTheme.colors

    Row(
        modifier = modifier
            .clip(MomoShapes.medium)
            .background(colors.surfaceGlass)
            .border(1.dp, colors.glassBorder, MomoShapes.medium)
            .padding(horizontal = MomoTheme.spacing.md, vertical = MomoTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currencySymbol,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(MomoTheme.spacing.sm))
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() || it == '.' }) {
                    onValueChange(newValue)
                }
            },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
