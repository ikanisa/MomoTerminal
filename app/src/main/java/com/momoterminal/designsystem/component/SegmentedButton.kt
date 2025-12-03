package com.momoterminal.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.theme.MomoTheme

@Composable
fun <T> SegmentedButton(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelSelector: (T) -> String = { it.toString() }
) {
    val colors = MomoTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceGlass)
            .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val bgColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "segmentBg"
            )
            val textColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "segmentText"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = MomoTheme.spacing.sm, horizontal = MomoTheme.spacing.md),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelSelector(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor
                )
            }
        }
    }
}
