package com.momoterminal.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Empty state component for displaying when no content is available.
 * 
 * Provides a consistent, user-friendly UI for various empty states:
 * - No transactions yet
 * - No search results
 * - Network errors
 * - Filtered results with no matches
 * 
 * Usage:
 * ```kotlin
 * if (transactions.isEmpty()) {
 *     EmptyState(
 *         icon = Icons.Outlined.Receipt,
 *         title = "No transactions yet",
 *         subtitle = "Tap 'Terminal' to accept your first payment",
 *         actionLabel = "Go to Terminal",
 *         onActionClick = { navController.navigate("terminal") }
 *     )
 * }
 * ```
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Subtitle
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // Actions
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onActionClick,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(text = actionLabel)
            }
        }
        
        if (secondaryActionLabel != null && onSecondaryActionClick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(
                onClick = onSecondaryActionClick
            ) {
                Text(text = secondaryActionLabel)
            }
        }
    }
}

/**
 * Pre-configured empty state for no transactions.
 */
@Composable
fun NoTransactionsEmptyState(
    onNavigateToTerminal: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Outlined.Receipt,
        title = "No transactions yet",
        subtitle = "Start accepting payments by tapping the Terminal tab below",
        actionLabel = "Go to Terminal",
        onActionClick = onNavigateToTerminal,
        modifier = modifier
    )
}

/**
 * Pre-configured empty state for no search results.
 */
@Composable
fun NoSearchResultsEmptyState(
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Outlined.Search,
        title = "No matching transactions",
        subtitle = "Try adjusting your search filters or date range",
        actionLabel = "Clear Filters",
        onActionClick = onClearFilters,
        modifier = modifier
    )
}

/**
 * Pre-configured empty state for network errors.
 */
@Composable
fun NetworkErrorEmptyState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Outlined.CloudOff,
        title = "Connection issue",
        subtitle = "Check your internet connection and try again",
        actionLabel = "Retry",
        onActionClick = onRetry,
        modifier = modifier
    )
}
