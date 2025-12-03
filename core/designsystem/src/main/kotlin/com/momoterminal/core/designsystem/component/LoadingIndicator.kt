package com.momoterminal.core.designsystem.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.theme.MomoTheme

@Composable
fun MomoLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = color,
        strokeWidth = 3.dp
    )
}

@Composable
fun PulsingDots(
    modifier: Modifier = Modifier,
    dotCount: Int = 3,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(dotCount) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 150)
                ),
                label = "dot$index"
            )
            Box(
                Modifier
                    .size(8.dp)
                    .scale(scale)
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    message: String? = null,
    content: @Composable () -> Unit
) {
    Box(modifier) {
        content()
        if (isLoading) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(MomoTheme.colors.surfaceGlass),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MomoLoadingIndicator()
                    if (message != null) {
                        Spacer(Modifier.height(MomoTheme.spacing.md))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
