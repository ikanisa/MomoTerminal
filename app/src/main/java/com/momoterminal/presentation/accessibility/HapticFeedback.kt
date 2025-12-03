package com.momoterminal.presentation.accessibility

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Types of haptic feedback available in the app.
 */
enum class HapticType {
    Tap,
    Success,
    Error,
    Warning,
    KeypadTap
}

/**
 * Helper class for providing haptic feedback throughout the app.
 */
@Singleton
class HapticFeedbackHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    fun trigger(type: HapticType) {
        val vibrator = this.vibrator ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (type) {
                HapticType.Tap, HapticType.KeypadTap -> 
                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                HapticType.Success -> VibrationEffect.createWaveform(
                    longArrayOf(0, 50, 50, 100),
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1
                )
                HapticType.Error -> VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 50, 100, 50, 100),
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1
                )
                HapticType.Warning -> VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            when (type) {
                HapticType.Tap, HapticType.KeypadTap -> vibrator.vibrate(30)
                HapticType.Success -> vibrator.vibrate(longArrayOf(0, 50, 50, 100), -1)
                HapticType.Error -> vibrator.vibrate(longArrayOf(0, 100, 50, 100, 50, 100), -1)
                HapticType.Warning -> vibrator.vibrate(150)
            }
        }
    }
    
    fun isAvailable(): Boolean = vibrator?.hasVibrator() == true
}

/**
 * Trigger haptic feedback from a View (Compose-friendly).
 */
fun View.performHaptic(type: HapticType = HapticType.Tap) {
    val constant = when (type) {
        HapticType.Tap, HapticType.KeypadTap -> HapticFeedbackConstants.KEYBOARD_TAP
        HapticType.Success -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 
            HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP
        HapticType.Error -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 
            HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        HapticType.Warning -> HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}

/**
 * Modifier extension for clickable with haptic feedback.
 */
fun Modifier.hapticClickable(
    hapticType: HapticType = HapticType.Tap,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    
    this.clickable(
        interactionSource = interactionSource,
        indication = null
    ) {
        view.performHaptic(hapticType)
        onClick()
    }
}

/**
 * Composable to get haptic trigger function.
 */
@Composable
fun rememberHapticTrigger(): (HapticType) -> Unit {
    val view = LocalView.current
    return remember { { type: HapticType -> view.performHaptic(type) } }
}
