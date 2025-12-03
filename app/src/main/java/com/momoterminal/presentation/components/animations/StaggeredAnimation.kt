package com.momoterminal.presentation.components.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.momoterminal.presentation.theme.MomoAnimation

/**
 * Wraps content with staggered fade + slide animation.
 * Use index to create cascading entry effect in lists.
 */
@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    delayPerItem: Int = 50,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(24f) }
    
    LaunchedEffect(Unit) {
        val delay = index * delayPerItem
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = MomoAnimation.DURATION_MEDIUM,
                delayMillis = delay,
                easing = MomoAnimation.EaseOutExpo
            )
        )
    }
    
    LaunchedEffect(Unit) {
        val delay = index * delayPerItem
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = MomoAnimation.DURATION_MEDIUM,
                delayMillis = delay,
                easing = MomoAnimation.EaseOutExpo
            )
        )
    }
    
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value
        }
    ) {
        content()
    }
}

/**
 * Scale + fade animation for cards and buttons.
 */
@Composable
fun ScaleInAnimatedItem(
    index: Int = 0,
    modifier: Modifier = Modifier,
    delayPerItem: Int = 50,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.92f) }
    
    LaunchedEffect(Unit) {
        val delay = index * delayPerItem
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = MomoAnimation.DURATION_FAST,
                delayMillis = delay
            )
        )
    }
    
    LaunchedEffect(Unit) {
        val delay = index * delayPerItem
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = MomoAnimation.DURATION_MEDIUM,
                delayMillis = delay,
                easing = MomoAnimation.EaseOutBack
            )
        )
    }
    
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            scaleX = scale.value
            scaleY = scale.value
        }
    ) {
        content()
    }
}
