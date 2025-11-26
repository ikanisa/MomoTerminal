package com.momoterminal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 shape definitions for MomoTerminal.
 */
val MomoShapes = Shapes(
    // Extra small - for chips, small buttons
    extraSmall = RoundedCornerShape(4.dp),

    // Small - for cards, text fields
    small = RoundedCornerShape(8.dp),

    // Medium - for dialogs, floating action buttons
    medium = RoundedCornerShape(12.dp),

    // Large - for large cards, bottom sheets
    large = RoundedCornerShape(16.dp),

    // Extra large - for bottom navigation, modal sheets
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Custom shapes for specific MomoTerminal components.
 */
object MomoCustomShapes {
    val keypadButton = RoundedCornerShape(16.dp)
    val amountDisplay = RoundedCornerShape(12.dp)
    val providerCard = RoundedCornerShape(12.dp)
    val transactionCard = RoundedCornerShape(12.dp)
    val statusBadge = RoundedCornerShape(8.dp)
    val nfcIndicator = RoundedCornerShape(percent = 50)
    val bottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val dialog = RoundedCornerShape(28.dp)
}
