package com.momoterminal.presentation.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.theme.MomoTerminalTheme

/**
 * Error dialog with icon, message, and action buttons.
 */
@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Error,
    confirmButtonText: String = "OK",
    onConfirm: (() -> Unit)? = null,
    dismissButtonText: String? = null,
    onDismissButton: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm?.invoke() ?: onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            if (dismissButtonText != null) {
                OutlinedButton(
                    onClick = { 
                        onDismissButton?.invoke() ?: onDismiss()
                    }
                ) {
                    Text(dismissButtonText)
                }
            }
        }
    )
}

/**
 * Warning dialog for non-critical issues.
 */
@Composable
fun WarningDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmButtonText: String = "Continue",
    onConfirm: (() -> Unit)? = null,
    dismissButtonText: String = "Cancel",
    onDismissButton: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm?.invoke() ?: onDismiss()
                }
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { 
                    onDismissButton?.invoke() ?: onDismiss()
                }
            ) {
                Text(dismissButtonText)
            }
        }
    )
}

@Preview
@Composable
private fun ErrorDialogPreview() {
    MomoTerminalTheme {
        ErrorDialog(
            title = "Payment Failed",
            message = "Unable to process the payment. Please check your connection and try again.",
            onDismiss = {},
            dismissButtonText = "Cancel"
        )
    }
}

@Preview
@Composable
private fun WarningDialogPreview() {
    MomoTerminalTheme {
        WarningDialog(
            title = "NFC Disabled",
            message = "NFC is currently disabled. Would you like to enable it in settings?",
            onDismiss = {}
        )
    }
}
