package com.momoterminal.presentation.components.error

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.components.MomoButton
import com.momoterminal.presentation.components.ButtonType

/**
 * Reusable error state component with retry functionality.
 * Use this instead of creating custom error UIs throughout the app.
 */
@Composable
fun ErrorStateView(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    errorType: ErrorType = ErrorType.NETWORK,
    showIcon: Boolean = true
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)) + slideInVertically(),
        exit = fadeOut(tween(300)) + slideOutVertically()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showIcon) {
                Icon(
                    imageVector = errorType.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text(
                text = errorType.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            MomoButton(
                text = "Try Again",
                onClick = onRetry,
                type = ButtonType.PRIMARY,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }
    }
}

/**
 * Types of errors with appropriate icons and titles.
 */
enum class ErrorType(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String
) {
    NETWORK(
        icon = Icons.Filled.CloudOff,
        title = "Connection Error"
    ),
    SERVER(
        icon = Icons.Filled.ErrorOutline,
        title = "Server Error"
    ),
    NOT_FOUND(
        icon = Icons.Filled.SearchOff,
        title = "Not Found"
    ),
    PERMISSION(
        icon = Icons.Filled.Lock,
        title = "Permission Denied"
    ),
    VALIDATION(
        icon = Icons.Filled.Warning,
        title = "Invalid Input"
    ),
    GENERIC(
        icon = Icons.Filled.Error,
        title = "Something Went Wrong"
    )
}

/**
 * Inline error message (for smaller spaces like forms).
 */
@Composable
fun InlineError(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Error snackbar with action.
 */
@Composable
fun ErrorSnackbar(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    ) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            actionColor = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Helper to show error in SnackbarHostState.
 */
suspend fun SnackbarHostState.showError(
    message: String,
    actionLabel: String? = "Dismiss",
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult {
    return showSnackbar(
        message = message,
        actionLabel = actionLabel,
        duration = duration
    )
}
