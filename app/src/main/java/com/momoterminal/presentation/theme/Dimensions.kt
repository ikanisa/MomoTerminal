package com.momoterminal.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Consistent spacing and sizing system for world-class UI.
 */
data class Dimensions(
    // Spacing scale (4dp base)
    val spaceXXS: Dp = 2.dp,
    val spaceXS: Dp = 4.dp,
    val spaceSM: Dp = 8.dp,
    val spaceMD: Dp = 12.dp,
    val spaceLG: Dp = 16.dp,
    val spaceXL: Dp = 24.dp,
    val spaceXXL: Dp = 32.dp,
    val spaceXXXL: Dp = 48.dp,
    
    // Component sizes
    val buttonHeight: Dp = 56.dp,
    val buttonHeightSmall: Dp = 40.dp,
    val iconSizeSmall: Dp = 16.dp,
    val iconSizeMedium: Dp = 24.dp,
    val iconSizeLarge: Dp = 32.dp,
    val iconSizeXL: Dp = 48.dp,
    
    // Touch targets (minimum 48dp for accessibility)
    val minTouchTarget: Dp = 48.dp,
    val keypadButtonSize: Dp = 72.dp,
    
    // Card & Container
    val cardElevation: Dp = 2.dp,
    val cardElevationPressed: Dp = 8.dp,
    
    // Border radius
    val radiusXS: Dp = 4.dp,
    val radiusSM: Dp = 8.dp,
    val radiusMD: Dp = 12.dp,
    val radiusLG: Dp = 16.dp,
    val radiusXL: Dp = 24.dp,
    val radiusFull: Dp = 100.dp,
    
    // Screen padding
    val screenPadding: Dp = 16.dp,
    val screenPaddingLarge: Dp = 24.dp
)

val LocalDimensions = staticCompositionLocalOf { Dimensions() }

object MomoDimens {
    val current: Dimensions
        @Composable
        get() = LocalDimensions.current
}
