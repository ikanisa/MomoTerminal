package com.momoterminal.presentation.components.animations

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.momoterminal.R
import com.momoterminal.presentation.accessibility.rememberReducedMotion

/**
 * Reusable Lottie animation composables for NFC feedback and other animations.
 * All animations respect the system "reduce motion" accessibility setting.
 */

/**
 * NFC scanning animation with pulsing effect.
 * Shows a pulsing NFC icon to indicate scanning is in progress.
 */
@Composable
fun NfcScanningAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val reduceMotion = rememberReducedMotion()
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.nfc_scanning)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = !reduceMotion
    )

    LottieAnimation(
        composition = composition,
        progress = { if (reduceMotion) 0f else progress },
        modifier = modifier.size(size)
    )
}

/**
 * Payment success animation with checkmark.
 * Shows a green checkmark animation on successful payment.
 */
@Composable
fun PaymentSuccessAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    onAnimationEnd: (() -> Unit)? = null
) {
    val reduceMotion = rememberReducedMotion()
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.nfc_success)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = !reduceMotion
    )

    // Trigger callback when animation completes
    if (progress >= 1f && onAnimationEnd != null) {
        onAnimationEnd()
    }

    LottieAnimation(
        composition = composition,
        progress = { if (reduceMotion) 1f else progress },
        modifier = modifier.size(size)
    )
}

/**
 * Payment error animation with X mark.
 * Shows a red X animation on payment failure.
 */
@Composable
fun PaymentErrorAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val reduceMotion = rememberReducedMotion()
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.nfc_error)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = !reduceMotion
    )

    LottieAnimation(
        composition = composition,
        progress = { if (reduceMotion) 1f else progress },
        modifier = modifier.size(size)
    )
}

/**
 * Processing animation with spinner.
 * Shows a loading spinner during payment processing.
 */
@Composable
fun ProcessingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    val reduceMotion = rememberReducedMotion()
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.payment_processing)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = !reduceMotion
    )

    LottieAnimation(
        composition = composition,
        progress = { if (reduceMotion) 0.5f else progress },
        modifier = modifier.size(size)
    )
}

/**
 * Sync loading animation with cloud icon.
 * Shows a syncing animation during data synchronization.
 */
@Composable
fun SyncLoadingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 60.dp
) {
    val reduceMotion = rememberReducedMotion()
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.sync_loading)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = !reduceMotion
    )

    LottieAnimation(
        composition = composition,
        progress = { if (reduceMotion) 0f else progress },
        modifier = modifier.size(size)
    )
}

/**
 * Empty state animation with document illustration.
 * Shows when there are no transactions to display.
 */
@Composable
fun EmptyStateAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val reduceMotion = rememberReducedMotion()
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.empty_state)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = !reduceMotion
    )

    LottieAnimation(
        composition = composition,
        progress = { if (reduceMotion) 0f else progress },
        modifier = modifier.size(size)
    )
}
