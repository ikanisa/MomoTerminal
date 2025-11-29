package com.momoterminal.presentation.screens.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.momoterminal.security.BiometricHelper
import kotlinx.coroutines.flow.collectLatest

/**
 * Composable wrapper for biometric authentication prompt.
 * Handles the BiometricPrompt integration with Compose.
 */
@Composable
fun BiometricPromptScreen(
    biometricHelper: BiometricHelper,
    title: String = "Authenticate",
    subtitle: String? = null,
    description: String? = null,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(Unit) {
        if (activity != null) {
            biometricHelper.authenticate(
                activity = activity,
                title = title,
                subtitle = subtitle,
                description = description,
                negativeButtonText = "Cancel",
                allowDeviceCredential = true
            ).collectLatest { result ->
                when (result) {
                    is BiometricHelper.BiometricResult.Success -> onSuccess()
                    is BiometricHelper.BiometricResult.Cancelled -> onCancel()
                    is BiometricHelper.BiometricResult.Failed -> {
                        // Don't close - let user retry
                    }
                    is BiometricHelper.BiometricResult.Error -> {
                        onError(result.errorMessage)
                    }
                    is BiometricHelper.BiometricResult.NotAvailable -> {
                        onError("Biometric authentication not available")
                    }
                    is BiometricHelper.BiometricResult.NotEnrolled -> {
                        onError("No biometrics enrolled. Please set up fingerprint or face in Settings")
                    }
                    is BiometricHelper.BiometricResult.HardwareUnavailable -> {
                        onError("Biometric hardware unavailable")
                    }
                }
            }
        } else {
            onError("Unable to show biometric prompt")
        }
    }

    // Empty box while biometric prompt is showing
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Biometric prompt is shown as a system dialog
    }
}

/**
 * Biometric authentication for payment confirmation.
 */
@Composable
fun BiometricPaymentConfirmation(
    biometricHelper: BiometricHelper,
    amount: String,
    recipient: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(Unit) {
        if (activity != null) {
            biometricHelper.authenticateForPayment(
                activity = activity,
                amount = amount,
                recipient = recipient
            ).collectLatest { result ->
                when (result) {
                    is BiometricHelper.BiometricResult.Success -> onSuccess()
                    is BiometricHelper.BiometricResult.Cancelled -> onCancel()
                    is BiometricHelper.BiometricResult.Failed -> {
                        // Don't close - let user retry
                    }
                    is BiometricHelper.BiometricResult.Error -> {
                        onError(result.errorMessage)
                    }
                    else -> {
                        onError("Biometric authentication failed")
                    }
                }
            }
        } else {
            onError("Unable to show biometric prompt")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Biometric prompt is shown as a system dialog
    }
}

/**
 * Biometric authentication for session unlock.
 */
@Composable
fun BiometricSessionUnlock(
    biometricHelper: BiometricHelper,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit
) {
    BiometricPromptScreen(
        biometricHelper = biometricHelper,
        title = "Unlock MomoTerminal",
        subtitle = "Use biometrics to unlock",
        description = "Your session has expired. Authenticate to continue.",
        onSuccess = onSuccess,
        onError = onError,
        onCancel = onCancel
    )
}
