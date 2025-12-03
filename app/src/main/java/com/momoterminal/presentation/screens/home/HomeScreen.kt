package com.momoterminal.presentation.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.R
import com.momoterminal.nfc.NfcPaymentData
import com.momoterminal.nfc.NfcState
import com.momoterminal.presentation.components.MomoButton
import com.momoterminal.presentation.components.ButtonType
import com.momoterminal.presentation.components.common.MomoTopAppBar
import com.momoterminal.presentation.components.status.NfcStatusIndicator
import com.momoterminal.presentation.components.status.SyncStatusBadge
import com.momoterminal.presentation.components.terminal.AmountDisplay
import com.momoterminal.presentation.components.terminal.AmountKeypad
import com.momoterminal.presentation.components.terminal.NfcPulseAnimation
import com.momoterminal.presentation.components.terminal.ProviderSelector
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MomoYellow

/**
 * Unified Home screen with integrated payment terminal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTerminal: () -> Unit = {},
    onNavigateToTransactions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val nfcState by viewModel.nfcState.collectAsState()
    val isNfcActive = nfcState.isActive()
    val isSuccess = nfcState is NfcState.Success

    Scaffold(
        topBar = {
            MomoTopAppBar(
                title = stringResource(R.string.app_name),
                actions = {
                    if (isNfcActive) {
                        IconButton(onClick = { viewModel.cancelPayment() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        IconButton(onClick = onNavigateToTransactions) {
                            Icon(Icons.Filled.History, contentDescription = stringResource(R.string.nav_history))
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.action_settings))
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
                label = "home_content"
            ) { showNfcMode ->
                if (showNfcMode) {
                    NfcActiveContent(
                        nfcState = nfcState,
                        amount = uiState.amount,
                        currency = uiState.currency,
                        onCancel = { viewModel.cancelPayment() }
                    )
                } else {
                    PaymentInputContent(
                        uiState = uiState,
                        nfcState = nfcState,
                        onDigitClick = viewModel::onDigitClick,
                        onBackspaceClick = viewModel::onBackspaceClick,
                        onClearClick = viewModel::onClearClick,
                        onProviderSelected = viewModel::onProviderSelected,
                        onActivate = viewModel::activatePayment,
                        onNavigateToSettings = onNavigateToSettings,
                        isValid = viewModel.isAmountValid() && viewModel.isNfcAvailable()
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentInputContent(
    uiState: HomeViewModel.HomeUiState,
    nfcState: NfcState,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onProviderSelected: (NfcPaymentData.Provider) -> Unit,
    onActivate: () -> Unit,
    onNavigateToSettings: () -> Unit,
    isValid: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Configuration warning
        if (!uiState.isConfigured) {
            ConfigurationBanner(onConfigureClick = onNavigateToSettings)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Amount display
        AmountDisplay(
            amount = uiState.amount,
            currency = uiState.currency,
            label = stringResource(R.string.amount_to_receive),
            isActive = uiState.amount.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        // Activate NFC button
        MomoButton(
            text = stringResource(R.string.activate_nfc),
            onClick = onActivate,
            enabled = isValid,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TodaySummaryRow(
    revenue: Double,
    count: Int,
    currency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.today),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$currency ${formatAmount(revenue)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "$count ${stringResource(R.string.transactions).lowercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConfigurationBanner(onConfigureClick: () -> Unit) {
    Card(
        onClick = onConfigureClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.tap_to_configure),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NfcActiveContent(
    nfcState: NfcState,
    amount: String,
    currency: String,
    onCancel: () -> Unit
) {
    val isSuccess = nfcState is NfcState.Success
    val message = nfcState.getDisplayMessage()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AmountDisplay(
            amount = amount,
            currency = currency,
            label = if (isSuccess) stringResource(R.string.payment_received) else stringResource(R.string.amount),
            isActive = true
        )

        Spacer(modifier = Modifier.height(48.dp))

        NfcPulseAnimation(
            isActive = nfcState.isActive(),
            isSuccess = isSuccess,
            message = message
        )

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(
            visible = !isSuccess,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            MomoButton(
                text = stringResource(R.string.cancel_payment),
                onClick = onCancel,
                type = ButtonType.OUTLINE,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun formatAmount(amount: Double): String = String.format("%,.0f", amount)

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MomoTerminalTheme {
        HomeScreen(
            onNavigateToTransactions = {},
            onNavigateToSettings = {}
        )
    }
}
