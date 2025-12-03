package com.momoterminal.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

/**
 * Modern animation specifications for fluid, premium feel.
 * Based on Material Motion guidelines with custom refinements.
 */
object MomoAnimation {
    // Duration constants
    const val DURATION_INSTANT = 100
    const val DURATION_FAST = 200
    const val DURATION_MEDIUM = 300
    const val DURATION_SLOW = 450
    const val DURATION_EMPHASIS = 500
    
    // Custom easing curves for premium feel
    val EaseOutExpo: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val EaseInOutCubic: Easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
    val EaseOutBack: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    val EaseOutQuint: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    
    // Spring specs for bouncy, natural animations
    val springBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val springSnappy = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    val springGentle = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    // Tween specs
    fun tweenFast() = tween<Float>(DURATION_FAST, easing = EaseOutExpo)
    fun tweenMedium() = tween<Float>(DURATION_MEDIUM, easing = EaseOutExpo)
    fun tweenSlow() = tween<Float>(DURATION_SLOW, easing = EaseInOutCubic)
    
    // Screen transitions
    val screenEnter = fadeIn(tween(DURATION_MEDIUM)) + slideInHorizontally(
        initialOffsetX = { it / 4 },
        animationSpec = tween(DURATION_MEDIUM, easing = EaseOutExpo)
    )
    
    val screenExit = fadeOut(tween(DURATION_FAST)) + slideOutHorizontally(
        targetOffsetX = { -it / 4 },
        animationSpec = tween(DURATION_FAST, easing = FastOutSlowInEasing)
    )
    
    val screenPopEnter = fadeIn(tween(DURATION_MEDIUM)) + slideInHorizontally(
        initialOffsetX = { -it / 4 },
        animationSpec = tween(DURATION_MEDIUM, easing = EaseOutExpo)
    )
    
    val screenPopExit = fadeOut(tween(DURATION_FAST)) + slideOutHorizontally(
        targetOffsetX = { it / 4 },
        animationSpec = tween(DURATION_FAST, easing = FastOutSlowInEasing)
    )
    
    // Bottom sheet / Modal transitions
    val bottomSheetEnter = fadeIn(tween(DURATION_MEDIUM)) + slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(DURATION_MEDIUM, easing = EaseOutExpo)
    )
    
    val bottomSheetExit = fadeOut(tween(DURATION_FAST)) + slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(DURATION_FAST, easing = FastOutSlowInEasing)
    )
    
    // Scale animations for buttons/cards
    val scaleEnter = scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(DURATION_MEDIUM, easing = EaseOutBack)
    ) + fadeIn(tween(DURATION_FAST))
    
    val scaleExit = scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(DURATION_FAST)
    ) + fadeOut(tween(DURATION_FAST))
}
