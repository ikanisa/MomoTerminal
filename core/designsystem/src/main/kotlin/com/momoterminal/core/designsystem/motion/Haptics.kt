package com.momoterminal.core.designsystem.motion

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Haptic feedback patterns for MomoTerminal interactions.
 */
enum class MomoHaptic {
    Tap,
    NfcDetected,
    NfcSuccess,
    NfcError,
    PaymentSuccess,
    PaymentError,
    BalanceUpdate,
    SmsSync,
    ButtonPress,
    Warning
}

/**
 * Haptic engine for triggering vibration patterns.
 */
class MomoHapticEngine(private val context: Context) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    fun trigger(haptic: MomoHaptic) {
        val v = vibrator ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (haptic) {
                MomoHaptic.Tap, MomoHaptic.ButtonPress ->
                    VibrationEffect.createOneShot(25, 80)
                MomoHaptic.NfcDetected ->
                    VibrationEffect.createOneShot(40, 120)
                MomoHaptic.NfcSuccess, MomoHaptic.PaymentSuccess ->
                    VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 80), intArrayOf(0, 100, 0, 150), -1)
                MomoHaptic.NfcError, MomoHaptic.PaymentError ->
                    VibrationEffect.createWaveform(longArrayOf(0, 60, 40, 60, 40, 60), intArrayOf(0, 180, 0, 180, 0, 180), -1)
                MomoHaptic.BalanceUpdate ->
                    VibrationEffect.createOneShot(30, 60)
                MomoHaptic.SmsSync ->
                    VibrationEffect.createWaveform(longArrayOf(0, 20, 30, 20), intArrayOf(0, 60, 0, 60), -1)
                MomoHaptic.Warning ->
                    VibrationEffect.createOneShot(100, 200)
            }
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            when (haptic) {
                MomoHaptic.Tap, MomoHaptic.ButtonPress -> v.vibrate(25)
                MomoHaptic.NfcDetected -> v.vibrate(40)
                MomoHaptic.NfcSuccess, MomoHaptic.PaymentSuccess -> v.vibrate(longArrayOf(0, 40, 60, 80), -1)
                MomoHaptic.NfcError, MomoHaptic.PaymentError -> v.vibrate(longArrayOf(0, 60, 40, 60, 40, 60), -1)
                MomoHaptic.BalanceUpdate -> v.vibrate(30)
                MomoHaptic.SmsSync -> v.vibrate(longArrayOf(0, 20, 30, 20), -1)
                MomoHaptic.Warning -> v.vibrate(100)
            }
        }
    }
    
    fun isAvailable(): Boolean = vibrator?.hasVibrator() == true
}

fun View.performMomoHaptic(haptic: MomoHaptic) {
    val constant = when (haptic) {
        MomoHaptic.Tap, MomoHaptic.ButtonPress -> HapticFeedbackConstants.KEYBOARD_TAP
        MomoHaptic.NfcDetected, MomoHaptic.BalanceUpdate, MomoHaptic.SmsSync -> HapticFeedbackConstants.KEYBOARD_TAP
        MomoHaptic.NfcSuccess, MomoHaptic.PaymentSuccess -> 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP
        MomoHaptic.NfcError, MomoHaptic.PaymentError -> 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        MomoHaptic.Warning -> HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}

@Composable
fun rememberMomoHaptic(): (MomoHaptic) -> Unit {
    val view = LocalView.current
    return remember { { haptic: MomoHaptic -> view.performMomoHaptic(haptic) } }
}
