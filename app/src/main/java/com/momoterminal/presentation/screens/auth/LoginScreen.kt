package com.momoterminal.presentation.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.R
import com.momoterminal.auth.AuthViewModel
import com.momoterminal.presentation.components.CountryCodeSelector
import com.momoterminal.presentation.components.MomoButton
import com.momoterminal.presentation.components.MomoTextField
import com.momoterminal.presentation.components.ButtonType
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
    var resendCountdown by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000)
            resendCountdown--
        }
    }

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

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isOtpSent) {
        if (uiState.isOtpSent && resendCountdown == 0) {
            resendCountdown = 60
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding() // Add IME padding for keyboard
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MomoYellow
            )

            Text(
                text = stringResource(R.string.service_description),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = stringResource(R.string.auth_login_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (uiState.isOtpSent) "Enter OTP from WhatsApp" else stringResource(R.string.auth_login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountryCodeSelector(
                    selectedCountryCode = uiState.countryCode,
                    onCountryCodeSelected = viewModel::updateCountryCode,
                    modifier = Modifier.padding(end = 8.dp)
                )

                MomoTextField(
                    value = uiState.phoneNumber,
                    onValueChange = viewModel::updatePhoneNumber,
                    label = stringResource(R.string.auth_phone_number),
                    placeholder = "78XXXXXXX",
                    modifier = Modifier.weight(1f),
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
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = uiState.isOtpSent) {
                Column {
                    Text(
                        text = stringResource(R.string.enter_otp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OtpInputField(
                        value = uiState.otpCode,
                        onValueChange = viewModel::updateOtpCode,
                        isError = uiState.error != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (resendCountdown > 0) {
                            Text(
                                text = stringResource(R.string.reg_resend_countdown, resendCountdown),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            TextButton(
                                onClick = {
                                    viewModel.requestOtp()
                                    resendCountdown = 60
                                },
                                enabled = !uiState.isLoading
                            ) {
                                Text(
                                    text = stringResource(R.string.resend_code),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = uiState.isLockedOut) {
                Text(
                    text = stringResource(R.string.too_many_attempts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            MomoButton(
                text = stringResource(if (uiState.isOtpSent) R.string.auth_sign_in else R.string.send_otp),
                onClick = {
                    if (uiState.isOtpSent) viewModel.login() else viewModel.requestOtp()
                },
                enabled = !uiState.isLoading && 
                         uiState.phoneNumber.isNotBlank() && 
                         (!uiState.isOtpSent || uiState.otpCode.length == 6) &&
                         !uiState.isLockedOut,
                isLoading = uiState.isLoading
            )

            if (uiState.isBiometricAvailable && !uiState.isOtpSent) {
                Spacer(modifier = Modifier.height(16.dp))
                MomoButton(
                    text = stringResource(R.string.auth_use_biometrics),
                    onClick = viewModel::triggerBiometricAuth,
                    type = ButtonType.OUTLINE,
                    enabled = !uiState.isLoading
                )
            }

            Spacer(modifier = Modifier.height(200.dp)) // Extra space for keyboard

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auth_no_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        text = stringResource(R.string.auth_register),
                        fontWeight = FontWeight.SemiBold,
                        color = MomoYellow
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
