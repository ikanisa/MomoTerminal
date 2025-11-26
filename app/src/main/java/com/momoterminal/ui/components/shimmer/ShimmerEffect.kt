package com.momoterminal.ui.components.shimmer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates a shimmer brush for loading animations.
 *
 * @param shimmerColors The colors to use for the shimmer effect
 * @param durationMillis The duration of one shimmer animation cycle
 */
@Composable
fun shimmerBrush(
    shimmerColors: List<Color> = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ),
    durationMillis: Int = 1500
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - 500f, translateAnimation - 500f),
        end = Offset(translateAnimation, translateAnimation)
    )
}

/**
 * A shimmer box placeholder.
 *
 * @param modifier Modifier for the box
 * @param width Width of the box (null for fillMaxWidth)
 * @param height Height of the box
 * @param cornerRadius Corner radius for the box
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 48.dp,
    cornerRadius: Dp = 8.dp
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

/**
 * A shimmer circle placeholder.
 *
 * @param modifier Modifier for the circle
 * @param size Size of the circle
 */
@Composable
fun ShimmerCircle(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush)
    )
}

/**
 * A shimmer line placeholder.
 *
 * @param modifier Modifier for the line
 * @param width Width of the line (null for fillMaxWidth)
 * @param height Height of the line
 */
@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(brush)
    )
}

/**
 * Transaction card shimmer placeholder for loading states.
 *
 * @param modifier Modifier for the card
 */
@Composable
fun TransactionCardShimmer(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            ShimmerCircle(size = 48.dp)

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                ShimmerLine(
                    width = 120.dp,
                    height = 16.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerLine(
                    width = 80.dp,
                    height = 12.dp
                )
            }

            // Amount placeholder
            Column(
                horizontalAlignment = Alignment.End
            ) {
                ShimmerLine(
                    width = 60.dp,
                    height = 16.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerLine(
                    width = 40.dp,
                    height = 12.dp
                )
            }
        }
    }
}

/**
 * Balance card shimmer placeholder for loading states.
 *
 * @param modifier Modifier for the card
 */
@Composable
fun BalanceCardShimmer(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Label placeholder
            ShimmerLine(
                width = 100.dp,
                height = 14.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount placeholder
            ShimmerLine(
                width = 180.dp,
                height = 48.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary info placeholder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ShimmerLine(
                    width = 80.dp,
                    height = 12.dp
                )
                ShimmerLine(
                    width = 80.dp,
                    height = 12.dp
                )
            }
        }
    }
}

/**
 * Transaction list shimmer for full screen loading state.
 *
 * @param modifier Modifier for the list
 * @param itemCount Number of shimmer items to show
 */
@Composable
fun TransactionListShimmer(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Balance card shimmer
        BalanceCardShimmer()

        Spacer(modifier = Modifier.height(8.dp))

        // Section header shimmer
        ShimmerLine(
            width = 140.dp,
            height = 20.dp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Transaction cards shimmer
        repeat(itemCount) {
            TransactionCardShimmer()
        }
    }
}

/**
 * Simple content shimmer for generic loading states.
 *
 * @param modifier Modifier for the content
 */
@Composable
fun ContentShimmer(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerLine(height = 24.dp)
        ShimmerLine(width = 200.dp, height = 16.dp)
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(height = 120.dp)
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerLine(height = 16.dp)
        ShimmerLine(width = 180.dp, height = 16.dp)
        ShimmerLine(width = 160.dp, height = 16.dp)
    }
}
