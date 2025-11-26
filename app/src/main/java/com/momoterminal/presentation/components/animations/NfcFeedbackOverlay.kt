package com.momoterminal.presentation.components.animations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.accessibility.HapticFeedbackHelper
import com.momoterminal.presentation.accessibility.HapticType

/**
 * NFC state representing different stages of NFC interaction.
 */
sealed class NfcState {
    data object Idle : NfcState()
    data object Scanning : NfcState()
    data object Processing : NfcState()
    data class Success(val message: String = "Payment Successful") : NfcState()
    data class Error(val message: String = "Payment Failed") : NfcState()
}

/**
 * Full-screen overlay that shows NFC state with appropriate animations.
 * Provides visual and haptic feedback during NFC interactions.
 */
@Composable
fun NfcFeedbackOverlay(
    nfcState: NfcState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    hapticHelper: HapticFeedbackHelper? = null
) {
    val isVisible = nfcState !is NfcState.Idle

    // Trigger haptic feedback based on state
    LaunchedEffect(nfcState) {
        when (nfcState) {
            is NfcState.Success -> hapticHelper?.trigger(HapticType.Success)
            is NfcState.Error -> hapticHelper?.trigger(HapticType.Error)
            is NfcState.Scanning -> hapticHelper?.trigger(HapticType.Tap)
            else -> { /* No haptic for idle or processing */ }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .semantics {
                    contentDescription = when (nfcState) {
                        is NfcState.Scanning -> "NFC scanning in progress. Hold your device near the terminal."
                        is NfcState.Processing -> "Processing payment. Please wait."
                        is NfcState.Success -> nfcState.message
                        is NfcState.Error -> nfcState.message
                        else -> ""
                    }
                    liveRegion = LiveRegionMode.Assertive
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                when (nfcState) {
                    is NfcState.Scanning -> {
                        NfcScanningAnimation()
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Hold your device near the terminal",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                    is NfcState.Processing -> {
                        ProcessingAnimation()
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Processing payment...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                    is NfcState.Success -> {
                        PaymentSuccessAnimation(
                            onAnimationEnd = onDismiss
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = nfcState.message,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                    is NfcState.Error -> {
                        PaymentErrorAnimation()
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = nfcState.message,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> { /* Idle state - nothing to show */ }
                }
            }
        }
    }
}
