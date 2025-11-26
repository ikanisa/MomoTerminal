package com.momoterminal.presentation.components.error

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momoterminal.error.AppError
import com.momoterminal.error.ErrorAction
import com.momoterminal.error.getTitle
import com.momoterminal.presentation.components.animations.PaymentErrorAnimation

/**
 * Full-screen error display with retry button.
 */
@Composable
fun ErrorScreen(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    onAction: ((ErrorAction) -> Unit)? = null,
    action: ErrorAction? = null,
    actionLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val accessibilityDescription = "Error: ${error.message}" + 
        if (error.isRecoverable) ". Tap retry to try again." else ""
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .semantics {
                contentDescription = accessibilityDescription
                liveRegion = LiveRegionMode.Assertive
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error animation
        PaymentErrorAnimation(
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Error title
        Text(
            text = error.getTitle(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Error message
        Text(
            text = error.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action buttons
        if (error.isRecoverable && onRetry != null) {
            Button(
                onClick = onRetry,
                modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp)
            ) {
                Text("Retry")
            }
        }
        
        if (action != null && actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onAction(action) },
                modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * Compact error card for inline display.
 */
@Composable
fun ErrorCard(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .semantics {
                contentDescription = "Error: ${error.message}"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = error.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        
        if (error.isRecoverable && onRetry != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.sizeIn(minHeight = 48.dp)
            ) {
                Text("Retry")
            }
        }
    }
}
