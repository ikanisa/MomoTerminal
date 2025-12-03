package com.momoterminal.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MomoElevation(
    val none: Dp = 0.dp,
    val level1: Dp = 2.dp,   // Subtle cards
    val level2: Dp = 4.dp,   // Glass cards
    val level3: Dp = 8.dp,   // Pinned headers
    val level4: Dp = 12.dp   // Floating actions, modals
)

@Immutable
data class MomoSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,       // Compact list rows
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,      // Standard padding
    val xl: Dp = 24.dp,      // Wallet summaries
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp     // Section gaps
)

@Immutable
data class MomoSizing(
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 24.dp,
    val iconLg: Dp = 32.dp,
    val iconXl: Dp = 48.dp,
    val buttonHeight: Dp = 56.dp,
    val buttonHeightSm: Dp = 40.dp,
    val chipHeight: Dp = 32.dp,
    val cardMinHeight: Dp = 80.dp,
    val balanceCardHeight: Dp = 160.dp,
    val transactionRowHeight: Dp = 72.dp,
    val bottomNavHeight: Dp = 80.dp
)

val LocalMomoElevation = staticCompositionLocalOf { MomoElevation() }
val LocalMomoSpacing = staticCompositionLocalOf { MomoSpacing() }
val LocalMomoSizing = staticCompositionLocalOf { MomoSizing() }
