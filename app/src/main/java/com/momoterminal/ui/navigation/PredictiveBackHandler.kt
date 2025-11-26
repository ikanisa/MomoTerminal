package com.momoterminal.ui.navigation

import android.os.Build
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * State class to track predictive back gesture progress.
 */
class PredictiveBackState {
    var progress by mutableFloatStateOf(0f)
        internal set
    var isGestureActive by mutableStateOf(false)
        internal set
    var swipeEdge by mutableStateOf(SwipeEdge.LEFT)
        internal set

    enum class SwipeEdge {
        LEFT,
        RIGHT
    }
}

/**
 * Remember a PredictiveBackState instance.
 */
@Composable
fun rememberPredictiveBackState(): PredictiveBackState {
    return remember { PredictiveBackState() }
}

/**
 * Modifier that applies predictive back animation to content.
 * Applies scale, translation, and alpha based on gesture progress.
 *
 * @param state The predictive back state to use
 * @param maxScale The maximum scale reduction (0.1 = 10% smaller at full progress)
 * @param maxTranslationX The maximum horizontal translation
 * @param minAlpha The minimum alpha at full progress
 */
fun Modifier.predictiveBackAnimation(
    state: PredictiveBackState,
    maxScale: Float = 0.1f,
    maxTranslationX: Float = 100f,
    minAlpha: Float = 0.8f
): Modifier {
    val scale = 1f - (state.progress * maxScale)
    val translationX = when (state.swipeEdge) {
        PredictiveBackState.SwipeEdge.LEFT -> state.progress * maxTranslationX
        PredictiveBackState.SwipeEdge.RIGHT -> -state.progress * maxTranslationX
    }
    val alpha = 1f - (state.progress * (1f - minAlpha))

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.translationX = translationX
        this.alpha = alpha
    }
}

/**
 * Composable that wraps content with predictive back gesture support.
 * Provides smooth spring animations for gesture response.
 *
 * @param onBack Callback when back is invoked
 * @param enabled Whether the back handler is enabled
 * @param content The content to wrap
 */
@Composable
fun PredictiveBackScreen(
    onBack: () -> Unit,
    enabled: Boolean = true,
    content: @Composable (PredictiveBackState) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val state = rememberPredictiveBackState()

    // Animated values for smooth transitions
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(state.progress) {
        animatedProgress.animateTo(
            targetValue = state.progress,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // Update state with animated progress
    LaunchedEffect(animatedProgress.value) {
        state.progress = animatedProgress.value
    }

    DisposableEffect(enabled, backDispatcher, activity) {
        if (!enabled) {
            return@DisposableEffect onDispose { }
        }

        // For Android 14+ use OnBackAnimationCallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && activity != null) {
            val callback = object : OnBackAnimationCallback {
                override fun onBackStarted(backEvent: BackEvent) {
                    state.isGestureActive = true
                    state.swipeEdge = if (backEvent.swipeEdge == BackEvent.EDGE_LEFT) {
                        PredictiveBackState.SwipeEdge.LEFT
                    } else {
                        PredictiveBackState.SwipeEdge.RIGHT
                    }
                }

                override fun onBackProgressed(backEvent: BackEvent) {
                    state.progress = backEvent.progress
                }

                override fun onBackInvoked() {
                    state.isGestureActive = false
                    state.progress = 0f
                    onBack()
                }

                override fun onBackCancelled() {
                    state.isGestureActive = false
                    state.progress = 0f
                }
            }

            activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                callback
            )

            onDispose {
                activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
            }
        } else if (backDispatcher != null) {
            // Fallback for older Android versions
            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBack()
                }
            }

            backDispatcher.addCallback(callback)

            onDispose {
                callback.remove()
            }
        } else {
            onDispose { }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .predictiveBackAnimation(state)
    ) {
        content(state)
    }
}

/**
 * Simple back handler for cases where predictive animation is not needed.
 *
 * @param enabled Whether the handler is enabled
 * @param onBack Callback when back is pressed
 */
@Composable
fun SimpleBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    DisposableEffect(enabled, backDispatcher) {
        if (!enabled || backDispatcher == null) {
            return@DisposableEffect onDispose { }
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBack()
            }
        }

        backDispatcher.addCallback(callback)

        onDispose {
            callback.remove()
        }
    }
}
