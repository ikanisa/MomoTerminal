package com.momoterminal.presentation.components.terminal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.core.domain.model.NfcPaymentData
import com.momoterminal.presentation.theme.MomoAnimation
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MtnYellow
import com.momoterminal.presentation.theme.VodafoneRed

/**
 * Horizontally scrollable provider selector with premium chip design.
 * Features smooth selection animations and haptic feedback.
 */
@Composable
fun ProviderSelector(
    selectedProvider: NfcPaymentData.Provider,
    onProviderSelected: (NfcPaymentData.Provider) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    
    Column(modifier = modifier) {
        Text(
            text = "Select Provider",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        
        Row(
            modifier = Modifier.horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NfcPaymentData.Provider.entries.forEach { provider ->
                ProviderChip(
                    provider = provider,
                    isSelected = provider == selectedProvider,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onProviderSelected(provider)
                    }
                )
            }
        }
    }
}

/**
 * Individual provider chip with selection animation.
 */
@Composable
private fun ProviderChip(
    provider: NfcPaymentData.Provider,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val providerColor = getProviderColor(provider)
    
    // Animated scale
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isSelected -> 1.02f
            else -> 1f
        },
        animationSpec = tween(MomoAnimation.DURATION_INSTANT),
        label = "scale"
    )
    
    // Animated background
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            providerColor.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "background"
    )
    
    // Animated border
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) providerColor else Color.Transparent,
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "border"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "borderWidth"
    )
    
    // Animated elevation
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "elevation"
    )
    
    Box(
        modifier = Modifier
            .scale(scale)
            .shadow(elevation, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Provider color indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(providerColor)
            )
            
            Text(
                text = provider.displayName,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) {
                    providerColor
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            // Checkmark for selected state
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(16.dp),
                    tint = providerColor
                )
            }
        }
    }
}

/**
 * Get brand color for provider from hex string.
 */
private fun getProviderColor(provider: NfcPaymentData.Provider): Color {
    return try {
        Color(android.graphics.Color.parseColor(provider.colorHex))
    } catch (_: Exception) {
        MtnYellow
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
