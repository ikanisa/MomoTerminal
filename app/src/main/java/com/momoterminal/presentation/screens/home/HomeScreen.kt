package com.momoterminal.presentation.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.R
import com.momoterminal.nfc.NfcState
import com.momoterminal.presentation.components.MomoButton
import com.momoterminal.presentation.components.ButtonType
import com.momoterminal.presentation.components.common.MomoTopAppBar
import com.momoterminal.presentation.components.terminal.AmountDisplay
import com.momoterminal.presentation.components.terminal.AmountKeypad
import com.momoterminal.presentation.components.terminal.NfcPulseAnimation
import com.momoterminal.presentation.theme.MomoAnimation
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MomoYellow

/**
 * Home screen with NFC payment terminal.
 * - Dynamic keypad (shows when tapping amount)
 * - NFC toggle switch
 * - No provider selector (determined by user's MoMo country)
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
                    AnimatedVisibility(
                        visible = isNfcActive,
                        enter = scaleIn(tween(MomoAnimation.DURATION_FAST)) + fadeIn(),
                        exit = scaleOut(tween(MomoAnimation.DURATION_FAST)) + fadeOut()
                    ) {
                        IconButton(onClick = { viewModel.cancelPayment() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !isNfcActive,
                        enter = fadeIn(tween(MomoAnimation.DURATION_FAST)),
                        exit = fadeOut(tween(MomoAnimation.DURATION_FAST))
                    ) {
                        Row {
                            IconButton(onClick = onNavigateToTransactions) {
                                Icon(Icons.Filled.History, contentDescription = stringResource(R.string.nav_history))
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.action_settings))
                            }
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
                transitionSpec = {
                    (fadeIn(tween(MomoAnimation.DURATION_MEDIUM)) + 
                        scaleIn(initialScale = 0.95f, animationSpec = tween(MomoAnimation.DURATION_MEDIUM)))
                        .togetherWith(
                            fadeOut(tween(MomoAnimation.DURATION_FAST)) + 
                            scaleOut(targetScale = 1.05f, animationSpec = tween(MomoAnimation.DURATION_FAST))
                        )
                },
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
                        onDigitClick = viewModel::onDigitClick,
                        onBackspaceClick = viewModel::onBackspaceClick,
                        onClearClick = viewModel::onClearClick,
                        onActivate = viewModel::activatePayment,
                        onToggleNfc = viewModel::toggleNfcEnabled,
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
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onActivate: () -> Unit,
    onToggleNfc: (Boolean) -> Unit,
    onNavigateToSettings: () -> Unit,
    isValid: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Configuration warning
        AnimatedVisibility(
            visible = !uiState.isConfigured,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(MomoAnimation.DURATION_MEDIUM, easing = MomoAnimation.EaseOutExpo)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(MomoAnimation.DURATION_FAST)
            ) + fadeOut()
        ) {
            ConfigurationBanner(
                onConfigureClick = onNavigateToSettings,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // NFC Toggle
        NfcToggleCard(
            isEnabled = uiState.isNfcEnabled,
            onToggle = onToggleNfc,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Amount display
        AmountDisplay(
            amount = uiState.amount,
            currency = uiState.currency,
            label = stringResource(R.string.amount_to_receive),
            isActive = uiState.amount.isNotEmpty()
        )

        // Provider info (from user's MoMo country)
        if (uiState.providerName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.providerName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Keypad - always visible
        AmountKeypad(
            onDigitClick = onDigitClick,
            onBackspaceClick = onBackspaceClick,
            onClearClick = onClearClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Activate NFC button
        val buttonScale by animateFloatAsState(
            targetValue = if (isValid && uiState.isNfcEnabled) 1f else 0.98f,
            animationSpec = tween(MomoAnimation.DURATION_MEDIUM),
            label = "buttonScale"
        )
        
        MomoButton(
            text = stringResource(R.string.activate_nfc),
            onClick = onActivate,
            enabled = isValid && uiState.isNfcEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .scale(buttonScale)
        )
    }
}

@Composable
private fun NfcToggleCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) 
                MomoYellow.copy(alpha = 0.15f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Nfc,
                contentDescription = null,
                tint = if (isEnabled) MomoYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.nfc_terminal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isEnabled) 
                        stringResource(R.string.nfc_ready) 
                    else 
                        stringResource(R.string.nfc_disabled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MomoYellow,
                    checkedTrackColor = MomoYellow.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun ConfigurationBanner(
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onConfigureClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.tap_to_configure),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        if (isSuccess) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        }
                    )
                )
            )
    ) {
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
                enter = fadeIn(tween(MomoAnimation.DURATION_MEDIUM)) + 
                    slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(MomoAnimation.DURATION_MEDIUM, easing = MomoAnimation.EaseOutExpo)
                    ),
                exit = fadeOut(tween(MomoAnimation.DURATION_FAST))
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
}

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
