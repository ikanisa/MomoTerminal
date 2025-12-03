package com.momoterminal.designsystem.interaction

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import com.momoterminal.designsystem.motion.MomoHaptic
import com.momoterminal.designsystem.motion.performMomoHaptic
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Long-press with haptic feedback and progress callback.
 */
fun Modifier.longPressable(
    onLongPress: () -> Unit,
    onProgress: ((Float) -> Unit)? = null,
    duration: Long = 500L,
    haptic: MomoHaptic = MomoHaptic.ButtonPress
) = composed {
    val view = LocalView.current
    var progress by remember { mutableFloatStateOf(0f) }
    
    pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                coroutineScope {
                    val job = launch {
                        val steps = 20
                        val stepDuration = duration / steps
                        repeat(steps) { i ->
                            delay(stepDuration)
                            progress = (i + 1) / steps.toFloat()
                            onProgress?.invoke(progress)
                        }
                        view.performMomoHaptic(haptic)
                        onLongPress()
                    }
                    tryAwaitRelease()
                    job.cancel()
                    progress = 0f
                    onProgress?.invoke(0f)
                }
            }
        )
    }
}

/**
 * Double-tap gesture with haptic.
 */
fun Modifier.doubleTappable(
    onDoubleTap: () -> Unit,
    haptic: MomoHaptic = MomoHaptic.Tap
) = composed {
    val view = LocalView.current
    
    pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = {
                view.performMomoHaptic(haptic)
                onDoubleTap()
            }
        )
    }
}

/**
 * Drag gesture state with bounds and resistance.
 */
@Stable
class DragState(
    private val bounds: ClosedFloatingPointRange<Float> = -Float.MAX_VALUE..Float.MAX_VALUE,
    private val resistance: Float = 0.3f
) {
    var offset by mutableStateOf(Offset.Zero)
        internal set
    
    var isDragging by mutableStateOf(false)
        internal set
    
    fun applyDrag(delta: Offset): Offset {
        val newX = (offset.x + delta.x).coerceIn(bounds)
        val newY = (offset.y + delta.y).coerceIn(bounds)
        
        // Apply resistance at bounds
        val resistedX = if (newX == bounds.start || newX == bounds.endInclusive) {
            offset.x + delta.x * resistance
        } else newX
        
        val resistedY = if (newY == bounds.start || newY == bounds.endInclusive) {
            offset.y + delta.y * resistance
        } else newY
        
        offset = Offset(resistedX, resistedY)
        return offset
    }
    
    fun reset() {
        offset = Offset.Zero
    }
}

@Composable
fun rememberDragState(
    bounds: ClosedFloatingPointRange<Float> = -Float.MAX_VALUE..Float.MAX_VALUE
) = remember { DragState(bounds) }

/**
 * 2D draggable modifier with spring-back.
 */
fun Modifier.draggable2D(
    state: DragState,
    onDragEnd: ((Offset) -> Unit)? = null
) = composed {
    val view = LocalView.current
    
    pointerInput(Unit) {
        detectDragGestures(
            onDragStart = {
                state.isDragging = true
                view.performMomoHaptic(MomoHaptic.Tap)
            },
            onDragEnd = {
                state.isDragging = false
                onDragEnd?.invoke(state.offset)
            },
            onDrag = { change, dragAmount ->
                change.consume()
                state.applyDrag(dragAmount)
            }
        )
    }
}

/**
 * Animated spring-back for drag state.
 */
@Composable
fun animatedDragOffset(state: DragState): Offset {
    val animatedX by animateFloatAsState(
        targetValue = if (state.isDragging) state.offset.x else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dragX"
    )
    val animatedY by animateFloatAsState(
        targetValue = if (state.isDragging) state.offset.y else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dragY"
    )
    return Offset(animatedX, animatedY)
}
