package com.momoterminal.designsystem.interaction

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import com.momoterminal.designsystem.motion.MomoHaptic
import com.momoterminal.designsystem.motion.MotionTokens
import com.momoterminal.designsystem.motion.performMomoHaptic

/**
 * Combined clickable with scale feedback and haptic.
 */
fun Modifier.interactiveClickable(
    enabled: Boolean = true,
    haptic: MomoHaptic = MomoHaptic.Tap,
    scaleOnPress: Float = 0.97f,
    onClick: () -> Unit
) = composed {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleOnPress else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "clickScale"
    )
    
    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {
                view.performMomoHaptic(haptic)
                onClick()
            }
        )
}

/**
 * Bounce-in animation when first composed.
 */
fun Modifier.bounceIn(
    delay: Int = 0,
    enabled: Boolean = true
) = composed {
    var triggered by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        triggered = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (triggered && enabled) 1f else 0.8f,
        animationSpec = tween(
            durationMillis = MotionTokens.STANDARD,
            delayMillis = delay,
            easing = MotionTokens.EaseOutBack
        ),
        label = "bounceIn"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (triggered && enabled) 1f else 0f,
        animationSpec = tween(
            durationMillis = MotionTokens.QUICK,
            delayMillis = delay,
            easing = MotionTokens.EaseOut
        ),
        label = "fadeIn"
    )
    
    graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}

/**
 * Slide-up animation when first composed.
 */
fun Modifier.slideUp(
    delay: Int = 0,
    offsetY: Float = 50f
) = composed {
    var triggered by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        triggered = true
    }
    
    val offset by animateFloatAsState(
        targetValue = if (triggered) 0f else offsetY,
        animationSpec = tween(
            durationMillis = MotionTokens.STANDARD,
            delayMillis = delay,
            easing = MotionTokens.EaseOutExpo
        ),
        label = "slideUp"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = tween(
            durationMillis = MotionTokens.QUICK,
            delayMillis = delay
        ),
        label = "fadeIn"
    )
    
    graphicsLayer {
        translationY = offset
        this.alpha = alpha
    }
}

/**
 * Hover/focus elevation effect.
 */
fun Modifier.elevateOnInteraction(
    baseElevation: Float = 2f,
    elevatedElevation: Float = 8f
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) elevatedElevation else baseElevation,
        animationSpec = tween(MotionTokens.QUICK),
        label = "elevation"
    )
    
    graphicsLayer {
        shadowElevation = elevation
    }
}

/**
 * Shimmer loading effect modifier.
 */
fun Modifier.shimmerEffect() = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    
    graphicsLayer { this.alpha = alpha }
}
