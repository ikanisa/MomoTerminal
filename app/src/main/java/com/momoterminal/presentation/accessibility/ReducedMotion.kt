package com.momoterminal.presentation.accessibility

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Composable that checks if the user has enabled "Reduce Motion" accessibility setting.
 * This should be used to disable or simplify animations for users who prefer reduced motion.
 * 
 * @return true if reduce motion is enabled (animation duration scale is 0), false otherwise
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        val animationScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        animationScale == 0f
    }
}

/**
 * Extension function to get animation duration respecting reduce motion setting.
 * Returns 0 if reduce motion is enabled, otherwise returns the original duration.
 */
fun Int.respectReducedMotion(reduceMotion: Boolean): Int {
    return if (reduceMotion) 0 else this
}

/**
 * Extension function to get animation duration respecting reduce motion setting.
 * Returns 0 if reduce motion is enabled, otherwise returns the original duration.
 */
fun Long.respectReducedMotion(reduceMotion: Boolean): Long {
    return if (reduceMotion) 0L else this
}
