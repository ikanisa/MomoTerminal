package com.momoterminal.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.theme.MomoBlue
import com.momoterminal.presentation.theme.MomoYellow

enum class ButtonType {
    PRIMARY, SECONDARY, OUTLINE, TEXT
}

@Composable
fun MomoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ButtonType = ButtonType.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    fullWidth: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding
) {
    val buttonModifier = modifier
        .height(56.dp)
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)

    when (type) {
        ButtonType.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MomoYellow,
                    contentColor = Color.Black,
                    disabledContainerColor = MomoYellow.copy(alpha = 0.5f),
                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(text, isLoading, Color.Black)
            }
        }
        ButtonType.SECONDARY -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MomoBlue,
                    contentColor = Color.White,
                    disabledContainerColor = MomoBlue.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(text, isLoading, Color.White)
            }
        }
        ButtonType.OUTLINE -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(text, isLoading, MaterialTheme.colorScheme.onSurface)
            }
        }
        ButtonType.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(text, isLoading, MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    isLoading: Boolean,
    contentColor: Color
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = contentColor,
            strokeWidth = 2.dp
        )
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}
