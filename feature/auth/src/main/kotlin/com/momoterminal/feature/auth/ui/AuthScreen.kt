package com.momoterminal.feature.auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.feature.auth.viewmodel.AuthEffect
import com.momoterminal.feature.auth.viewmodel.AuthEvent
import com.momoterminal.feature.auth.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var phone by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                AuthEffect.NavigateToHome -> onNavigateToHome()
                AuthEffect.OtpSent -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sign In",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.otpSent) {
            OutlinedTextField(
                value = otpCode,
                onValueChange = { otpCode = it },
                label = { Text("OTP Code") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.onEvent(AuthEvent.VerifyOtp(phone, otpCode)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && otpCode.isNotEmpty()
            ) {
                Text("Verify OTP")
            }
        } else {
            Button(
                onClick = { viewModel.onEvent(AuthEvent.SendOtp(phone)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && phone.isNotEmpty()
            ) {
                Text("Send OTP")
            }
        }

        if (state.isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
