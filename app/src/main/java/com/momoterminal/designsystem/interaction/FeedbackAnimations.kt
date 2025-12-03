package com.momoterminal.designsystem.interaction

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import com.momoterminal.designsystem.motion.MomoHaptic
import com.momoterminal.designsystem.motion.MotionTokens
import com.momoterminal.designsystem.motion.performMomoHaptic
import kotlinx.coroutines.delay

/**
 * Success animation with bounce and haptic.
 */
@Composable
fun SuccessAnimation(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    var animationState by remember { mutableStateOf(AnimState.Idle) }
    
    val scale by animateFloatAsState(
        targetValue = when (animationState) {
            AnimState.Idle -> if (trigger) 0f else 1f
            AnimState.Growing -> 1.2f
            AnimState.Bouncing -> 1f
            AnimState.Complete -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "successScale"
    )
    
    LaunchedEffect(trigger) {
        if (trigger) {
            animationState = AnimState.Growing
            delay(150)
            view.performMomoHaptic(MomoHaptic.PaymentSuccess)
            animationState = AnimState.Bouncing
            delay(300)
            animationState = AnimState.Complete
            onComplete()
        }
    }
    
    Box(modifier = modifier.scale(scale)) {
        content()
    }
}

/**
 * Error shake animation with haptic.
 */
@Composable
fun ErrorShakeAnimation(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    var isShaking by remember { mutableStateOf(false) }
    
    val shakeOffset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = if (isShaking) {
            keyframes {
                durationMillis = 400
                0f at 0
                -10f at 50
                10f at 100
                -10f at 150
                10f at 200
                -5f at 250
                5f at 300
                0f at 400
            }
        } else {
            tween(0)
        },
        label = "shakeOffset"
    )
    
    LaunchedEffect(trigger) {
        if (trigger) {
            isShaking = true
            view.performMomoHaptic(MomoHaptic.PaymentError)
            delay(400)
            isShaking = false
            onComplete()
        }
    }
    
    Box(
        modifier = modifier.graphicsLayer {
            translationX = if (isShaking) shakeOffset else 0f
        }
    ) {
        content()
    }
}

/**
 * Loading spinner with rotation.
 */
@Composable
fun LoadingRotation(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadingRotation"
    )
    
    Box(modifier = modifier.rotate(if (isLoading) rotation else 0f)) {
        content()
    }
}

/**
 * Pulse animation for attention.
 */
@Composable
fun PulseAnimation(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (enabled) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = MotionTokens.EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (enabled) 0.7f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = MotionTokens.EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        content()
    }
}

/**
 * Counting animation for numbers.
 */
@Composable
fun animatedCount(
    targetValue: Int,
    durationMillis: Int = MotionTokens.FINANCIAL
): Int {
    var currentValue by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(targetValue) {
        val startValue = currentValue
        val diff = targetValue - startValue
        val steps = 30
        val stepDuration = durationMillis / steps
        
        repeat(steps) { i ->
            delay(stepDuration.toLong())
            currentValue = startValue + (diff * (i + 1) / steps)
        }
        currentValue = targetValue
    }
    
    return currentValue
}

/**
 * Animated float value with formatting.
 */
@Composable
fun animatedFloat(
    targetValue: Float,
    durationMillis: Int = MotionTokens.FINANCIAL
): Float {
    val animatedValue by animateFloatAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis, easing = MotionTokens.EaseOut),
        label = "animatedFloat"
    )
    return animatedValue
}

private enum class AnimState { Idle, Growing, Bouncing, Complete }
