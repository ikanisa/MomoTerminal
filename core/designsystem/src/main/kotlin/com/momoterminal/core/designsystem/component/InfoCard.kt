package com.momoterminal.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.momoterminal.core.designsystem.theme.MomoTheme

@Composable
fun InfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null
) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(MomoTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(MomoTheme.sizing.iconMd)
                )
                Spacer(Modifier.width(MomoTheme.spacing.md))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trend: String? = null,
    trendPositive: Boolean = true
) {
    val colors = MomoTheme.colors
    GlassCard(modifier = modifier) {
        Column(Modifier.padding(MomoTheme.spacing.md)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(MomoTheme.spacing.xs))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (trend != null) {
                Spacer(Modifier.height(MomoTheme.spacing.xs))
                Text(
                    text = trend,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (trendPositive) colors.credit else colors.debit
                )
            }
        }
    }
}
