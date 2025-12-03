package com.momoterminal.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.theme.MomoTheme

enum class IconButtonStyle { Filled, Glass, Outlined, Ghost }

@Composable
fun MomoIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    style: IconButtonStyle = IconButtonStyle.Glass,
    size: Dp = 48.dp,
    enabled: Boolean = true
) {
    val colors = MomoTheme.colors
    val (bgColor, borderColor, iconColor) = when (style) {
        IconButtonStyle.Filled -> Triple(
            MaterialTheme.colorScheme.primary,
            Color.Transparent,
            MaterialTheme.colorScheme.onPrimary
        )
        IconButtonStyle.Glass -> Triple(
            colors.surfaceGlass,
            colors.glassBorder,
            MaterialTheme.colorScheme.onSurface
        )
        IconButtonStyle.Outlined -> Triple(
            Color.Transparent,
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.onSurface
        )
        IconButtonStyle.Ghost -> Triple(
            Color.Transparent,
            Color.Transparent,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .then(if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, CircleShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) iconColor else iconColor.copy(alpha = 0.38f),
            modifier = Modifier.size(size * 0.5f)
        )
    }
}
