package com.momoterminal.feature.nfc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.core.designsystem.theme.MomoTheme
import com.momoterminal.feature.nfc.NfcState
import com.momoterminal.feature.nfc.NfcViewModel

@Composable
fun NfcScanScreen(
    viewModel: NfcViewModel = hiltViewModel(),
    onScanSuccess: (String) -> Unit,
    onScanError: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val nfcState by viewModel.nfcState.collectAsState()
    var amountInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MomoTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.md)
    ) {
        // Header
        Text(
            text = "NFC Payment",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(MomoTheme.spacing.md))
        
        // Amount Input Card
        if (nfcState is NfcState.Ready || nfcState is NfcState.Disabled || nfcState is NfcState.NotSupported) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(MomoTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)
                ) {
                    Text(
                        text = "Enter Amount",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { char -> char.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Amount (RWF)") },
                        placeholder = { Text("1000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = nfcState is NfcState.Ready,
                        isError = amountInput.isEmpty() && amountInput.isNotBlank()
                    )
                    
                    Text(
                        text = "Payer will tap their phone to complete USSD payment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(MomoTheme.spacing.lg))
            
            // Activate NFC Button
            Button(
                onClick = {
                    val amount = amountInput.toLongOrNull()
                    if (amount != null && amount > 0) {
                        viewModel.activateNfcPayment(amount)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = amountInput.isNotEmpty() && amountInput.toLongOrNull() != null && nfcState is NfcState.Ready,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MomoTheme.colors.tokenAccent
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = "Activate NFC",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(MomoTheme.spacing.sm))
                Text(
                    text = "Activate NFC Writer",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // NFC Status Messages
            when (nfcState) {
                is NfcState.NotSupported -> {
                    Text(
                        text = "⚠️ NFC is not supported on this device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(MomoTheme.spacing.md)
                    )
                }
                is NfcState.Disabled -> {
                    Text(
                        text = "⚠️ Please enable NFC in device settings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(MomoTheme.spacing.md)
                    )
                }
                else -> {}
            }
        }
        
        // Active NFC State Display
        if (nfcState is NfcState.Active || nfcState is NfcState.Activating || nfcState is NfcState.Processing) {
            Spacer(modifier = Modifier.weight(1f))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MomoTheme.colors.tokenAccent.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MomoTheme.spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.md)
                ) {
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = "NFC Active",
                        modifier = Modifier.size(64.dp),
                        tint = MomoTheme.colors.tokenAccent
                    )
                    
                    if (nfcState is NfcState.Processing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MomoTheme.colors.tokenAccent
                        )
                    }
                    
                    Text(
                        text = when (nfcState) {
                            is NfcState.Activating -> "Activating NFC..."
                            is NfcState.Active -> "✓ NFC Active\n\nAsk payer to tap their phone"
                            is NfcState.Processing -> "Processing payment..."
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = MomoTheme.colors.tokenAccent
                    )
                    
                    if (nfcState is NfcState.Active) {
                        val paymentData = (nfcState as NfcState.Active).paymentData
                        Text(
                            text = "Amount: ${paymentData.getWholeAmount()} ${paymentData.currency}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            OutlinedButton(
                onClick = {
                    viewModel.onScanCancelled()
                    amountInput = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
        
        // Success State
        if (nfcState is NfcState.Success) {
            val successState = nfcState as NfcState.Success
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MomoTheme.spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✓ Success!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Transaction: ${successState.transactionId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                onScanSuccess(successState.transactionId)
                amountInput = ""
            }
        }
        
        // Error State
        if (nfcState is NfcState.Error) {
            val errorState = nfcState as NfcState.Error
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MomoTheme.spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "❌ Error",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                onScanError(errorState.message)
            }
        }
        
        // Timeout State
        if (nfcState is NfcState.Timeout) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "⏱ Timeout - Please try again",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MomoTheme.spacing.md)
                )
            }
        }
    }
}
