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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import com.momoterminal.presentation.components.CountryCodeSelector
import com.momoterminal.presentation.components.MomoButton
import com.momoterminal.presentation.components.MomoTextField
import com.momoterminal.presentation.components.OtpInputField
import com.momoterminal.presentation.theme.MomoYellow

/** Timer update interval for OTP expiry countdown in milliseconds */
private const val OTP_TIMER_UPDATE_INTERVAL_MS = 1000L

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
                title = { Text(stringResource(R.string.auth_register_title)) },
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
                            contentDescription = stringResource(R.string.back)
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
                            countryCode = uiState.countryCode,
                            onPhoneChange = viewModel::updatePhoneNumber,
                            onCountryCodeChange = viewModel::updateCountryCode,
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
                            onChangePhoneNumber = viewModel::changePhoneNumber,
                            phoneNumber = uiState.phoneNumber,
                            formattedPhoneNumber = uiState.formattedPhoneNumber.ifBlank { uiState.phoneNumber },
                            otpExpiryCountdown = uiState.otpExpiryCountdown,
                            resendCountdown = uiState.resendCountdown,
                            canResendOtp = uiState.canResendOtp
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
                    text = stringResource(R.string.auth_has_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = stringResource(R.string.auth_sign_in),
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
    val steps = listOf(
        R.string.reg_step_phone,
        R.string.reg_step_otp,
        R.string.reg_step_pin,
        R.string.reg_step_info,
        R.string.reg_step_terms
    )
    val currentIndex = currentStep.ordinal

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, stepRes ->
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
                    text = stringResource(stepRes),
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
    countryCode: String,
    onPhoneChange: (String) -> Unit,
    onCountryCodeChange: (String) -> Unit,
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
            text = stringResource(R.string.reg_welcome),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.reg_phone_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        // WhatsApp Info Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("📱", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.reg_whatsapp_info),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountryCodeSelector(
                selectedCountryCode = countryCode,
                onCountryCodeSelected = onCountryCodeChange,
                modifier = Modifier.padding(end = 8.dp)
            )

            MomoTextField(
                value = phoneNumber,
                onValueChange = onPhoneChange,
                label = stringResource(R.string.auth_phone_number),
                placeholder = "78XXXXXXX",
                modifier = Modifier.weight(1f),
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
                enabled = !isLoading,
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        MomoButton(
            text = stringResource(R.string.reg_send_code),
            onClick = onRequestOtp,
            enabled = !isLoading && phoneNumber.isNotBlank(),
            isLoading = isLoading
        )
    }
}

@Composable
private fun OtpVerificationStep(
    otpCode: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    onVerifyOtp: () -> Unit,
    onResendOtp: () -> Unit,
    onChangePhoneNumber: () -> Unit,
    phoneNumber: String,
    formattedPhoneNumber: String,
    otpExpiryCountdown: Int,
    resendCountdown: Int = 0,
    canResendOtp: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.reg_verify_phone),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("📱") // WhatsApp icon
            Text(
                text = stringResource(R.string.reg_otp_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = formattedPhoneNumber,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        
        // OTP Expiry Timer - Removed duplicate display here, kept the one below
        if (otpExpiryCountdown <= 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reg_code_expired_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        
        // Phone number display with change option
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formattedPhoneNumber.ifEmpty { phoneNumber },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            TextButton(
                onClick = onChangePhoneNumber,
                enabled = !isLoading
            ) {
                Text(
                    text = stringResource(R.string.reg_change),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // OTP expiry timer
        if (otpExpiryCountdown > 0) {
            val minutes = otpExpiryCountdown / 60
            val seconds = otpExpiryCountdown % 60
            Text(
                text = stringResource(R.string.reg_code_expires, minutes, seconds.toString().padStart(2, '0')),
                style = MaterialTheme.typography.bodySmall,
                color = if (otpExpiryCountdown <= 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = stringResource(R.string.reg_code_expired),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OtpInputField(
            value = otpCode,
            onValueChange = onOtpChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))



        Spacer(modifier = Modifier.height(16.dp))

        // Resend button with countdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canResendOtp) {
                TextButton(
                    onClick = onResendOtp,
                    enabled = !isLoading
                ) {
                    Text(
                        text = stringResource(R.string.reg_resend_code),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.reg_resend_countdown, resendCountdown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        MomoButton(
            text = stringResource(R.string.reg_verify),
            onClick = onVerifyOtp,
            enabled = !isLoading && otpCode.length == 6 && otpExpiryCountdown > 0,
            isLoading = isLoading
        )
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
            text = stringResource(R.string.reg_secure_account),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.reg_pin_instruction),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        MomoTextField(
            value = pin,
            onValueChange = onPinChange,
            label = stringResource(R.string.auth_pin),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
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

        MomoTextField(
            value = confirmPin,
            onValueChange = onConfirmPinChange,
            label = stringResource(R.string.auth_confirm_pin),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showConfirmPin) VisualTransformation.None else PasswordVisualTransformation(),
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
            errorMessage = if (confirmPin.isNotEmpty() && pin != confirmPin) stringResource(R.string.reg_pins_not_match) else null
        )

        Spacer(modifier = Modifier.height(32.dp))

        MomoButton(
            text = stringResource(R.string.reg_continue),
            onClick = onNext,
            enabled = !isLoading && pin.length == 6 && pin == confirmPin,
            isLoading = isLoading
        )
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
            text = stringResource(R.string.reg_business_profile),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.reg_business_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        MomoTextField(
            value = merchantName,
            onValueChange = onMerchantNameChange,
            label = stringResource(R.string.auth_merchant_name),
            placeholder = "My Store",
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

        Spacer(modifier = Modifier.height(32.dp))

        MomoButton(
            text = stringResource(R.string.reg_continue),
            onClick = onNext,
            enabled = !isLoading && merchantName.isNotBlank(),
            isLoading = isLoading
        )
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
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.terms_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        // Terms Card
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.terms_content),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

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

        Spacer(modifier = Modifier.height(32.dp))

        MomoButton(
            text = stringResource(R.string.auth_sign_up),
            onClick = onRegister,
            enabled = !isLoading && acceptedTerms,
            isLoading = isLoading
        )
    }
}
