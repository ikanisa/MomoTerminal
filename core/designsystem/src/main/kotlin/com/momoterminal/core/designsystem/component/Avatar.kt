package com.momoterminal.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.theme.MomoTheme

enum class AvatarSize(val dp: Dp) {
    Small(32.dp), Medium(40.dp), Large(56.dp), XLarge(72.dp)
}

@Composable
fun MomoAvatar(
    modifier: Modifier = Modifier,
    initials: String? = null,
    icon: ImageVector? = null,
    size: AvatarSize = AvatarSize.Medium,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val colors = MomoTheme.colors
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, colors.glassBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            initials != null -> Text(
                text = initials.take(2).uppercase(),
                style = when (size) {
                    AvatarSize.Small -> MaterialTheme.typography.labelSmall
                    AvatarSize.Medium -> MaterialTheme.typography.labelLarge
                    AvatarSize.Large -> MaterialTheme.typography.titleMedium
                    AvatarSize.XLarge -> MaterialTheme.typography.titleLarge
                },
                color = contentColor
            )
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(size.dp * 0.5f)
            )
        }
    }
}
