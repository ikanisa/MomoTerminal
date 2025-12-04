package com.momoterminal.presentation.screens.transaction

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.R
import com.momoterminal.core.database.entity.TransactionEntity
import com.momoterminal.domain.model.Provider
import com.momoterminal.presentation.components.MomoButton
import com.momoterminal.presentation.components.ButtonType
import com.momoterminal.presentation.components.common.MomoTopAppBar
import com.momoterminal.util.toRelativeTime

/**
 * Transaction Detail screen showing full transaction information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.showCopiedMessage) {
        if (uiState.showCopiedMessage) {
            snackbarHostState.showSnackbar("Transaction ID copied")
        }
    }
    
    Scaffold(
        topBar = {
            MomoTopAppBar(
                title = stringResource(R.string.transaction_details),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, viewModel.getShareableText())
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Transaction"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.transaction == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.transaction_not_found),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MomoButton(
                            text = stringResource(R.string.go_back),
                            onClick = onNavigateBack
                        )
                    }
                }
                
                else -> {
                    TransactionDetailContent(
                        transaction = uiState.transaction!!,
                        provider = uiState.provider,
                        isRawMessageExpanded = uiState.isRawMessageExpanded,
                        isSyncing = uiState.isSyncing,
                        syncError = uiState.syncError,
                        onToggleRawMessage = viewModel::toggleRawMessageExpanded,
                        onRetrySync = viewModel::retrySync,
                        onCopyTransactionId = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Transaction ID", it)
                            clipboard.setPrimaryClip(clip)
                            viewModel.onTransactionIdCopied()
                        },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, viewModel.getShareableText())
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Transaction"))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailContent(
    transaction: TransactionEntity,
    provider: Provider?,
    isRawMessageExpanded: Boolean,
    isSyncing: Boolean,
    syncError: String?,
    onToggleRawMessage: () -> Unit,
    onRetrySync: () -> Unit,
    onCopyTransactionId: (String) -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Amount card
        AmountCard(
            amount = transaction.amount,
            currency = transaction.currency ?: "GHS",
            provider = provider
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status card
        StatusCard(
            status = transaction.status,
            isSyncing = isSyncing,
            syncError = syncError,
            onRetrySync = onRetrySync
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Details card
        DetailsCard(
            transaction = transaction,
            provider = provider,
            onCopyTransactionId = onCopyTransactionId
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Raw message card
        RawMessageCard(
            message = transaction.body,
            isExpanded = isRawMessageExpanded,
            onToggle = onToggleRawMessage
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        ActionButtons(
            transactionId = transaction.transactionId,
            showRetrySync = transaction.status == "FAILED",
            onCopyTransactionId = onCopyTransactionId,
            onShare = onShare,
            onRetrySync = onRetrySync
        )
    }
}

@Composable
private fun AmountCard(
    amount: Double?,
    currency: String,
    provider: Provider?
) {
    val providerColor = when (provider) {
        Provider.MTN -> Color(0xFFFFCC00)
        Provider.AIRTEL -> Color(0xFFED1C24)
        Provider.TIGO -> Color(0xFF0066CC)
        Provider.VODACOM -> Color(0xFFE60000)
        Provider.HALOTEL -> Color(0xFF9C27B0)
        Provider.LUMICASH -> Color(0xFF4CAF50)
        Provider.ECOCASH -> Color(0xFFFF9800)
        Provider.VODAFONE -> Color(0xFFE60000)
        Provider.AIRTELTIGO -> Color(0xFFED1C24)
        null -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = providerColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            provider?.let {
                Text(
                    text = it.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = providerColor
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = if (amount != null) {
                    "$currency ${"%.2f".format(amount)}"
                } else {
                    "Amount not available"
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatusCard(
    status: String,
    isSyncing: Boolean,
    syncError: String?,
    onRetrySync: () -> Unit
) {
    val (statusIcon, statusColor, statusText) = when (status) {
        "PENDING" -> Triple(Icons.Default.Pending, Color(0xFFF59E0B), "Pending")
        "SENT", "SYNCED" -> Triple(Icons.Default.CheckCircle, Color(0xFF10B981), "Synced")
        "FAILED" -> Triple(Icons.Default.Error, Color(0xFFEF4444), "Failed")
        else -> Triple(Icons.Default.Pending, MaterialTheme.colorScheme.outline, status)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isSyncing) "Syncing..." else statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor
                )
                syncError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            if (status == "FAILED" && !isSyncing) {
                IconButton(onClick = onRetrySync) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry sync",
                        tint = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsCard(
    transaction: TransactionEntity,
    provider: Provider?,
    onCopyTransactionId: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.transaction_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Transaction ID
            transaction.transactionId?.let { txId ->
                DetailRow(
                    label = "Transaction ID",
                    value = txId,
                    onCopy = { onCopyTransactionId(txId) }
                )
            }
            
            // Sender
            DetailRow(label = "From", value = transaction.sender)
            
            // Provider
            provider?.let {
                DetailRow(label = "Provider", value = it.displayName)
            }
            
            // Merchant Code
            transaction.merchantCode?.let {
                DetailRow(label = "Merchant Code", value = it)
            }
            
            // Timestamp
            DetailRow(
                label = "Time",
                value = "${formatDate(transaction.timestamp)} (${transaction.timestamp.toRelativeTime()})"
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        onCopy?.let {
            IconButton(onClick = it) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RawMessageCard(
    message: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.raw_sms_message),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
            
            if (!isExpanded) {
                Text(
                    text = message.take(100) + if (message.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    transactionId: String?,
    showRetrySync: Boolean,
    onCopyTransactionId: (String) -> Unit,
    onShare: () -> Unit,
    onRetrySync: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            transactionId?.let { txId ->
                MomoButton(
                    text = stringResource(R.string.copy_id),
                    onClick = { onCopyTransactionId(txId) },
                    type = ButtonType.OUTLINE,
                    modifier = Modifier.weight(1f)
                )
            }
            
            MomoButton(
                text = stringResource(R.string.share),
                onClick = onShare,
                type = ButtonType.SECONDARY,
                modifier = Modifier.weight(1f)
            )
        }
        
        if (showRetrySync) {
            MomoButton(
                text = stringResource(R.string.retry_sync),
                onClick = onRetrySync,
                modifier = Modifier.fillMaxWidth(),
                // Note: MomoButton doesn't support custom colors directly yet, 
                // but primary (yellow) is fine for retry action in this context
            )
        }
    }
}

// Date formatter cached at top-level to avoid creating new instances on each call
private val dateFormatter by lazy {
    java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
}

@Synchronized
private fun formatDate(timestamp: Long): String {
    // SimpleDateFormat is not thread-safe, so we synchronize access
    return dateFormatter.format(java.util.Date(timestamp))
}
