package com.momoterminal.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.theme.GlassCardShape
import com.momoterminal.core.designsystem.theme.MomoTheme

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    borderEnabled: Boolean = true,
    elevation: Dp = if (elevated) MomoTheme.elevation.level3 else MomoTheme.elevation.level2,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MomoTheme.colors
    val backgroundColor = if (elevated) colors.surfaceGlassElevated else colors.surfaceGlass
    
    Box(
        modifier = modifier
            .shadow(elevation, GlassCardShape)
            .clip(GlassCardShape)
            .background(backgroundColor)
            .then(
                if (borderEnabled) {
                    Modifier.border(1.dp, colors.glassBorder, GlassCardShape)
                } else Modifier
            )
            .padding(MomoTheme.spacing.lg),
        content = content
    )
}

@Composable
fun GlassCardGradient(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MomoTheme.colors
    val gradient = gradientColors ?: listOf(
        colors.gradientStart.copy(alpha = 0.9f),
        colors.gradientEnd.copy(alpha = 0.9f)
    )
    
    Box(
        modifier = modifier
            .shadow(MomoTheme.elevation.level3, GlassCardShape)
            .clip(GlassCardShape)
            .background(Brush.linearGradient(gradient))
            .border(1.dp, Color.White.copy(alpha = 0.2f), GlassCardShape)
            .padding(MomoTheme.spacing.lg),
        content = content
    )
}
