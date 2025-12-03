package com.momoterminal.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MomoBadge(
    modifier: Modifier = Modifier,
    count: Int? = null,
    color: Color = MaterialTheme.colorScheme.error
) {
    val displayText = when {
        count == null -> null
        count > 99 -> "99+"
        count > 0 -> count.toString()
        else -> null
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = if (displayText != null) 18.dp else 8.dp, minHeight = 8.dp)
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = if (displayText != null) 4.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        if (displayText != null) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun BadgedBox(
    badge: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier) {
        content()
        Box(Modifier.align(Alignment.TopEnd)) { badge() }
    }
}
