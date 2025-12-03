package com.momoterminal.designsystem.interaction

import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import kotlin.math.absoluteValue

/**
 * Scroll-aware header that collapses/fades on scroll.
 */
@Composable
fun collapsingHeaderProgress(
    listState: LazyListState,
    headerHeight: Int
): Float {
    val scrollOffset = remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat() / headerHeight.coerceAtLeast(1)
            } else 1f
        }
    }
    return scrollOffset.value.coerceIn(0f, 1f)
}

/**
 * Modifier for parallax scroll effect.
 */
fun Modifier.parallaxScroll(
    scrollState: LazyListState,
    rate: Float = 0.5f
): Modifier {
    val offset = if (scrollState.firstVisibleItemIndex == 0) {
        scrollState.firstVisibleItemScrollOffset * rate
    } else 0f
    
    return graphicsLayer {
        translationY = -offset
    }
}

/**
 * Fade-on-scroll modifier.
 */
@Composable
fun Modifier.fadeOnScroll(
    listState: LazyListState,
    threshold: Int = 100
): Modifier {
    val alpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                1f - (listState.firstVisibleItemScrollOffset.toFloat() / threshold).coerceIn(0f, 1f)
            } else 0f
        }
    }
    return this.alpha(alpha)
}

/**
 * Scale-on-scroll modifier for hero elements.
 */
@Composable
fun Modifier.scaleOnScroll(
    listState: LazyListState,
    minScale: Float = 0.8f,
    threshold: Int = 200
): Modifier {
    val scale by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                val progress = (listState.firstVisibleItemScrollOffset.toFloat() / threshold).coerceIn(0f, 1f)
                1f - (1f - minScale) * progress
            } else minScale
        }
    }
    return this.scale(scale)
}

/**
 * Item visibility state for scroll-triggered animations.
 */
@Stable
class ItemVisibilityState {
    var isVisible by mutableStateOf(false)
        internal set
    var visibilityProgress by mutableFloatStateOf(0f)
        internal set
}

@Composable
fun rememberItemVisibilityState() = remember { ItemVisibilityState() }

/**
 * Modifier to track item visibility in viewport.
 */
fun Modifier.trackVisibility(
    state: ItemVisibilityState,
    viewportHeight: Int
) = onGloballyPositioned { coordinates ->
    val itemTop = coordinates.positionInParent().y
    val itemHeight = coordinates.size.height
    val itemBottom = itemTop + itemHeight
    
    // Check if item is in viewport
    state.isVisible = itemTop < viewportHeight && itemBottom > 0
    
    // Calculate visibility progress (0 = just entering, 1 = fully visible)
    state.visibilityProgress = if (state.isVisible) {
        val visibleHeight = minOf(itemBottom, viewportHeight.toFloat()) - maxOf(itemTop, 0f)
        (visibleHeight / itemHeight).coerceIn(0f, 1f)
    } else 0f
}

/**
 * Scroll velocity tracker for momentum-based effects.
 */
@Composable
fun scrollVelocity(listState: LazyListState): Float {
    var previousOffset by remember { mutableIntStateOf(0) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var velocity by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(listState.firstVisibleItemScrollOffset, listState.firstVisibleItemIndex) {
        val currentOffset = listState.firstVisibleItemScrollOffset
        val currentIndex = listState.firstVisibleItemIndex
        
        velocity = if (currentIndex == previousIndex) {
            (currentOffset - previousOffset).toFloat()
        } else {
            (currentIndex - previousIndex) * 100f
        }
        
        previousOffset = currentOffset
        previousIndex = currentIndex
    }
    
    return velocity
}

/**
 * Sticky header elevation based on scroll.
 */
@Composable
fun stickyHeaderElevation(
    listState: LazyListState,
    maxElevation: Float = 8f
): Float {
    return remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
                maxElevation
            } else 0f
        }
    }.value
}
