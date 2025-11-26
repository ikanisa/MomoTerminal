package com.momoterminal.ui.components.loading

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.momoterminal.ui.components.feedback.rememberHapticFeedback

/**
 * Pull-to-refresh box with Material 3 design and haptic feedback.
 *
 * @param isRefreshing Whether the refresh is in progress
 * @param onRefresh Callback when refresh is triggered
 * @param modifier Modifier for the box
 * @param enableHapticFeedback Whether to trigger haptic feedback on refresh
 * @param content The scrollable content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomoPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enableHapticFeedback: Boolean = true,
    content: @Composable () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val state = rememberPullToRefreshState()

    // Trigger haptic feedback when refresh starts
    LaunchedEffect(isRefreshing) {
        if (isRefreshing && enableHapticFeedback) {
            haptic.performSuccess()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (enableHapticFeedback) {
                haptic.performClick()
            }
            onRefresh()
        },
        modifier = modifier,
        state = state,
        content = { content() }
    )
}

/**
 * Custom pull-to-refresh indicator with rotation and scale animations.
 *
 * @param state The pull-to-refresh state
 * @param isRefreshing Whether the refresh is in progress
 * @param modifier Modifier for the indicator
 * @param color The color of the indicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPullToRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(0f) }

    LaunchedEffect(state.distanceFraction) {
        // Scale based on pull distance
        scale.animateTo(
            targetValue = (state.distanceFraction * 1.2f).coerceIn(0f, 1f),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            // Continuous rotation when refreshing
            while (true) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessVeryLow
                    )
                )
            }
        } else {
            // Reset rotation when not refreshing
            rotation.animateTo(0f)
        }
    }

    Box(
        modifier = modifier
            .scale(scale.value)
            .rotate(rotation.value),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = color,
            strokeWidth = 2.dp
        )
    }
}

/**
 * Animated pull-to-refresh with optional Lottie animation support.
 * For now, uses a custom animated indicator.
 *
 * @param isRefreshing Whether the refresh is in progress
 * @param onRefresh Callback when refresh is triggered
 * @param modifier Modifier for the container
 * @param indicatorColor The color of the refresh indicator
 * @param enableHapticFeedback Whether to trigger haptic feedback
 * @param content The scrollable content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    enableHapticFeedback: Boolean = true,
    content: @Composable () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val state = rememberPullToRefreshState()

    LaunchedEffect(isRefreshing) {
        if (isRefreshing && enableHapticFeedback) {
            haptic.performSuccess()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (enableHapticFeedback) {
                haptic.performClick()
            }
            onRefresh()
        },
        modifier = modifier,
        state = state,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = state,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                color = indicatorColor
            )
        },
        content = { content() }
    )
}

/**
 * Simple pull-to-refresh wrapper for lazy lists.
 *
 * @param isRefreshing Whether the refresh is in progress
 * @param onRefresh Callback when refresh is triggered
 * @param modifier Modifier for the container
 * @param content The lazy list content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    MomoPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        content()
    }
}
