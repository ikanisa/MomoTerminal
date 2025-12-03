package com.momoterminal.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.motion.MomoHaptic
import com.momoterminal.core.designsystem.motion.MotionTokens
import com.momoterminal.core.designsystem.motion.performMomoHaptic

/**
 * PressableCard - Interactive card with scale + shadow + haptic feedback.
 * 
 * Used for:
 * - Wallet cards
 * - NFC terminal card
 * - SMS transaction items
 * - Token detail cards
 */
@Composable
fun PressableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    defaultElevation: Dp = 2.dp,
    pressedElevation: Dp = 0.dp,
    haptic: MomoHaptic = MomoHaptic.Tap,
    content: @Composable BoxScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = MotionTokens.SpringResponsive,
        label = "scale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed && enabled) pressedElevation else defaultElevation,
        label = "elevation"
    )
    
    Card(
        modifier = modifier
            .scale(scale)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        view.performMomoHaptic(haptic)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(content = content)
    }
}

/**
 * Variant for transaction rows - more subtle interaction
 */
@Composable
fun PressableRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.99f else 1f,
        animationSpec = MotionTokens.SpringSnappy,
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        view.performMomoHaptic(MomoHaptic.Tap)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        content = content
    )
}
