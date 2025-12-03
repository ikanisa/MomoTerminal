package com.momoterminal.designsystem.interaction

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.momoterminal.designsystem.motion.MotionTokens

/**
 * Composition local for reduced motion preference.
 */
val LocalReducedMotion = compositionLocalOf { false }

/**
 * Check if user prefers reduced motion.
 */
@Composable
fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE
            ) == 0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Provide reduced motion context to children.
 */
@Composable
fun ReducedMotionProvider(content: @Composable () -> Unit) {
    val reducedMotion = isReducedMotionEnabled()
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        content()
    }
}

/**
 * Get duration respecting reduced motion preference.
 */
@Composable
fun adaptiveDuration(baseDuration: Int = MotionTokens.STANDARD): Int {
    val reducedMotion = LocalReducedMotion.current
    return if (reducedMotion) 0 else baseDuration
}

/**
 * Motion scale factor (0 for reduced motion, 1 for normal).
 */
@Composable
fun motionScale(): Float {
    return if (LocalReducedMotion.current) 0f else 1f
}
