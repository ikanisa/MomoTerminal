package com.momoterminal.presentation.components.terminal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.nfc.NfcPaymentData
import com.momoterminal.presentation.theme.AirtelTigoRed
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MtnYellow
import com.momoterminal.presentation.theme.PaymentShapes
import com.momoterminal.presentation.theme.VodafoneRed

/**
 * Provider selector for choosing mobile money provider.
 */
@Composable
fun ProviderSelector(
    selectedProvider: NfcPaymentData.Provider,
    onProviderSelected: (NfcPaymentData.Provider) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier.selectableGroup()
    ) {
        Text(
            text = "Select Provider",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NfcPaymentData.Provider.entries.forEach { provider ->
                ProviderCard(
                    provider = provider,
                    isSelected = provider == selectedProvider,
                    onClick = { onProviderSelected(provider) },
                    modifier = Modifier.weight(1f),
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: NfcPaymentData.Provider,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val providerColor = getProviderColor(provider)
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "scale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) providerColor else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "border"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            providerColor.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 150),
        label = "background"
    )
    
    Surface(
        modifier = modifier
            .scale(scale)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                enabled = enabled,
                role = Role.RadioButton
            ),
        shape = PaymentShapes.providerCard,
        color = backgroundColor,
        border = BorderStroke(
            width = 2.dp,
            color = borderColor
        ),
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Provider icon/indicator
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = providerColor
                ) {
                    if (isSelected) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = provider.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) providerColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Get the brand color for a provider.
 */
@Composable
fun getProviderColor(provider: NfcPaymentData.Provider): Color {
    return when (provider) {
        NfcPaymentData.Provider.MTN -> MtnYellow
        NfcPaymentData.Provider.VODAFONE -> VodafoneRed
        NfcPaymentData.Provider.AIRTEL_TIGO -> AirtelTigoRed
    }
}

@Preview(showBackground = true)
@Composable
private fun ProviderSelectorPreview() {
    MomoTerminalTheme {
        ProviderSelector(
            selectedProvider = NfcPaymentData.Provider.MTN,
            onProviderSelected = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProviderSelectorVodafonePreview() {
    MomoTerminalTheme {
        ProviderSelector(
            selectedProvider = NfcPaymentData.Provider.VODAFONE,
            onProviderSelected = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
