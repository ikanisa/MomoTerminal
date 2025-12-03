package com.momoterminal.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.theme.MomoTheme

@Composable
fun MomoCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null
) {
    val colors = MomoTheme.colors
    Row(
        modifier = modifier.clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) MaterialTheme.colorScheme.primary else colors.surfaceGlass)
                .border(1.dp, if (checked) Color.Transparent else colors.glassBorder, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (label != null) {
            Spacer(Modifier.width(MomoTheme.spacing.sm))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
fun MomoRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null
) {
    val colors = MomoTheme.colors
    Row(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(colors.surfaceGlass)
                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else colors.glassBorder, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        if (label != null) {
            Spacer(Modifier.width(MomoTheme.spacing.sm))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}
