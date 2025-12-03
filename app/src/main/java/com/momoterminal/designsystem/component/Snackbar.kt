package com.momoterminal.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.theme.MomoShapes
import com.momoterminal.designsystem.theme.MomoTheme

enum class SnackbarType { Info, Success, Warning, Error }

@Composable
fun MomoSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    type: SnackbarType = SnackbarType.Info,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = MomoTheme.colors
    val accentColor = when (type) {
        SnackbarType.Info -> MaterialTheme.colorScheme.primary
        SnackbarType.Success -> colors.credit
        SnackbarType.Warning -> colors.warning
        SnackbarType.Error -> MaterialTheme.colorScheme.error
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MomoShapes.medium)
            .background(colors.surfaceGlassElevated)
            .border(1.dp, colors.glassBorder, MomoShapes.medium)
            .padding(MomoTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(MomoTheme.spacing.sm))
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.width(MomoTheme.spacing.sm))
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}
