package com.momoterminal.presentation.screens.wallet

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.presentation.components.MomoButton
import com.momoterminal.presentation.components.MomoTextField
import com.momoterminal.presentation.theme.MomoYellow
import com.momoterminal.presentation.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onNavigateToTransactions: () -> Unit = {},
    viewModel: WalletViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showTopUpDialog by remember { mutableStateOf(false) }
    var topUpAmount by remember { mutableStateOf("") }
    
    val animatedBalance by animateFloatAsState(
        targetValue = uiState.balance.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "balance"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Wallet", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showTopUpDialog = true },
                icon = { Icon(Icons.Filled.Add, "Top Up") },
                text = { Text("Top Up") },
                containerColor = MomoYellow,
                contentColor = Color.Black
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            WalletBalanceCard(
                balance = animatedBalance.toInt().toLong(),
                currency = uiState.currency,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            QuickActionsRow(
                onTopUp = { showTopUpDialog = true },
                onHistory = onNavigateToTransactions,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (uiState.recentTransactions.isEmpty()) {
                EmptyTransactionsView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.recentTransactions) { transaction ->
                        TransactionItem(transaction)
                    }
                }
            }
        }
    }
    
    if (showTopUpDialog) {
        TopUpDialog(
            currentAmount = topUpAmount,
            onAmountChange = { topUpAmount = it },
            onDismiss = { showTopUpDialog = false; topUpAmount = "" },
            onTopUp = { amount ->
                viewModel.initiateTopUp(amount)
                val ussdCode = viewModel.generateTopUpUssd(amount)
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(ussdCode)}"))
                context.startActivity(intent)
                showTopUpDialog = false
                topUpAmount = ""
            }
        )
    }
}

@Composable
private fun WalletBalanceCard(
    balance: Long,
    currency: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MomoYellow.copy(alpha = shimmerAlpha),
                            MomoYellow.copy(alpha = shimmerAlpha * 0.7f),
                            MomoYellow.copy(alpha = shimmerAlpha)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        tint = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Available Balance",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$currency ",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Black.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format("%,d", balance),
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "• Use for in-app services",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onTopUp: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(
            icon = Icons.Filled.AddCircle,
            label = "Top Up",
            onClick = onTopUp,
            modifier = Modifier.weight(1f),
            containerColor = SuccessGreen
        )
        QuickActionButton(
            icon = Icons.Filled.History,
            label = "History",
            onClick = onHistory,
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    Card(
        modifier = modifier.scale(scale).clickable { pressed = true; onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, label, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
    
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}

@Composable
private fun TopUpDialog(
    currentAmount: String,
    onAmountChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onTopUp: (Long) -> Unit
) {
    val amount = currentAmount.toLongOrNull() ?: 0
    val isValid = amount in 100..4000
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AddCircle, null, tint = SuccessGreen)
                Spacer(Modifier.width(8.dp))
                Text("Top Up Wallet")
            }
        },
        text = {
            Column {
                Text("Add money to your wallet via Mobile Money", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                MomoTextField(
                    value = currentAmount,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) onAmountChange(it) },
                    label = "Amount (FRW)",
                    placeholder = "Enter amount",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = currentAmount.isNotEmpty() && !isValid
                )
                if (currentAmount.isNotEmpty() && !isValid) {
                    Text(
                        if (amount < 100) "Minimum: 100 FRW" else "Maximum: 4,000 FRW",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("Quick Select:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(500L, 1000L, 2000L, 4000L).forEach { quickAmount ->
                        QuickAmountChip(quickAmount, amount == quickAmount, { onAmountChange(quickAmount.toString()) }, Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("How it works:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("1. Enter amount (100-4,000 FRW)\n2. Tap 'Proceed'\n3. Enter PIN on USSD screen\n4. Wallet credited instantly", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            MomoButton(text = "Proceed to Pay", onClick = { if (isValid) onTopUp(amount) }, enabled = isValid)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun QuickAmountChip(amount: Long, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor by animateColorAsState(if (isSelected) MomoYellow else MaterialTheme.colorScheme.surfaceVariant, label = "bg")
    val contentColor by animateColorAsState(if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant, label = "content")
    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(backgroundColor).border(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) Color.Black else MaterialTheme.colorScheme.outline,
            RoundedCornerShape(8.dp)
        ).clickable(onClick = onClick).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("${amount / 1000}K", style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = contentColor)
    }
}

@Composable
private fun TransactionItem(transaction: WalletTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(
                        if (transaction.type == TransactionType.TOP_UP) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (transaction.type == TransactionType.TOP_UP) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                        null,
                        tint = if (transaction.type == TransactionType.TOP_UP) SuccessGreen else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(transaction.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(transaction.timestamp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "${if (transaction.type == TransactionType.TOP_UP) "+" else "-"}${transaction.amount} FRW",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == TransactionType.TOP_UP) SuccessGreen else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EmptyTransactionsView() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.AccountBalanceWallet, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))
            Text("No transactions yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("Top up your wallet to get started", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}
