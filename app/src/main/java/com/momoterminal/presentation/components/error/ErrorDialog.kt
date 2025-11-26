package com.momoterminal.presentation.components.error

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.momoterminal.error.AppError
import com.momoterminal.error.ErrorAction

/**
 * Alert dialog for displaying critical errors.
 */
@Composable
fun ErrorDialog(
    error: AppError,
    onDismiss: () -> Unit,
    onAction: ((ErrorAction) -> Unit)? = null,
    action: ErrorAction? = null,
    actionLabel: String? = null,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = getErrorTitle(error),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            if (action != null && actionLabel != null && onAction != null) {
                TextButton(onClick = { onAction(action) }) {
                    Text(actionLabel)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = if (action != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        } else null,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Get a title for the error dialog based on error type.
 */
private fun getErrorTitle(error: AppError): String {
    return when (error) {
        is AppError.Network -> "Network Error"
        is AppError.Api -> "Server Error"
        is AppError.Nfc -> "NFC Error"
        is AppError.Sms -> "SMS Error"
        is AppError.Biometric -> "Authentication Error"
        is AppError.Database -> "Storage Error"
        is AppError.Validation -> "Validation Error"
        is AppError.Security -> "Security Error"
        is AppError.Unknown -> "Error"
    }
}

/**
 * Confirmation dialog for destructive actions.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        }
    )
}
