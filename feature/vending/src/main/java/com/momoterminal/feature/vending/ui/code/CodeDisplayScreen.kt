package com.momoterminal.feature.vending.ui.code

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momoterminal.core.designsystem.component.MomoTopAppBar
import com.momoterminal.core.designsystem.theme.MomoTheme

@Composable
fun CodeDisplayScreen(
    orderId: String,
    onNavigateToHistory: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: CodeDisplayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            MomoTopAppBar(
                title = "Your Vending Code",
                onNavigateBack = onNavigateHome
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(MomoTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState) {
                is CodeDisplayUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is CodeDisplayUiState.Error -> {
                    Text(
                        text = "Error: ${(uiState as CodeDisplayUiState.Error).message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is CodeDisplayUiState.Success -> {
                    val order = (uiState as CodeDisplayUiState.Success).order
                    val code = order.code
                    
                    if (code != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(MomoTheme.spacing.lg),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.md)
                            ) {
                                Text(
                                    text = "Your Code",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Text(
                                    text = code.code.chunked(2).joinToString(" "),
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                HorizontalDivider()
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Cups",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "${code.remainingServes} of ${code.totalServes}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Expires in",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        val remainingSec = code.remainingSeconds()
                                        Text(
                                            text = "${remainingSec / 60}:${(remainingSec % 60).toString().padStart(2, '0')}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (remainingSec < 60) MaterialTheme.colorScheme.error 
                                                   else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(MomoTheme.spacing.xl))
                        
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(MomoTheme.spacing.md),
                                verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)
                            ) {
                                Text(
                                    text = "Machine Location",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = order.machineName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = order.machineLocation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = MomoTheme.spacing.sm))
                                
                                Text(
                                    text = "Instructions",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "1. Go to the machine location",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "2. Enter your 4-digit code on the keypad",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "3. Pour ${code.remainingServes} cup(s) of ${order.productName}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "4. Each cup is ${order.servingSizeML}ml",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                    } else {
                        Text(
                            text = "No code available for this order",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
