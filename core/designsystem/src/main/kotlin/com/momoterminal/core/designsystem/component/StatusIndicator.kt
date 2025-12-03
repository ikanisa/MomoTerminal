package com.momoterminal.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.theme.MomoTheme

enum class StatusType { SUCCESS, WARNING, ERROR, INFO, PENDING }

@Composable
fun StatusIndicator(
    type: StatusType,
    label: String,
    modifier: Modifier = Modifier
) {
    val (dotColor, textColor) = when (type) {
        StatusType.SUCCESS -> MomoTheme.colors.credit to MomoTheme.colors.credit
        StatusType.WARNING -> MomoTheme.colors.warning to MomoTheme.colors.warning
        StatusType.ERROR -> MomoTheme.colors.debit to MomoTheme.colors.debit
        StatusType.INFO -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primary
        StatusType.PENDING -> MaterialTheme.colorScheme.outline to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

@Composable
fun StatusBadge(
    type: StatusType,
    label: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (type) {
        StatusType.SUCCESS -> MomoTheme.colors.credit.copy(alpha = 0.15f) to MomoTheme.colors.credit
        StatusType.WARNING -> MomoTheme.colors.warning.copy(alpha = 0.15f) to MomoTheme.colors.warning
        StatusType.ERROR -> MomoTheme.colors.debit.copy(alpha = 0.15f) to MomoTheme.colors.debit
        StatusType.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        StatusType.PENDING -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(bgColor)
            .padding(horizontal = MomoTheme.spacing.sm, vertical = MomoTheme.spacing.xs)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
