package com.momoterminal.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.momoterminal.designsystem.theme.MomoShapes
import com.momoterminal.designsystem.theme.MomoTheme

@Composable
fun MomoTooltip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MomoShapes.small)
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(horizontal = MomoTheme.spacing.md, vertical = MomoTheme.spacing.sm)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.inverseOnSurface
        )
    }
}
