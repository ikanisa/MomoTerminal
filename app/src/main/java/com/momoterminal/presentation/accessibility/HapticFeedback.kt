package com.momoterminal.presentation.accessibility

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Types of haptic feedback available in the app.
 */
enum class HapticType {
    /**
     * Light tap feedback for button presses.
     */
    Tap,
    
    /**
     * Success feedback for successful operations.
     */
    Success,
    
    /**
     * Error feedback for failed operations.
     */
    Error,
    
    /**
     * Warning feedback for attention-requiring situations.
     */
    Warning
}

/**
 * Helper class for providing haptic feedback throughout the app.
 * Supports different vibration patterns for different feedback types.
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
    
    /**
     * Trigger haptic feedback of the specified type.
     */
    fun trigger(type: HapticType) {
        val vibrator = this.vibrator ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (type) {
                HapticType.Tap -> VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
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
                HapticType.Tap -> vibrator.vibrate(50)
                HapticType.Success -> vibrator.vibrate(longArrayOf(0, 50, 50, 100), -1)
                HapticType.Error -> vibrator.vibrate(longArrayOf(0, 100, 50, 100, 50, 100), -1)
                HapticType.Warning -> vibrator.vibrate(150)
            }
        }
    }
    
    /**
     * Check if haptic feedback is available on this device.
     */
    fun isAvailable(): Boolean {
        return vibrator?.hasVibrator() == true
    }
}
