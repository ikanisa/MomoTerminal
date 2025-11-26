package com.momoterminal.presentation.screens.terminal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.nfc.NfcState
import com.momoterminal.presentation.components.common.MomoCenterTopAppBar
import com.momoterminal.presentation.components.terminal.AmountDisplay
import com.momoterminal.presentation.components.terminal.AmountKeypad
import com.momoterminal.presentation.components.terminal.NfcPulseAnimation
import com.momoterminal.presentation.components.terminal.ProviderSelector
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MomoYellow

/**
 * Terminal screen for NFC payment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onNavigateBack: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val nfcState by viewModel.nfcState.collectAsState()
    
    val isNfcActive = nfcState.isActive()
    val isSuccess = nfcState is NfcState.Success
    
    Scaffold(
        topBar = {
            MomoCenterTopAppBar(
                title = "Payment Terminal",
                navigationIcon = if (!isNfcActive) Icons.AutoMirrored.Filled.ArrowBack else null,
                onNavigationClick = if (!isNfcActive) onNavigateBack else null,
                actions = {
                    if (isNfcActive) {
                        IconButton(onClick = { viewModel.cancelPayment() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = isNfcActive || isSuccess,
                label = "terminal_content"
            ) { showNfcMode ->
                if (showNfcMode) {
                    // NFC Active Mode
                    NfcActiveContent(
                        nfcState = nfcState,
                        amount = uiState.amount,
                        onCancel = { viewModel.cancelPayment() }
                    )
                } else {
                    // Input Mode
                    InputContent(
                        uiState = uiState,
                        onDigitClick = viewModel::onDigitClick,
                        onBackspaceClick = viewModel::onBackspaceClick,
                        onClearClick = viewModel::onClearClick,
                        onProviderSelected = viewModel::onProviderSelected,
                        onActivate = viewModel::activatePayment,
                        isValid = viewModel.isAmountValid() && viewModel.isNfcAvailable()
                    )
                }
            }
        }
    }
}

@Composable
private fun InputContent(
    uiState: TerminalViewModel.TerminalUiState,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onProviderSelected: (com.momoterminal.nfc.NfcPaymentData.Provider) -> Unit,
    onActivate: () -> Unit,
    isValid: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Amount display
        AmountDisplay(
            amount = uiState.amount,
            label = "Amount to Receive",
            isActive = uiState.amount.isNotEmpty()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Provider selector
        ProviderSelector(
            selectedProvider = uiState.selectedProvider,
            onProviderSelected = onProviderSelected,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Keypad
        AmountKeypad(
            onDigitClick = onDigitClick,
            onBackspaceClick = onBackspaceClick,
            onClearClick = onClearClick,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Activate button
        Button(
            onClick = onActivate,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = MomoYellow,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Activate NFC",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        if (!uiState.isConfigured) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please configure merchant details in Settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun NfcActiveContent(
    nfcState: NfcState,
    amount: String,
    onCancel: () -> Unit
) {
    val isSuccess = nfcState is NfcState.Success
    val message = nfcState.getDisplayMessage()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Amount display
        AmountDisplay(
            amount = amount,
            label = if (isSuccess) "Payment Received" else "Amount",
            isActive = true
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // NFC animation
        NfcPulseAnimation(
            isActive = nfcState.isActive(),
            isSuccess = isSuccess,
            message = message
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Cancel button (hidden during success)
        AnimatedVisibility(
            visible = !isSuccess,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "Cancel Payment",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TerminalScreenPreview() {
    MomoTerminalTheme {
        TerminalScreen(
            onNavigateBack = {}
        )
    }
}
