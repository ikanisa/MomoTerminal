package com.momoterminal.feature.vending.ui.event

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.component.*
import com.momoterminal.core.designsystem.theme.MomoTheme

@Composable
fun EventOrderScreen(
    eventId: String,
    onNavigateToCode: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MomoTopAppBar(
            title = "Event Mode",
            onNavigateBack = onNavigateBack
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MomoTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.md)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(MomoTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)
                ) {
                    Text(
                        text = "Event Code: $eventId",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Scan your event wristband or QR code to receive a free beverage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = "Select Cup Size",
                style = MaterialTheme.typography.titleMedium
            )
            
            var selectedSize by remember { mutableStateOf("500ml") }
            
            Column(verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)) {
                listOf("250ml", "500ml", "750ml").forEach { size ->
                    PressableCard(
                        onClick = { selectedSize = size },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MomoTheme.spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(size, style = MaterialTheme.typography.titleMedium)
                            if (selectedSize == size) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            PrimaryActionButton(
                text = "Confirm Order",
                onClick = {
                    onNavigateToCode("event-order-${System.currentTimeMillis()}")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
