package com.momoterminal.designsystem.interaction

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.motion.MomoHaptic
import com.momoterminal.designsystem.motion.MotionTokens
import com.momoterminal.designsystem.motion.performMomoHaptic
import kotlin.math.absoluteValue

/**
 * Enhanced LazyColumn with bouncy overscroll and item animations.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BouncyLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        flingBehavior = flingBehavior,
        content = content
    )
}

/**
 * Animated list item with entrance animation and press feedback.
 */
@Composable
fun AnimatedListItem(
    modifier: Modifier = Modifier,
    index: Int = 0,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) { isVisible = true }
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> InteractionDefaults.PRESS_SCALE
            !isVisible -> 0.95f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "itemScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = MotionTokens.STANDARD,
            delayMillis = index * 30,
            easing = MotionTokens.EaseOut
        ),
        label = "itemAlpha"
    )
    
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 20f,
        animationSpec = tween(
            durationMillis = MotionTokens.STANDARD,
            delayMillis = index * 30,
            easing = MotionTokens.EaseOutExpo
        ),
        label = "itemOffset"
    )
    
    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = offsetY
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.pressable(
                        state = rememberPressableState().also { isPressed = it.isPressed },
                        onClick = {
                            view.performMomoHaptic(MomoHaptic.Tap)
                            onClick()
                        }
                    )
                } else Modifier
            )
    ) {
        content()
    }
}

/**
 * Swipeable list item with reveal actions.
 */
@Composable
fun SwipeableListItem(
    modifier: Modifier = Modifier,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    leftContent: @Composable (BoxScope.() -> Unit)? = null,
    rightContent: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeRevealState()
    val view = LocalView.current
    val offset = animatedSwipeOffset(state)
    
    // Haptic feedback at threshold
    LaunchedEffect(state.progress) {
        if (state.progress > 0.5f && state.progress < 0.55f) {
            view.performMomoHaptic(MomoHaptic.Tap)
        }
    }
    
    Box(modifier = modifier) {
        // Background actions
        Row(Modifier.matchParentSize()) {
            // Left action (revealed on swipe right)
            if (leftContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f)
                        .graphicsLayer {
                            alpha = if (state.offsetX > 0) state.progress else 0f
                            scaleX = 0.8f + state.progress * 0.2f
                            scaleY = 0.8f + state.progress * 0.2f
                        },
                    content = leftContent
                )
            }
            
            Spacer(Modifier.weight(1f))
            
            // Right action (revealed on swipe left)
            if (rightContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f)
                        .graphicsLayer {
                            alpha = if (state.offsetX < 0) state.progress else 0f
                            scaleX = 0.8f + state.progress * 0.2f
                            scaleY = 0.8f + state.progress * 0.2f
                        },
                    content = rightContent
                )
            }
        }
        
        // Main content
        Box(
            modifier = Modifier
                .offset { offset }
                .swipeToReveal(
                    state = state,
                    onReveal = {
                        if (state.offsetX < 0) onSwipeLeft?.invoke()
                        else onSwipeRight?.invoke()
                    }
                )
        ) {
            content()
        }
    }
}

/**
 * Pull-to-refresh indicator state.
 */
@Stable
class PullRefreshState {
    var isRefreshing by mutableStateOf(false)
    var pullProgress by mutableFloatStateOf(0f)
        internal set
}

@Composable
fun rememberPullRefreshState() = remember { PullRefreshState() }
