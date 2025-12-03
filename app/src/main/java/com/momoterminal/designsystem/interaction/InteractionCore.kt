package com.momoterminal.designsystem.interaction

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import com.momoterminal.designsystem.motion.MomoHaptic
import com.momoterminal.designsystem.motion.performMomoHaptic
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * MOTION PRINCIPLES
 * =================
 * 1. NATURAL PHYSICS: Use springs for interactive gestures, tweens for state changes
 * 2. RESPONSIVE: Immediate visual feedback (<100ms), completion within 300ms
 * 3. CONTEXTUAL: Financial operations slower (450ms), utilities faster (200ms)
 * 4. HAPTIC SYNC: Vibration aligned with visual keyframes
 * 
 * WHEN TO USE:
 * - Spring: Drag, fling, interactive gestures (feels physical)
 * - Tween: State transitions, enter/exit (predictable timing)
 * - Keyframes: Complex multi-stage animations (loading, success)
 */
object InteractionDefaults {
    // Press feedback
    const val PRESS_SCALE = 0.97f
    const val PRESS_ALPHA = 0.9f
    
    // Swipe thresholds
    const val SWIPE_THRESHOLD_DP = 56f
    const val VELOCITY_THRESHOLD = 500f
    
    // Snap positions for bottom sheet (fraction of height)
    val SNAP_POSITIONS = listOf(0f, 0.5f, 1f) // collapsed, half, expanded
    
    // Spring configs
    val DragSpring = SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val SnapSpring = SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    
    val BounceSpring = SpringSpec<Float>(
        dampingRatio = 0.5f,
        stiffness = Spring.StiffnessLow
    )
}

/**
 * Pressable state for interactive elements.
 */
@Stable
class PressableState {
    var isPressed by mutableStateOf(false)
        internal set
    
    val scale: Float get() = if (isPressed) InteractionDefaults.PRESS_SCALE else 1f
    val alpha: Float get() = if (isPressed) InteractionDefaults.PRESS_ALPHA else 1f
}

@Composable
fun rememberPressableState() = remember { PressableState() }

/**
 * Modifier for press interactions with haptic feedback.
 */
fun Modifier.pressable(
    state: PressableState,
    enabled: Boolean = true,
    haptic: MomoHaptic = MomoHaptic.Tap,
    onClick: () -> Unit
) = composed {
    val view = LocalView.current
    
    pointerInput(enabled) {
        if (!enabled) return@pointerInput
        detectTapGestures(
            onPress = {
                state.isPressed = true
                view.performMomoHaptic(haptic)
                tryAwaitRelease()
                state.isPressed = false
            },
            onTap = { onClick() }
        )
    }
}

/**
 * Draggable state with velocity tracking and snap behavior.
 */
@Stable
class DraggableState(
    private val snapPositions: List<Float>,
    initialPosition: Float = 0f
) {
    var offset by mutableFloatStateOf(initialPosition)
        internal set
    
    var isDragging by mutableStateOf(false)
        internal set
    
    private val velocityTracker = VelocityTracker()
    
    fun snapToNearest(velocity: Float = 0f): Float {
        val projected = offset + velocity * 0.1f // Project based on velocity
        return snapPositions.minByOrNull { (it - projected).absoluteValue } ?: offset
    }
    
    fun trackVelocity(change: Offset) {
        velocityTracker.addPosition(System.currentTimeMillis(), change)
    }
    
    fun getVelocity(): Float = velocityTracker.calculateVelocity().y
    
    fun reset() {
        velocityTracker.resetTracking()
    }
}

@Composable
fun rememberDraggableState(
    snapPositions: List<Float> = InteractionDefaults.SNAP_POSITIONS,
    initialPosition: Float = 0f
) = remember { DraggableState(snapPositions, initialPosition) }

/**
 * Swipe-to-reveal state for list items.
 */
@Stable
class SwipeRevealState {
    var offsetX by mutableFloatStateOf(0f)
        internal set
    
    var isRevealed by mutableStateOf(false)
        internal set
    
    val progress: Float get() = (offsetX.absoluteValue / 200f).coerceIn(0f, 1f)
}

@Composable
fun rememberSwipeRevealState() = remember { SwipeRevealState() }

/**
 * Modifier for swipe-to-reveal with magnetic snap.
 */
fun Modifier.swipeToReveal(
    state: SwipeRevealState,
    revealThreshold: Float = InteractionDefaults.SWIPE_THRESHOLD_DP,
    onReveal: () -> Unit = {},
    onHide: () -> Unit = {}
) = composed {
    val scope = rememberCoroutineScope()
    val animatedOffset = remember { Animatable(0f) }
    
    LaunchedEffect(state.offsetX) {
        animatedOffset.snapTo(state.offsetX)
    }
    
    pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart = { },
            onDragEnd = {
                scope.launch {
                    val target = if (state.offsetX.absoluteValue > revealThreshold * density) {
                        if (state.offsetX < 0) -200f else 200f
                    } else 0f
                    
                    animatedOffset.animateTo(
                        target,
                        animationSpec = InteractionDefaults.SnapSpring
                    )
                    state.offsetX = target
                    
                    val wasRevealed = state.isRevealed
                    state.isRevealed = target != 0f
                    
                    if (state.isRevealed && !wasRevealed) onReveal()
                    if (!state.isRevealed && wasRevealed) onHide()
                }
            },
            onHorizontalDrag = { _, dragAmount ->
                // Resistance at edges
                val resistance = if (state.offsetX.absoluteValue > 200f) 0.3f else 1f
                state.offsetX = (state.offsetX + dragAmount * resistance).coerceIn(-250f, 250f)
            }
        )
    }
}

/**
 * Animated offset for swipe gestures.
 */
@Composable
fun animatedSwipeOffset(state: SwipeRevealState): IntOffset {
    val animatedX by animateFloatAsState(
        targetValue = state.offsetX,
        animationSpec = InteractionDefaults.SnapSpring,
        label = "swipeOffset"
    )
    return IntOffset(animatedX.roundToInt(), 0)
}
