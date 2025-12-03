package com.momoterminal.presentation.components.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.theme.MomoAnimation

/**
 * Modern glassmorphism card with subtle blur effect and gradient border.
 * Provides premium, modern aesthetic with smooth press animations.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
    borderGradient: Brush? = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ),
    elevation: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "scale"
    )
    
    val shadowElevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else elevation.value,
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "elevation"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .shadow(shadowElevation.dp, shape)
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (borderGradient != null) {
                    Modifier.border(1.dp, borderGradient, shape)
                } else Modifier
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        content = content
    )
}

/**
 * Elevated card with smooth press animation and shadow.
 */
@Composable
fun ElevatedPressCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(MomoAnimation.DURATION_INSTANT),
        label = "scale"
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 1f else 4f,
        animationSpec = tween(MomoAnimation.DURATION_INSTANT),
        label = "elevation"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .shadow(elevation.dp, shape)
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        content = content
    )
}
