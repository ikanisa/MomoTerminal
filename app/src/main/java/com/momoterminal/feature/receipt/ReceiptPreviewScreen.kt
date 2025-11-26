package com.momoterminal.feature.receipt

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File

/**
 * Receipt preview screen composable showing receipt details
 * with share and download functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPreviewScreen(
    receiptData: ReceiptData,
    pdfReceiptGenerator: PdfReceiptGenerator,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var isGenerating by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receipt") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ReceiptActionButtons(
                isGenerating = isGenerating,
                onShare = {
                    scope.launch {
                        isGenerating = true
                        try {
                            val file = generatedFile ?: pdfReceiptGenerator.generateReceipt(receiptData)
                            generatedFile = file
                            
                            val shareIntent = pdfReceiptGenerator.shareReceipt(file)
                            context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "Share Receipt")
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Failed to generate receipt: ${e.message}")
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                onDownload = {
                    scope.launch {
                        isGenerating = true
                        try {
                            val file = generatedFile ?: pdfReceiptGenerator.generateReceipt(receiptData)
                            generatedFile = file
                            snackbarHostState.showSnackbar("Receipt saved to ${file.name}")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Failed to generate receipt: ${e.message}")
                        } finally {
                            isGenerating = false
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status indicator
            StatusIndicator(status = receiptData.status)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Amount
            Text(
                text = receiptData.formattedAmount,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = receiptData.typeDisplayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Transaction details card
            TransactionDetailsCard(receiptData = receiptData)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Party information card
            PartyInfoCard(receiptData = receiptData)
            
            // Amount breakdown card
            if (receiptData.fee > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                AmountBreakdownCard(receiptData = receiptData)
            }
            
            // Merchant info card
            receiptData.merchantInfo?.let { merchant ->
                Spacer(modifier = Modifier.height(16.dp))
                MerchantInfoCard(merchantInfo = merchant)
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Space for bottom buttons
        }
    }
}

/**
 * Status indicator with icon and color.
 */
@Composable
private fun StatusIndicator(
    status: TransactionStatus,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = when (status) {
        TransactionStatus.SUCCESS -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF4CAF50),
            "Successful"
        )
        TransactionStatus.PENDING -> Triple(
            Icons.Default.Pending,
            Color(0xFFFF9800),
            "Pending"
        )
        TransactionStatus.FAILED -> Triple(
            Icons.Default.Error,
            Color(0xFFF44336),
            "Failed"
        )
        TransactionStatus.CANCELLED -> Triple(
            Icons.Default.Error,
            Color(0xFF9E9E9E),
            "Cancelled"
        )
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(40.dp),
                tint = color
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Card showing transaction details.
 */
@Composable
private fun TransactionDetailsCard(
    receiptData: ReceiptData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Transaction Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DetailRow(label = "Transaction ID", value = receiptData.transactionId)
            DetailRow(label = "Reference", value = receiptData.referenceNumber)
            DetailRow(label = "Date", value = receiptData.formattedDate)
            DetailRow(label = "Time", value = receiptData.formattedTime)
            
            receiptData.description?.let {
                DetailRow(label = "Description", value = it)
            }
        }
    }
}

/**
 * Card showing sender and recipient information.
 */
@Composable
private fun PartyInfoCard(
    receiptData: ReceiptData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sender",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            DetailRow(label = "Name", value = receiptData.senderName)
            DetailRow(label = "Phone", value = receiptData.senderPhone)
            
            receiptData.recipientName?.let { name ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                Text(
                    text = "Recipient",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                DetailRow(label = "Name", value = name)
                receiptData.recipientPhone?.let { phone ->
                    DetailRow(label = "Phone", value = phone)
                }
            }
        }
    }
}

/**
 * Card showing amount breakdown.
 */
@Composable
private fun AmountBreakdownCard(
    receiptData: ReceiptData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Amount Breakdown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DetailRow(label = "Amount", value = receiptData.formattedAmount)
            DetailRow(label = "Fee", value = receiptData.formattedFee)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            DetailRow(
                label = "Total",
                value = receiptData.formattedTotal,
                isBold = true
            )
        }
    }
}

/**
 * Card showing merchant information.
 */
@Composable
private fun MerchantInfoCard(
    merchantInfo: MerchantInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Merchant Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DetailRow(label = "Name", value = merchantInfo.name)
            DetailRow(label = "Code", value = merchantInfo.code)
            
            merchantInfo.address?.let {
                DetailRow(label = "Address", value = it)
            }
            
            merchantInfo.phone?.let {
                DetailRow(label = "Phone", value = it)
            }
        }
    }
}

/**
 * Row showing a label and value.
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

/**
 * Bottom action buttons for share and download.
 */
@Composable
private fun ReceiptActionButtons(
    isGenerating: Boolean,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDownload,
            enabled = !isGenerating,
            modifier = Modifier.weight(1f)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text("Download")
        }
        
        Button(
            onClick = onShare,
            enabled = !isGenerating,
            modifier = Modifier.weight(1f)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text("Share")
        }
    }
}
