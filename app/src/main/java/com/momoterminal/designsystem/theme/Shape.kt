package com.momoterminal.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val MomoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // Small chips
    small = RoundedCornerShape(8.dp),        // Token chips
    medium = RoundedCornerShape(12.dp),      // Cards
    large = RoundedCornerShape(16.dp),       // Glass cards
    extraLarge = RoundedCornerShape(24.dp)   // Large action buttons, modals
)

// Custom shapes
val GlassCardShape = RoundedCornerShape(20.dp)
val BalanceCardShape = RoundedCornerShape(24.dp)
val TokenChipShape = RoundedCornerShape(12.dp)
val ActionButtonShape = RoundedCornerShape(16.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
