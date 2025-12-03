package com.momoterminal.designsystem.interaction

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.motion.MomoHaptic
import com.momoterminal.designsystem.motion.performMomoHaptic
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Pull-to-refresh state.
 */
@Stable
class PullToRefreshState {
    var isRefreshing by mutableStateOf(false)
    var pullProgress by mutableFloatStateOf(0f)
        internal set
    
    internal var offsetY by mutableFloatStateOf(0f)
    
    val isTriggered: Boolean get() = pullProgress >= 1f
}

@Composable
fun rememberPullToRefreshState() = remember { PullToRefreshState() }

/**
 * Pull-to-refresh container with indicator.
 */
@Composable
fun PullToRefreshContainer(
    state: PullToRefreshState,
    onRefresh: suspend () -> Unit,
    modifier: Modifier = Modifier,
    threshold: Float = 80f,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (state.isRefreshing) return androidx.compose.ui.geometry.Offset.Zero
                
                // Only consume if pulling down and we have offset
                if (available.y < 0 && state.offsetY > 0) {
                    val consumed = available.y.coerceAtLeast(-state.offsetY)
                    state.offsetY += consumed
                    state.pullProgress = (state.offsetY / threshold).coerceIn(0f, 1.5f)
                    return androidx.compose.ui.geometry.Offset(0f, consumed)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
            
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (state.isRefreshing) return androidx.compose.ui.geometry.Offset.Zero
                
                // Pull down when at top
                if (available.y > 0) {
                    val resistance = if (state.offsetY > threshold) 0.4f else 0.6f
                    state.offsetY = (state.offsetY + available.y * resistance).coerceAtMost(threshold * 1.5f)
                    state.pullProgress = (state.offsetY / threshold).coerceIn(0f, 1.5f)
                    
                    // Haptic at threshold
                    if (state.pullProgress >= 1f && !hasTriggeredHaptic) {
                        view.performMomoHaptic(MomoHaptic.Tap)
                        hasTriggeredHaptic = true
                    } else if (state.pullProgress < 1f) {
                        hasTriggeredHaptic = false
                    }
                    
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
            
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (state.isRefreshing) return Velocity.Zero
                
                if (state.pullProgress >= 1f) {
                    state.isRefreshing = true
                    view.performMomoHaptic(MomoHaptic.ButtonPress)
                    scope.launch {
                        onRefresh()
                        state.isRefreshing = false
                        state.offsetY = 0f
                        state.pullProgress = 0f
                    }
                } else {
                    state.offsetY = 0f
                    state.pullProgress = 0f
                }
                hasTriggeredHaptic = false
                return Velocity.Zero
            }
        }
    }
    
    Box(
        modifier = modifier.nestedScroll(nestedScrollConnection)
    ) {
        // Content with offset
        Box(
            modifier = Modifier.graphicsLayer {
                translationY = state.offsetY
            }
        ) {
            content()
        }
        
        // Refresh indicator
        PullIndicator(
            progress = state.pullProgress,
            isRefreshing = state.isRefreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = state.offsetY - 60f
                    alpha = state.pullProgress.coerceIn(0f, 1f)
                }
        )
    }
}

@Composable
private fun PullIndicator(
    progress: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = progress * 180f,
        animationSpec = tween(100),
        label = "pullRotation"
    )
    
    Box(
        modifier = modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
