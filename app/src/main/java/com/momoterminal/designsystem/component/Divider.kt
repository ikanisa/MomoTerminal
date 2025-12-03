package com.momoterminal.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.theme.MomoTheme

@Composable
fun MomoDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    startIndent: Dp = 0.dp
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(thickness)
            .background(MomoTheme.colors.glassBorder)
    )
}

@Composable
fun LabeledDivider(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = MomoTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(MomoTheme.colors.glassBorder))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MomoTheme.spacing.md)
        )
        Box(Modifier.weight(1f).height(1.dp).background(MomoTheme.colors.glassBorder))
    }
}
