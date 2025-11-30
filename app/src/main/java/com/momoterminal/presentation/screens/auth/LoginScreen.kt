package com.momoterminal.presentation.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.auth.AuthViewModel
import com.momoterminal.presentation.components.OtpInputField
import com.momoterminal.presentation.theme.MomoYellow
import kotlinx.coroutines.delay

/**
 * Login screen for user authentication.
 * Supports WhatsApp OTP and biometric authentication.
 */
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToForgotPin: () -> Unit = {},
    onShowBiometricPrompt: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val events by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Resend OTP countdown timer
    var resendCountdown by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000)
            resendCountdown--
        }
    }

    // Handle events
    LaunchedEffect(events) {
        when (events) {
            is AuthViewModel.AuthEvent.NavigateToHome -> {
                viewModel.clearEvent()
                onNavigateToHome()
            }
            is AuthViewModel.AuthEvent.ShowBiometricPrompt -> {
                viewModel.clearEvent()
                onShowBiometricPrompt()
            }
            is AuthViewModel.AuthEvent.ShowError -> {
                snackbarHostState.showSnackbar((events as AuthViewModel.AuthEvent.ShowError).message)
                viewModel.clearEvent()
            }
            else -> {}
        }
    }

    // Show error in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Start countdown when OTP is sent
    LaunchedEffect(uiState.isOtpSent) {
        if (uiState.isOtpSent && resendCountdown == 0) {
            resendCountdown = 60 // 60 seconds countdown
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // App Logo/Title
            Text(
                text = "MomoTerminal",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MomoYellow
            )

            Text(
                text = "Mobile Money POS",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = if (uiState.isOtpSent) "Enter OTP from WhatsApp" else "Sign in to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Phone Number Input
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = viewModel::updatePhoneNumber,
                label = { Text("Phone Number") },
                placeholder = { Text("078XXXXXXX") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = if (uiState.isOtpSent) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (!uiState.isOtpSent) {
                            focusManager.clearFocus()
                            viewModel.requestOtp()
                        }
                    }
                ),
                enabled = !uiState.isLoading && !uiState.isOtpSent
            )

            Spacer(modifier = Modifier.height(16.dp))

            // OTP Input (shown after OTP is sent)
            AnimatedVisibility(visible = uiState.isOtpSent) {
                Column {
                    Text(
                        text = "Enter 6-digit OTP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OtpInputField(
                        value = uiState.otpCode,
                        onValueChange = viewModel::updateOtpCode,
                        isError = uiState.error != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Resend OTP button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Didn't receive OTP?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        TextButton(
                            onClick = {
                                viewModel.requestOtp()
                                resendCountdown = 60
                            },
                            enabled = resendCountdown == 0 && !uiState.isLoading
                        ) {
                            Text(
                                text = if (resendCountdown > 0) "Resend in ${resendCountdown}s" else "Resend OTP",
                                color = if (resendCountdown > 0) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Lockout warning
            AnimatedVisibility(visible = uiState.isLockedOut) {
                Text(
                    text = "Too many failed attempts. Please try again later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Send OTP / Sign In Button
            Button(
                onClick = {
                    if (uiState.isOtpSent) {
                        viewModel.login()
                    } else {
                        viewModel.requestOtp()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading && 
                         uiState.phoneNumber.isNotBlank() && 
                         (!uiState.isOtpSent || uiState.otpCode.length == 6) &&
                         !uiState.isLockedOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MomoYellow,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!uiState.isOtpSent) {
                            // WhatsApp icon placeholder - you can add actual icon
                            Text("📱")
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (uiState.isOtpSent) "Sign In" else "Send OTP via WhatsApp",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Biometric Login Option
            if (uiState.isBiometricAvailable && !uiState.isOtpSent) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = viewModel::triggerBiometricAuth,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Biometrics")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Register Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        text = "Register",
                        fontWeight = FontWeight.SemiBold,
                        color = MomoYellow
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
