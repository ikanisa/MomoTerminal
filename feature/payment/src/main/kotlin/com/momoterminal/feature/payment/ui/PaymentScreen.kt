package com.momoterminal.feature.payment.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.feature.payment.viewmodel.PaymentEvent
import com.momoterminal.feature.payment.viewmodel.PaymentViewModel

@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Payment",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = state.amount,
            onValueChange = { viewModel.onEvent(PaymentEvent.UpdateAmount(it)) },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isProcessing
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.onEvent(PaymentEvent.InitiatePayment) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isProcessing && state.amount.isNotEmpty()
        ) {
            Text("Pay")
        }

        if (state.isProcessing) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
