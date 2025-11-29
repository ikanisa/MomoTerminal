package com.momoterminal.presentation.screens.auth

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.R
import com.momoterminal.auth.AuthViewModel
import com.momoterminal.presentation.theme.MomoYellow

/**
 * Registration screen with multi-step flow.
 * Steps: Phone Entry -> OTP Verification -> PIN Creation -> Merchant Info -> Terms Acceptance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val events by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(events) {
        when (events) {
            is AuthViewModel.AuthEvent.NavigateToHome -> {
                viewModel.clearEvent()
                onNavigateToHome()
            }
            is AuthViewModel.AuthEvent.NavigateToLogin -> {
                viewModel.clearEvent()
                onNavigateToLogin()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.registrationStep == AuthViewModel.RegistrationStep.PHONE_ENTRY) {
                            onNavigateToLogin()
                        } else {
                            viewModel.previousRegistrationStep()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .padding(24.dp)
        ) {
            // Step indicator
            StepIndicator(
                currentStep = uiState.registrationStep,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step content
            AnimatedContent(
                targetState = uiState.registrationStep,
                label = "registration_step"
            ) { step ->
                when (step) {
                    AuthViewModel.RegistrationStep.PHONE_ENTRY -> {
                        PhoneEntryStep(
                            phoneNumber = uiState.phoneNumber,
                            onPhoneChange = viewModel::updatePhoneNumber,
                            isLoading = uiState.isLoading,
                            onRequestOtp = viewModel::requestOtp
                        )
                    }
                    AuthViewModel.RegistrationStep.OTP_VERIFICATION -> {
                        OtpVerificationStep(
                            otpCode = uiState.otpCode,
                            onOtpChange = viewModel::updateOtpCode,
                            isLoading = uiState.isLoading,
                            onVerifyOtp = viewModel::verifyOtp,
                            onResendOtp = viewModel::requestOtp,
                            phoneNumber = uiState.formattedPhoneNumber.ifBlank { uiState.phoneNumber },
                            resendCountdown = uiState.otpResendCountdown,
                            otpExpiresAt = uiState.otpExpiresAt
                        )
                    }
                    AuthViewModel.RegistrationStep.PIN_CREATION -> {
                        PinCreationStep(
                            pin = uiState.pin,
                            confirmPin = uiState.confirmPin,
                            onPinChange = viewModel::updatePin,
                            onConfirmPinChange = viewModel::updateConfirmPin,
                            isLoading = uiState.isLoading,
                            onNext = viewModel::nextRegistrationStep
                        )
                    }
                    AuthViewModel.RegistrationStep.MERCHANT_INFO -> {
                        MerchantInfoStep(
                            merchantName = uiState.merchantName,
                            onMerchantNameChange = viewModel::updateMerchantName,
                            isLoading = uiState.isLoading,
                            onNext = viewModel::nextRegistrationStep
                        )
                    }
                    AuthViewModel.RegistrationStep.TERMS_ACCEPTANCE -> {
                        TermsAcceptanceStep(
                            acceptedTerms = uiState.acceptedTerms,
                            onTermsChange = viewModel::updateTermsAcceptance,
                            isLoading = uiState.isLoading,
                            onRegister = viewModel::register
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Login link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = "Sign In",
                        fontWeight = FontWeight.SemiBold,
                        color = MomoYellow
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: AuthViewModel.RegistrationStep,
    modifier: Modifier = Modifier
) {
    val steps = listOf("Phone", "OTP", "PIN", "Info", "Terms")
    val currentIndex = currentStep.ordinal

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, step ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                val isCompleted = index < currentIndex
                val isCurrent = index == currentIndex

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = when {
                        isCompleted -> MomoYellow
                        isCurrent -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isCompleted || isCurrent -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
    }
}

@Composable
private fun PhoneEntryStep(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    isLoading: Boolean,
    onRequestOtp: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Enter Your Phone Number",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("📱") // WhatsApp icon
            Text(
                text = "We'll send you a verification code via WhatsApp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number") },
            placeholder = { Text("+250788767816") },
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
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onRequestOtp()
                }
            ),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestOtp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && phoneNumber.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MomoYellow,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isLoading) {
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
                    Text("📱")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send WhatsApp Code")
                }
            }
        }
    }
}

@Composable
private fun OtpVerificationStep(
    otpCode: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    onVerifyOtp: () -> Unit,
    onResendOtp: () -> Unit,
    phoneNumber: String,
    resendCountdown: Int = 0,
    otpExpiresAt: Long = 0L
) {
    // Calculate remaining time for OTP expiry
    var remainingSeconds by remember { mutableStateOf(0) }
    
    LaunchedEffect(otpExpiresAt) {
        if (otpExpiresAt > 0) {
            while (true) {
                val remaining = ((otpExpiresAt - System.currentTimeMillis()) / 1000).toInt()
                remainingSeconds = maxOf(0, remaining)
                if (remainingSeconds <= 0) break
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Verify Your Phone",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("📱") // WhatsApp icon
            Text(
                text = "Enter the 6-digit code sent to WhatsApp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = phoneNumber,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        
        // OTP Expiry Timer
        if (remainingSeconds > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            Text(
                text = "Code expires in ${minutes}:${String.format("%02d", seconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (remainingSeconds < 60) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (otpExpiresAt > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Code expired. Please request a new one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        com.momoterminal.presentation.components.OtpInputField(
            value = otpCode,
            onValueChange = onOtpChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onResendOtp,
            enabled = !isLoading && resendCountdown == 0
        ) {
            Text(
                if (resendCountdown > 0) 
                    "Resend in ${resendCountdown}s"
                else 
                    "Didn't receive the code? Resend"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onVerifyOtp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && otpCode.length == 6 && remainingSeconds > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = MomoYellow,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Verify")
            }
        }
    }
}

@Composable
private fun PinCreationStep(
    pin: String,
    confirmPin: String,
    onPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    isLoading: Boolean,
    onNext: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var showPin by remember { mutableStateOf(false) }
    var showConfirmPin by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Create Your PIN",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Choose a 6-digit PIN to secure your account",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            label = { Text("6-Digit PIN") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showPin) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { showPin = !showPin }) {
                    Icon(
                        imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPin,
            onValueChange = onConfirmPinChange,
            label = { Text("Confirm PIN") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showConfirmPin) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { showConfirmPin = !showConfirmPin }) {
                    Icon(
                        imageVector = if (showConfirmPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (pin.length == 6 && pin == confirmPin) {
                        onNext()
                    }
                }
            ),
            enabled = !isLoading,
            isError = confirmPin.isNotEmpty() && pin != confirmPin,
            supportingText = if (confirmPin.isNotEmpty() && pin != confirmPin) {
                { Text("PINs do not match") }
            } else null
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && pin.length == 6 && pin == confirmPin,
            colors = ButtonDefaults.buttonColors(
                containerColor = MomoYellow,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun MerchantInfoStep(
    merchantName: String,
    onMerchantNameChange: (String) -> Unit,
    isLoading: Boolean,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Business Information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Enter your business or merchant name",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = merchantName,
            onValueChange = onMerchantNameChange,
            label = { Text("Business Name") },
            placeholder = { Text("My Store") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && merchantName.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MomoYellow,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun TermsAcceptanceStep(
    acceptedTerms: Boolean,
    onTermsChange: (Boolean) -> Unit,
    isLoading: Boolean,
    onRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.terms_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = stringResource(R.string.terms_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Terms content from string resources
        Text(
            text = stringResource(R.string.terms_content),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = acceptedTerms,
                onCheckedChange = onTermsChange,
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.terms_accept_checkbox),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRegister,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && acceptedTerms,
            colors = ButtonDefaults.buttonColors(
                containerColor = MomoYellow,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Create Account")
            }
        }
    }
}
