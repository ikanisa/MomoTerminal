package com.momoterminal.designsystem.interaction

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import com.momoterminal.designsystem.motion.MotionTokens

/**
 * Screen transition presets for navigation.
 */
object NavTransitions {
    
    /**
     * Horizontal slide - standard forward/back navigation.
     */
    val slideHorizontal: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ContentTransform = {
        val enter = fadeIn(tween(MotionTokens.STANDARD)) + slideInHorizontally(
            initialOffsetX = { it / 4 },
            animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOutExpo)
        )
        val exit = fadeOut(tween(MotionTokens.QUICK)) + slideOutHorizontally(
            targetOffsetX = { -it / 4 },
            animationSpec = tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)
        )
        enter togetherWith exit
    }
    
    /**
     * Vertical slide - modal/sheet presentation.
     */
    val slideVertical: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ContentTransform = {
        val enter = fadeIn(tween(MotionTokens.STANDARD)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOutExpo)
        )
        val exit = fadeOut(tween(MotionTokens.QUICK)) + slideOutVertically(
            targetOffsetY = { it / 3 },
            animationSpec = tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)
        )
        enter togetherWith exit
    }
    
    /**
     * Scale fade - for detail screens.
     */
    val scaleFade: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ContentTransform = {
        val enter = fadeIn(tween(MotionTokens.STANDARD)) + scaleIn(
            initialScale = 0.92f,
            animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOutExpo)
        )
        val exit = fadeOut(tween(MotionTokens.QUICK)) + scaleOut(
            targetScale = 1.05f,
            animationSpec = tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)
        )
        enter togetherWith exit
    }
    
    /**
     * Shared axis - Material 3 style.
     */
    val sharedAxisX: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ContentTransform = {
        val enter = fadeIn(tween(MotionTokens.STANDARD, delayMillis = 90)) + slideInHorizontally(
            initialOffsetX = { 30 },
            animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOut)
        )
        val exit = fadeOut(tween(90)) + slideOutHorizontally(
            targetOffsetX = { -30 },
            animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseIn)
        )
        enter togetherWith exit
    }
}

/**
 * Animated screen wrapper for consistent enter/exit animations.
 */
@Composable
fun AnimatedScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibilityContainer(
        visible = true,
        modifier = modifier,
        style = AnimationStyle.FadeScale
    ) {
        content()
    }
}

/**
 * Transition specs for AnimatedNavHost.
 */
fun enterTransition(): EnterTransition = fadeIn(
    tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOut)
) + slideInHorizontally(
    initialOffsetX = { it / 4 },
    animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOutExpo)
)

fun exitTransition(): ExitTransition = fadeOut(
    tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)
) + slideOutHorizontally(
    targetOffsetX = { -it / 4 },
    animationSpec = tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)
)

fun popEnterTransition(): EnterTransition = fadeIn(
    tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOut)
) + slideInHorizontally(
    initialOffsetX = { -it / 4 },
    animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOutExpo)
)

fun popExitTransition(): ExitTransition = fadeOut(
    tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)
) + slideOutHorizontally(
    targetOffsetX = { it / 4 },
    animationSpec = tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)
)
