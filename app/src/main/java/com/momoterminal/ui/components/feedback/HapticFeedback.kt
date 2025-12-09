package com.momoterminal.ui.components.feedback

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Manager class for haptic feedback with various feedback types.
 * Supports different vibration patterns for different user interactions.
 * Respects user's vibration preference setting.
 */
class HapticFeedbackManager(
    private val context: Context,
    private val view: View,
    private val isEnabled: () -> Boolean = { true }
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
     * Light tap feedback for button presses.
     */
    fun performClick() {
        if (!isEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Positive confirmation feedback for successful operations.
     * Uses CONFIRM effect on Android 11+ or double click pattern.
     */
    fun performSuccess() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 50, 50, 100),
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 50, 100), -1)
        }
    }

    /**
     * Negative feedback for failed operations.
     * Uses REJECT effect on Android 11+ or heavy click pattern.
     */
    fun performError() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 50, 100, 50, 100),
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 100, 50, 100, 50, 100), -1)
        }
    }

    /**
     * Moderate feedback for warning situations.
     */
    fun performWarning() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(150)
        }
    }

    /**
     * Long press feedback.
     */
    fun performLongPress() {
        if (!isEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /**
     * Keyboard tap feedback.
     */
    fun performKeyboardTap() {
        if (!isEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Context click (right-click) feedback.
     */
    fun performContextClick() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } else {
            performClick()
        }
    }

    /**
     * Custom pattern for money received (credit) transactions.
     * Celebratory double-pulse pattern.
     */
    fun performMoneyReceived() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 80, 100, 80, 100, 150),
                    intArrayOf(0, 200, 0, 200, 0, 255),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 80, 100, 80, 100, 150), -1)
        }
    }

    /**
     * Custom pattern for money sent (debit) transactions.
     * Short acknowledgment pulse.
     */
    fun performMoneySent() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 50, 50, 100),
                    intArrayOf(0, 180, 0, 255),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 50, 100), -1)
        }
    }

    /**
     * NFC detection feedback.
     * Quick pulse to indicate NFC tap detected.
     */
    fun performNfcTap() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(30)
        }
    }

    /**
     * Heavy click feedback.
     */
    fun performHeavyClick() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(100, 255)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(100)
        }
    }

    /**
     * Double click feedback.
     */
    fun performDoubleClick() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 40, 60, 40),
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 40, 60, 40), -1)
        }
    }

    /**
     * Check if haptic feedback is available.
     */
    fun isAvailable(): Boolean {
        return vibrator?.hasVibrator() == true
    }
}

/**
 * Remember a HapticFeedbackManager instance.
 * Uses default enabled state (always on).
 * For preference-aware feedback, inject UserPreferences and collect the flow.
 */
@Composable
fun rememberHapticFeedback(
    isEnabled: () -> Boolean = { true }
): HapticFeedbackManager {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view, isEnabled) {
        HapticFeedbackManager(context, view, isEnabled)
    }
}

/**
 * Modifier extension to add haptic feedback on click.
 *
 * @param feedbackType The type of haptic feedback to perform
 * @param onClick The click handler
 */
fun Modifier.withHapticFeedback(
    feedbackType: HapticFeedbackType = HapticFeedbackType.LongPress,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current

    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) {
        when (feedbackType) {
            HapticFeedbackType.LongPress -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            HapticFeedbackType.TextHandleMove -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            else -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        onClick()
    }
}

/**
 * Modifier extension to add click haptic feedback.
 *
 * @param onClick The click handler
 */
fun Modifier.withClickHaptic(
    onClick: () -> Unit
): Modifier = composed {
    val haptic = rememberHapticFeedback()

    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) {
        haptic.performClick()
        onClick()
    }
}
