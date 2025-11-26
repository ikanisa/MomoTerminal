package com.momoterminal.presentation.components.error

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.momoterminal.error.AppError
import com.momoterminal.error.ErrorAction
import com.momoterminal.error.ErrorEvent
import kotlinx.coroutines.flow.SharedFlow

/**
 * Snackbar host that displays error events.
 */
@Composable
fun ErrorSnackbarHost(
    errorEvents: SharedFlow<ErrorEvent>,
    onAction: (ErrorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        errorEvents.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.error.message,
                actionLabel = event.actionLabel,
                duration = if (event.error.isRecoverable) {
                    SnackbarDuration.Long
                } else {
                    SnackbarDuration.Short
                }
            )
            
            if (result == SnackbarResult.ActionPerformed && event.action != null) {
                onAction(event.action)
            }
        }
    }
    
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    ) { snackbarData ->
        ErrorSnackbar(snackbarData = snackbarData)
    }
}

/**
 * Custom styled snackbar for errors.
 */
@Composable
fun ErrorSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
) {
    Snackbar(
        modifier = modifier,
        action = snackbarData.visuals.actionLabel?.let {
            {
                TextButton(onClick = { snackbarData.performAction() }) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            }
        },
        dismissAction = {
            TextButton(onClick = { snackbarData.dismiss() }) {
                Text(
                    text = "Dismiss",
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(text = snackbarData.visuals.message)
    }
}

/**
 * Simple error snackbar with message and optional action.
 */
@Composable
fun SimpleErrorSnackbar(
    error: AppError,
    onActionClick: (() -> Unit)? = null,
    actionLabel: String? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Snackbar(
        modifier = modifier,
        action = if (actionLabel != null && onActionClick != null) {
            {
                TextButton(onClick = onActionClick) {
                    Text(
                        text = actionLabel,
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            }
        } else null,
        dismissAction = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(text = error.message)
    }
}
