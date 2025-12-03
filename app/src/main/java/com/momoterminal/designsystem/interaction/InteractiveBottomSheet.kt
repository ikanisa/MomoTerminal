package com.momoterminal.designsystem.interaction

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.motion.MomoHaptic
import com.momoterminal.designsystem.motion.MotionTokens
import com.momoterminal.designsystem.motion.performMomoHaptic
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Bottom sheet snap positions.
 */
enum class SheetPosition { Hidden, Peek, Half, Expanded }

/**
 * State for interactive bottom sheet.
 */
@Stable
class InteractiveSheetState(
    initialPosition: SheetPosition = SheetPosition.Hidden
) {
    var currentPosition by mutableStateOf(initialPosition)
        internal set
    
    internal var sheetHeight by mutableIntStateOf(0)
    internal var offsetY = Animatable(0f)
    
    val isVisible: Boolean get() = currentPosition != SheetPosition.Hidden
    val progress: Float get() = 1f - (offsetY.value / sheetHeight.coerceAtLeast(1)).coerceIn(0f, 1f)
    
    suspend fun show(position: SheetPosition = SheetPosition.Half) {
        currentPosition = position
        animateToPosition(position)
    }
    
    suspend fun hide() {
        currentPosition = SheetPosition.Hidden
        animateToPosition(SheetPosition.Hidden)
    }
    
    suspend fun expand() = show(SheetPosition.Expanded)
    suspend fun collapse() = show(SheetPosition.Peek)
    
    internal suspend fun animateToPosition(position: SheetPosition) {
        val target = when (position) {
            SheetPosition.Hidden -> sheetHeight.toFloat()
            SheetPosition.Peek -> sheetHeight * 0.85f
            SheetPosition.Half -> sheetHeight * 0.5f
            SheetPosition.Expanded -> 0f
        }
        offsetY.animateTo(
            target,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    
    internal fun snapToNearest(velocity: Float): SheetPosition {
        val currentOffset = offsetY.value
        val projected = currentOffset + velocity * 0.15f
        
        val positions = listOf(
            SheetPosition.Expanded to 0f,
            SheetPosition.Half to sheetHeight * 0.5f,
            SheetPosition.Peek to sheetHeight * 0.85f,
            SheetPosition.Hidden to sheetHeight.toFloat()
        )
        
        // Velocity-based snap
        if (velocity.absoluteValue > 1000f) {
            return if (velocity > 0) SheetPosition.Hidden else SheetPosition.Expanded
        }
        
        return positions.minByOrNull { (it.second - projected).absoluteValue }?.first 
            ?: SheetPosition.Hidden
    }
}

@Composable
fun rememberInteractiveSheetState(
    initialPosition: SheetPosition = SheetPosition.Hidden
) = remember { InteractiveSheetState(initialPosition) }

/**
 * Interactive bottom sheet with smooth drag, snap, and visual feedback.
 */
@Composable
fun InteractiveBottomSheet(
    state: InteractiveSheetState,
    modifier: Modifier = Modifier,
    scrimColor: Color = Color.Black.copy(alpha = 0.5f),
    onDismiss: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current
    
    // Scrim
    if (state.isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(state.progress * 0.5f)
                .background(scrimColor)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, _ -> }
                    // Tap to dismiss
                }
        )
    }
    
    // Sheet
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val sheetScale by animateFloatAsState(
            targetValue = if (state.progress > 0.8f) 1f else 0.98f + state.progress * 0.02f,
            animationSpec = tween(100),
            label = "sheetScale"
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .onSizeChanged { state.sheetHeight = it.height }
                .offset { IntOffset(0, state.offsetY.value.roundToInt()) }
                .scale(sheetScale)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    var velocityY = 0f
                    detectVerticalDragGestures(
                        onDragStart = {
                            view.performMomoHaptic(MomoHaptic.Tap)
                        },
                        onDragEnd = {
                            scope.launch {
                                val target = state.snapToNearest(velocityY)
                                state.currentPosition = target
                                state.animateToPosition(target)
                                
                                if (target == SheetPosition.Hidden) {
                                    onDismiss()
                                }
                                
                                // Haptic on snap
                                view.performMomoHaptic(MomoHaptic.Tap)
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            velocityY = dragAmount * 10
                            
                            scope.launch {
                                // Resistance at top
                                val resistance = if (state.offsetY.value < 0) 0.3f else 1f
                                val newOffset = (state.offsetY.value + dragAmount * resistance)
                                    .coerceIn(-50f, state.sheetHeight.toFloat())
                                state.offsetY.snapTo(newOffset)
                            }
                        }
                    )
                }
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
            
            content()
        }
    }
    
    // Initialize position
    LaunchedEffect(state.currentPosition) {
        if (state.sheetHeight > 0) {
            state.animateToPosition(state.currentPosition)
        }
    }
}
