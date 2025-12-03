package com.momoterminal.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.R
import com.momoterminal.presentation.components.CountryCodeSelector
import com.momoterminal.presentation.components.MomoButton
import com.momoterminal.presentation.components.MomoTextField
import com.momoterminal.presentation.components.OtpInputField
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.SuccessGreen
import kotlinx.coroutines.delay

/**
 * Forgot PIN screen for resetting user PIN.
 * 
 * Flow:
 * 1. Enter registered phone number
 * 2. Verify WhatsApp OTP
 * 3. Enter new PIN
 * 4. Confirm new PIN
 * 5. Success
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPinScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ForgotPinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    
    // Resend OTP countdown
    var resendCountdown by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000)
            resendCountdown--
        }
    }
    
    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    // Handle success navigation
    LaunchedEffect(uiState.step) {
        if (uiState.step == ForgotPinViewModel.ForgotPinStep.SUCCESS) {
            delay(2000) // Show success message
            onNavigateToLogin()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.forgot_pin_title)) },
                navigationIcon = {
                    if (uiState.step != ForgotPinViewModel.ForgotPinStep.SUCCESS) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState.step) {
                ForgotPinViewModel.ForgotPinStep.PHONE_ENTRY -> {
                    PhoneEntryStep(
                        uiState = uiState,
                        viewModel = viewModel,
                        focusManager = focusManager
                    )
                }
                ForgotPinViewModel.ForgotPinStep.OTP_VERIFICATION -> {
                    OtpVerificationStep(
                        uiState = uiState,
                        viewModel = viewModel,
                        resendCountdown = resendCountdown,
                        onResendClick = {
                            resendCountdown = 60
                            viewModel.resendOtp()
                        }
                    )
                }
                ForgotPinViewModel.ForgotPinStep.PIN_ENTRY -> {
                    PinEntryStep(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
                ForgotPinViewModel.ForgotPinStep.PIN_CONFIRM -> {
                    PinConfirmStep(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
                ForgotPinViewModel.ForgotPinStep.SUCCESS -> {
                    SuccessStep()
                }
            }
        }
    }
}

@Composable
private fun PhoneEntryStep(
    uiState: ForgotPinViewModel.ForgotPinUiState,
    viewModel: ForgotPinViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Icon(
        imageVector = Icons.Default.Phone,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Text(
        text = stringResource(R.string.forgot_pin_enter_phone),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = stringResource(R.string.forgot_pin_whatsapp_info),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    // Country Code Selector
    CountryCodeSelector(
        selectedCountryCode = uiState.countryCode,
        onCountryCodeSelected = viewModel::updateCountryCode,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Phone Number Input
    MomoTextField(
        value = uiState.phoneNumber,
        onValueChange = viewModel::updatePhoneNumber,
        label = stringResource(R.string.auth_phone_number),
        placeholder = "201234567",
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                viewModel.sendOtp()
            }
        ),
        isError = uiState.phoneNumberError != null
    )
    
    if (uiState.phoneNumberError != null) {
        Text(
            text = uiState.phoneNumberError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp)
        )
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    
    // Send OTP Button
    MomoButton(
        text = stringResource(R.string.forgot_pin_send_code),
        onClick = { viewModel.sendOtp() },
        enabled = !uiState.isLoading && uiState.phoneNumber.isNotBlank(),
        isLoading = uiState.isLoading,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OtpVerificationStep(
    uiState: ForgotPinViewModel.ForgotPinUiState,
    viewModel: ForgotPinViewModel,
    resendCountdown: Int,
    onResendClick: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Phone,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Text(
        text = stringResource(R.string.forgot_pin_verify_code),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = stringResource(R.string.forgot_pin_otp_sent, uiState.countryCode, uiState.phoneNumber),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    // OTP Input
    OtpInputField(
        value = uiState.otpCode,
        onValueChange = { otp ->
            viewModel.updateOtpCode(otp)
            if (otp.length == 6) {
                viewModel.verifyOtp()
            }
        },
        digitCount = 6,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    // Resend OTP
    if (resendCountdown > 0) {
        Text(
            text = stringResource(R.string.reg_resend_countdown, resendCountdown),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        TextButton(onClick = onResendClick) {
            Text(stringResource(R.string.forgot_pin_resend_code))
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Verify Button
    MomoButton(
        text = stringResource(R.string.reg_verify),
        onClick = { viewModel.verifyOtp() },
        enabled = !uiState.isLoading && uiState.otpCode.length == 6,
        isLoading = uiState.isLoading,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PinEntryStep(
    uiState: ForgotPinViewModel.ForgotPinUiState,
    viewModel: ForgotPinViewModel
) {
    Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Text(
        text = stringResource(R.string.forgot_pin_create_new),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = stringResource(R.string.forgot_pin_4digit_instruction),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    // PIN Input
    OtpInputField(
        value = uiState.newPin,
        onValueChange = { pin ->
            viewModel.updateNewPin(pin)
            if (pin.length == 4) {
                viewModel.continueToConfirm()
            }
        },
        digitCount = 4,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    // Continue Button
    MomoButton(
        text = stringResource(R.string.reg_continue),
        onClick = { viewModel.continueToConfirm() },
        enabled = uiState.newPin.length == 4,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PinConfirmStep(
    uiState: ForgotPinViewModel.ForgotPinUiState,
    viewModel: ForgotPinViewModel
) {
    Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Text(
        text = stringResource(R.string.forgot_pin_confirm),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = stringResource(R.string.forgot_pin_confirm_instruction),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    // PIN Confirm Input
    OtpInputField(
        value = uiState.confirmPin,
        onValueChange = { pin ->
            viewModel.updateConfirmPin(pin)
            if (pin.length == 4) {
                viewModel.resetPin()
            }
        },
        digitCount = 4,
        modifier = Modifier.fillMaxWidth()
    )
    
    if (uiState.pinMismatch) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.forgot_pin_mismatch),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    
    // Reset PIN Button
    MomoButton(
        text = stringResource(R.string.forgot_pin_reset_button),
        onClick = { viewModel.resetPin() },
        enabled = !uiState.isLoading && uiState.confirmPin.length == 4,
        isLoading = uiState.isLoading,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SuccessStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = SuccessGreen
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.forgot_pin_success),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.forgot_pin_success_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPinScreenPreview() {
    MomoTerminalTheme {
        ForgotPinScreen(
            onNavigateBack = {},
            onNavigateToLogin = {}
        )
    }
}
