package com.momoterminal.presentation.components.error

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momoterminal.core.error.MomoError
import com.momoterminal.core.ui.ErrorAction
import com.momoterminal.core.ui.SettingsType

@Composable
fun ErrorView(
    error: MomoError,
    onAction: (ErrorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (error is MomoError.NetworkUnavailable) Icons.Default.WifiOff else Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(error.message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        
        if (error.isRecoverable) {
            Button(onClick = { onAction(ErrorAction.Retry) }) {
                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
        if (error.requiresUserAction) {
            OutlinedButton(onClick = { 
                onAction(ErrorAction.OpenSettings(
                    when (error) {
                        is MomoError.NfcDisabled -> SettingsType.NFC_SETTINGS
                        is MomoError.PermissionDenied -> SettingsType.APP_PERMISSIONS
                        else -> SettingsType.NETWORK_SETTINGS
                    }
                ))
            }) {
                Text("Open Settings")
            }
        }
    }
}
