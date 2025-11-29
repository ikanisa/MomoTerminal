package com.momoterminal.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momoterminal.presentation.theme.MomoYellow

/**
 * PIN entry screen for transaction authorization and session unlock.
 * Features a numeric keypad with biometric option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinScreen(
    title: String = "Enter PIN",
    subtitle: String = "Enter your 6-digit PIN to continue",
    onPinEntered: (String) -> Unit,
    onCancel: () -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    isBiometricAvailable: Boolean = false,
    isLoading: Boolean = false,
    error: String? = null,
    maxAttempts: Int = 3,
    currentAttempts: Int = 0
) {
    var pin by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val pinLength = 6

    // Show error in snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Auto-submit when PIN is complete
    LaunchedEffect(pin) {
        if (pin.length == pinLength) {
            onPinEntered(pin)
            pin = "" // Reset after submission
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Attempt warning
            if (currentAttempts > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Attempts remaining: ${maxAttempts - currentAttempts}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // PIN dots indicator
            PinDotsIndicator(
                pinLength = pinLength,
                filledCount = pin.length
            )

            Spacer(modifier = Modifier.weight(1f))

            // Number pad
            NumberPad(
                onNumberClick = { number ->
                    if (pin.length < pinLength && !isLoading) {
                        pin += number
                    }
                },
                onBackspaceClick = {
                    if (pin.isNotEmpty() && !isLoading) {
                        pin = pin.dropLast(1)
                    }
                },
                onBiometricClick = if (isBiometricAvailable && onBiometricClick != null) {
                    onBiometricClick
                } else null,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PinDotsIndicator(
    pinLength: Int,
    filledCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(pinLength) { index ->
            val isFilled = index < filledCount
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFilled) {
                            MomoYellow
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        }
                    )
                    .border(
                        width = 2.dp,
                        color = if (isFilled) {
                            MomoYellow
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun NumberPad(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: (() -> Unit)?,
    enabled: Boolean
) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("biometric", "0", "backspace")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        numbers.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { item ->
                    when (item) {
                        "biometric" -> {
                            if (onBiometricClick != null) {
                                NumberPadKey(
                                    onClick = onBiometricClick,
                                    enabled = enabled
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric",
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(72.dp))
                            }
                        }
                        "backspace" -> {
                            NumberPadKey(
                                onClick = onBackspaceClick,
                                enabled = enabled
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        else -> {
                            NumberPadKey(
                                onClick = { onNumberClick(item) },
                                enabled = enabled
                            ) {
                                Text(
                                    text = item,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberPadKey(
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = if (enabled) 0.5f else 0.2f
                )
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Simplified PIN verification dialog for transaction confirmation.
 */
@Composable
fun PinVerificationScreen(
    transactionAmount: String? = null,
    transactionRecipient: String? = null,
    onPinVerified: (String) -> Unit,
    onCancel: () -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    isBiometricAvailable: Boolean = false,
    isLoading: Boolean = false,
    error: String? = null
) {
    val title = "Confirm Transaction"
    val subtitle = if (transactionAmount != null && transactionRecipient != null) {
        "Enter PIN to confirm payment of $transactionAmount to $transactionRecipient"
    } else {
        "Enter your PIN to confirm this transaction"
    }

    PinScreen(
        title = title,
        subtitle = subtitle,
        onPinEntered = onPinVerified,
        onCancel = onCancel,
        onBiometricClick = onBiometricClick,
        isBiometricAvailable = isBiometricAvailable,
        isLoading = isLoading,
        error = error
    )
}
