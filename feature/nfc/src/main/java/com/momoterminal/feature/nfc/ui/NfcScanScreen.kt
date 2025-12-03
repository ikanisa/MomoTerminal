package com.momoterminal.feature.nfc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(MomoTheme.spacing.md),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Placeholder for Lottie Animation
            Text(
                text = "NFC Animation Here",
                style = MaterialTheme.typography.headlineMedium,
                color = MomoTheme.colors.tokenAccent
            )
            
            Spacer(modifier = Modifier.height(MomoTheme.spacing.lg))
            
            Text(
                text = when (nfcState) {
                    is NfcState.Ready -> "Ready to Scan"
                    is NfcState.Activating -> "Activating..."
                    is NfcState.Active -> "Hold device near reader"
                    is NfcState.Processing -> "Processing..."
                    is NfcState.Success -> "Success!"
                    is NfcState.Error -> "Error"
                    is NfcState.Timeout -> "Timeout"
                    is NfcState.NotSupported -> "NFC Not Supported"
                    is NfcState.Disabled -> "NFC Disabled"
                },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (nfcState is NfcState.Error) {
                val errorState = nfcState as NfcState.Error
                Spacer(modifier = Modifier.height(MomoTheme.spacing.sm))
                Text(
                    text = errorState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
