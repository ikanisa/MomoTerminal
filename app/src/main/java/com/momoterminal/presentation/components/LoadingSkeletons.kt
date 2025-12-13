package com.momoterminal.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer loading skeleton components.
 * Shows animated placeholders while data is loading.
 */

@Composable
fun ShimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )
}

/**
 * Basic skeleton box with shimmer effect.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
    width: Dp? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(shape)
            .background(ShimmerBrush())
    )
}

/**
 * Skeleton for transaction list item.
 */
@Composable
fun TransactionSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            // Icon placeholder
            SkeletonBox(
                modifier = Modifier.size(40.dp),
                height = 40.dp,
                width = 40.dp,
                shape = CircleShape
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // Title
                SkeletonBox(width = 120.dp, height = 16.dp)
                Spacer(modifier = Modifier.height(8.dp))
                // Subtitle
                SkeletonBox(width = 80.dp, height = 12.dp)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                // Amount
                SkeletonBox(width = 60.dp, height = 16.dp)
                Spacer(modifier = Modifier.height(8.dp))
                // Date
                SkeletonBox(width = 50.dp, height = 12.dp)
            }
        }
    }
}

/**
 * Skeleton for vending machine card.
 */
@Composable
fun VendingMachineSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Image placeholder
            SkeletonBox(
                modifier = Modifier.fillMaxWidth(),
                height = 150.dp,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Title
            SkeletonBox(width = 150.dp, height = 20.dp)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Location
            SkeletonBox(width = 100.dp, height = 14.dp)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Availability indicator
            Row {
                SkeletonBox(width = 60.dp, height = 12.dp)
                Spacer(modifier = Modifier.width(12.dp))
                SkeletonBox(width = 80.dp, height = 12.dp)
            }
        }
    }
}

/**
 * Skeleton for profile card.
 */
@Composable
fun ProfileSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        SkeletonBox(
            modifier = Modifier.size(80.dp),
            height = 80.dp,
            width = 80.dp,
            shape = CircleShape
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Name
        SkeletonBox(width = 120.dp, height = 20.dp)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Phone
        SkeletonBox(width = 100.dp, height = 16.dp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(3) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SkeletonBox(width = 40.dp, height = 24.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    SkeletonBox(width = 60.dp, height = 12.dp)
                }
            }
        }
    }
}

/**
 * Skeleton for settings list item.
 */
@Composable
fun SettingsItemSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        SkeletonBox(
            modifier = Modifier.size(24.dp),
            height = 24.dp,
            width = 24.dp,
            shape = RoundedCornerShape(4.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(width = 100.dp, height = 16.dp)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonBox(width = 150.dp, height = 12.dp)
        }
        
        // Trailing content
        SkeletonBox(width = 40.dp, height = 20.dp)
    }
}

/**
 * Shows a list of skeleton items.
 */
@Composable
fun SkeletonList(
    count: Int = 5,
    itemContent: @Composable (index: Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(count) { index ->
            itemContent(index)
        }
    }
}
