package com.momoterminal.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.theme.MomoAnimation
import com.momoterminal.presentation.theme.MomoBlue
import com.momoterminal.presentation.theme.MomoYellow

enum class ButtonType {
    PRIMARY, SECONDARY, OUTLINE, TEXT, DANGER
}

/**
 * Premium button component with fluid press animations.
 * Features scale animation, shadow transitions, and loading state.
 */
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Smooth scale animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.97f else 1f,
        animationSpec = tween(MomoAnimation.DURATION_INSTANT),
        label = "scale"
    )
    
    // Shadow animation
    val shadowElevation by animateDpAsState(
        targetValue = when {
            !enabled -> 0.dp
            isPressed -> 1.dp
            type == ButtonType.PRIMARY -> 4.dp
            type == ButtonType.SECONDARY -> 4.dp
            else -> 0.dp
        },
        animationSpec = tween(MomoAnimation.DURATION_INSTANT),
        label = "shadow"
    )
    
    val buttonModifier = modifier
        .height(56.dp)
        .scale(scale)
        .shadow(shadowElevation, shape)
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)

    when (type) {
        ButtonType.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MomoYellow,
                    contentColor = Color.Black,
                    disabledContainerColor = MomoYellow.copy(alpha = 0.4f),
                    disabledContentColor = Color.Black.copy(alpha = 0.4f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
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
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MomoBlue,
                    contentColor = Color.White,
                    disabledContainerColor = MomoBlue.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
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
                interactionSource = interactionSource,
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
                interactionSource = interactionSource,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(text, isLoading, MaterialTheme.colorScheme.primary)
            }
        }
        ButtonType.DANGER -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.4f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(text, isLoading, MaterialTheme.colorScheme.onError)
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
            strokeWidth = 2.5.dp
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
