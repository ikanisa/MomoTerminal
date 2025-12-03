package com.momoterminal.designsystem.interaction

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import com.momoterminal.designsystem.motion.MotionTokens

/**
 * Smooth visibility container with customizable enter/exit animations.
 */
@Composable
fun AnimatedVisibilityContainer(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enterDuration: Int = MotionTokens.STANDARD,
    exitDuration: Int = MotionTokens.QUICK,
    enterDelay: Int = 0,
    style: AnimationStyle = AnimationStyle.FadeScale,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val (enter, exit) = style.toSpecs(enterDuration, exitDuration, enterDelay)
    
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        content = content
    )
}

enum class AnimationStyle {
    Fade,
    FadeScale,
    FadeSlideUp,
    FadeSlideDown,
    FadeSlideLeft,
    FadeSlideRight,
    Expand,
    ExpandHorizontal
}

private fun AnimationStyle.toSpecs(
    enterDuration: Int,
    exitDuration: Int,
    enterDelay: Int
): Pair<EnterTransition, ExitTransition> {
    return when (this) {
        AnimationStyle.Fade -> 
            fadeIn(tween(enterDuration, enterDelay, MotionTokens.EaseOutExpo)) to 
            fadeOut(tween(exitDuration, easing = MotionTokens.EaseIn))
        
        AnimationStyle.FadeScale -> 
            fadeIn(tween(enterDuration, enterDelay, MotionTokens.EaseOutExpo)) + 
            scaleIn(initialScale = 0.92f, animationSpec = tween(enterDuration, enterDelay, MotionTokens.EaseOutExpo)) to
            fadeOut(tween(exitDuration, easing = MotionTokens.EaseIn)) + 
            scaleOut(targetScale = 0.92f, animationSpec = tween(exitDuration, easing = MotionTokens.EaseIn))
        
        AnimationStyle.FadeSlideUp ->
            fadeIn(tween(enterDuration, enterDelay, MotionTokens.EaseOutExpo)) + 
            slideInVertically(tween(enterDuration, enterDelay, MotionTokens.EaseOutExpo)) { it / 4 } to
            fadeOut(tween(exitDuration, easing = MotionTokens.EaseIn)) + 
            slideOutVertically(tween(exitDuration, easing = MotionTokens.EaseIn)) { -it / 4 }
        
        AnimationStyle.FadeSlideDown ->
            fadeIn(tween(enterDuration, enterDelay, MotionTokens.EaseOutExpo)) + 
            slideInVertically(tween(enterDuration, enterDelay, MotionTokens.EaseOutExpo)) { -it / 4 } to
            fadeOut(tween(exitDuration, easing = MotionTokens.EaseIn)) + 
            slideOutVertically(tween(exitDuration, easing = MotionTokens.EaseIn)) { it / 4 }
        
        AnimationStyle.FadeSlideLeft ->
            fadeIn(tween(enterDuration, enterDelay, MotionTokens.EaseOutExpo)) + 
            slideInHorizontally(tween(enterDuration, enterDelay, MotionTokens.EaseOutExpo)) { it / 4 } to
            fadeOut(tween(exitDuration, easing = MotionTokens.EaseIn)) + 
            slideOutHorizontally(tween(exitDuration, easing = MotionTokens.EaseIn)) { -it / 4 }
        
        AnimationStyle.FadeSlideRight ->
            fadeIn(tween(enterDuration, enterDelay, MotionTokens.EaseOutExpo)) + 
            slideInHorizontally(tween(enterDuration, enterDelay, MotionTokens.EaseOutExpo)) { -it / 4 } to
            fadeOut(tween(exitDuration, easing = MotionTokens.EaseIn)) + 
            slideOutHorizontally(tween(exitDuration, easing = MotionTokens.EaseIn)) { it / 4 }
        
        AnimationStyle.Expand ->
            fadeIn(tween(enterDuration, enterDelay, MotionTokens.EaseOutExpo)) + 
            expandVertically(tween(enterDuration, enterDelay)) to
            fadeOut(tween(exitDuration, easing = MotionTokens.EaseIn)) + 
            shrinkVertically(tween(exitDuration))
        
        AnimationStyle.ExpandHorizontal ->
            fadeIn(tween(enterDuration, enterDelay, MotionTokens.EaseOutExpo)) + 
            expandHorizontally(tween(enterDuration, enterDelay)) to
            fadeOut(tween(exitDuration, easing = MotionTokens.EaseIn)) + 
            shrinkHorizontally(tween(exitDuration))
    }
}

/**
 * Staggered animation for list items.
 */
@Composable
fun StaggeredAnimatedItem(
    visible: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
    staggerDelay: Int = 50,
    content: @Composable () -> Unit
) {
    AnimatedVisibilityContainer(
        visible = visible,
        modifier = modifier,
        enterDelay = index * staggerDelay,
        style = AnimationStyle.FadeSlideUp,
        content = { content() }
    )
}

/**
 * Animated content switcher with smooth crossfade.
 */
@Composable
fun <T> AnimatedContentSwitcher(
    targetState: T,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOut)) togetherWith
            fadeOut(tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn))
        },
        contentAlignment = contentAlignment,
        label = "contentSwitcher"
    ) { state ->
        content(state)
    }
}

/**
 * Pulsing animation for attention-grabbing elements.
 */
@Composable
fun PulsingContainer(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (enabled) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = MotionTokens.EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Box(modifier = modifier.scale(scale)) {
        content()
    }
}

/**
 * Shimmer loading effect.
 */
@Composable
fun ShimmerContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    
    Box(modifier = modifier.alpha(alpha)) {
        content()
    }
}
