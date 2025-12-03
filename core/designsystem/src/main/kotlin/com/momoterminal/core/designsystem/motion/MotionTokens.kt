package com.momoterminal.core.designsystem.motion

import androidx.compose.animation.core.*
import androidx.compose.animation.*

/**
 * Motion Design System for MomoTerminal
 * 
 * PRINCIPLES:
 * - Financial confirmations: Slightly slower (450-500ms), reassuring easing
 * - Quick utilities: Fast (150-250ms), snappy response
 * - NFC interactions: Immediate feedback with sustained visual confirmation
 * 
 * All animations designed to feel "money-safe" - solid, trustworthy, responsive.
 */
object MotionTokens {
    
    // ═══════════════════════════════════════════════════════════════════
    // DURATION TOKENS
    // ═══════════════════════════════════════════════════════════════════
    
    /** Instant feedback - haptics, micro-interactions */
    const val INSTANT = 100
    
    /** Quick utilities - list updates, toggles, NFC scan feedback */
    const val QUICK = 200
    
    /** Standard transitions - screen changes, card interactions */
    const val STANDARD = 300
    
    /** Financial confirmations - balance updates, payment success */
    const val FINANCIAL = 450
    
    /** Emphasis - important state changes, errors */
    const val EMPHASIS = 500
    
    // ═══════════════════════════════════════════════════════════════════
    // EASING CURVES
    // ═══════════════════════════════════════════════════════════════════
    
    /** Smooth deceleration - for elements entering view */
    val EaseOut: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    
    /** Smooth acceleration - for elements leaving view */
    val EaseIn: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    
    /** Balanced - for elements moving within view */
    val EaseInOut: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    /** Premium feel - exponential deceleration for polished interactions */
    val EaseOutExpo: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    
    /** Trustworthy - slower, more deliberate for financial operations */
    val EaseFinancial: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    
    /** Bouncy - subtle overshoot for success states */
    val EaseOutBack: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    
    // ═══════════════════════════════════════════════════════════════════
    // SPRING SPECS
    // ═══════════════════════════════════════════════════════════════════
    
    /** Snappy spring - for quick interactions */
    val SpringSnappy = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    /** Responsive spring - for card presses */
    val SpringResponsive = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    
    /** Gentle spring - for balance animations */
    val SpringGentle = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
}

/**
 * Screen transition specs for navigation
 */
object ScreenTransitions {
    
    // Wallet <-> NFC transitions (horizontal slide)
    val enterFromRight = fadeIn(tween(MotionTokens.STANDARD)) + slideInHorizontally(
        initialOffsetX = { it / 3 },
        animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOutExpo)
    )
    
    val exitToLeft = fadeOut(tween(MotionTokens.QUICK)) + slideOutHorizontally(
        targetOffsetX = { -it / 3 },
        animationSpec = tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)
    )
    
    val enterFromLeft = fadeIn(tween(MotionTokens.STANDARD)) + slideInHorizontally(
        initialOffsetX = { -it / 3 },
        animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOutExpo)
    )
    
    val exitToRight = fadeOut(tween(MotionTokens.QUICK)) + slideOutHorizontally(
        targetOffsetX = { it / 3 },
        animationSpec = tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)
    )
    
    // Bottom sheet / modal (vertical slide)
    val bottomSheetEnter = fadeIn(tween(MotionTokens.STANDARD)) + slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOutExpo)
    )
    
    val bottomSheetExit = fadeOut(tween(MotionTokens.QUICK)) + slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)
    )
    
    // Financial confirmation screens (slower, more deliberate)
    val financialEnter = fadeIn(tween(MotionTokens.FINANCIAL, easing = MotionTokens.EaseFinancial)) +
        scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(MotionTokens.FINANCIAL, easing = MotionTokens.EaseFinancial)
        )
    
    val financialExit = fadeOut(tween(MotionTokens.STANDARD))
}

/**
 * Tween animation specs for common use cases
 */
object TweenSpecs {
    fun <T> quick() = tween<T>(MotionTokens.QUICK, easing = MotionTokens.EaseOut)
    fun <T> standard() = tween<T>(MotionTokens.STANDARD, easing = MotionTokens.EaseOutExpo)
    fun <T> financial() = tween<T>(MotionTokens.FINANCIAL, easing = MotionTokens.EaseFinancial)
    fun <T> emphasis() = tween<T>(MotionTokens.EMPHASIS, easing = MotionTokens.EaseOutBack)
}
