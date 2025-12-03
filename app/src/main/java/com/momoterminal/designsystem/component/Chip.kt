package com.momoterminal.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.theme.MomoShapes
import com.momoterminal.designsystem.theme.MomoTheme

@Composable
fun MomoChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = MomoTheme.colors
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else colors.surfaceGlass,
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chipContent"
    )

    Row(
        modifier = modifier
            .clip(MomoShapes.small)
            .background(bgColor)
            .border(1.dp, colors.glassBorder, MomoShapes.small)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = MomoTheme.spacing.md, vertical = MomoTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(MomoTheme.spacing.xs))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

@Composable
fun FilterChipRow(
    chips: List<String>,
    selectedIndex: Int,
    onChipSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)
    ) {
        chips.forEachIndexed { index, label ->
            MomoChip(
                label = label,
                selected = index == selectedIndex,
                onClick = { onChipSelected(index) }
            )
        }
    }
}
