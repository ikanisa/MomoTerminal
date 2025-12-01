package com.momoterminal.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momoterminal.presentation.theme.SuccessGreen
import com.momoterminal.presentation.theme.MomoYellow

/**
 * Animated status badge with pulse effect for active states.
 * 
 * @param text The text to display in the badge
 * @param isActive Whether the badge represents an active state (enables pulse animation)
 * @param backgroundColor Background color of the badge
 * @param textColor Text color
 */
@Composable
fun AnimatedStatusBadge(
    text: String,
    isActive: Boolean = false,
    backgroundColor: Color = SuccessGreen,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    // Pulse animation for active badges
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 0.7f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .scale(if (isActive) scale else 1f)
            .background(
                color = backgroundColor.copy(alpha = if (isActive) alpha else 1f),
                shape = CircleShape
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Pulse dot for active status
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(textColor, CircleShape)
                )
            }
            
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Status badge variants for common use cases.
 */
object StatusBadgeDefaults {
    @Composable
    fun Active(modifier: Modifier = Modifier) {
        AnimatedStatusBadge(
            text = "Active",
            isActive = true,
            backgroundColor = SuccessGreen,
            modifier = modifier
        )
    }
    
    @Composable
    fun Inactive(modifier: Modifier = Modifier) {
        AnimatedStatusBadge(
            text = "Inactive",
            isActive = false,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    }
    
    @Composable
    fun Testing(modifier: Modifier = Modifier) {
        AnimatedStatusBadge(
            text = "Testing",
            isActive = true,
            backgroundColor = MomoYellow,
            textColor = Color.Black,
            modifier = modifier
        )
    }
    
    @Composable
    fun Success(modifier: Modifier = Modifier) {
        AnimatedStatusBadge(
            text = "Success",
            isActive = false,
            backgroundColor = SuccessGreen,
            modifier = modifier
        )
    }
    
    @Composable
    fun Failed(modifier: Modifier = Modifier) {
        AnimatedStatusBadge(
            text = "Failed",
            isActive = false,
            backgroundColor = MaterialTheme.colorScheme.error,
            modifier = modifier
        )
    }
}
