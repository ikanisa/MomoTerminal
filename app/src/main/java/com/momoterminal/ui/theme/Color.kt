package com.momoterminal.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * MoMo Brand Colors
 */
object MomoBrandColors {
    val MomoYellow = Color(0xFFFFCC00)
    val MomoYellowLight = Color(0xFFFFE066)
    val MomoYellowDark = Color(0xFFCC9900)

    val MomoBlue = Color(0xFF0046AD)
    val MomoBlueLight = Color(0xFF3373C4)
    val MomoBlueDark = Color(0xFF003080)
}

/**
 * Provider Colors for different mobile money providers.
 */
object ProviderColors {
    val MtnYellow = Color(0xFFFFCC00)
    val VodafoneRed = Color(0xFFE60000)
    val AirtelTigoRed = Color(0xFFED1C24)
}

/**
 * Status Colors for transaction states.
 */
object StatusColors {
    val Success = Color(0xFF00A651)
    val SuccessLight = Color(0xFF4CAF50)
    val Error = Color(0xFFE60000)
    val ErrorLight = Color(0xFFFF5252)
    val Warning = Color(0xFFFF9800)
    val WarningLight = Color(0xFFFFB74D)
    val Info = Color(0xFF2196F3)
    val InfoLight = Color(0xFF64B5F6)
    val Pending = Color(0xFFFF9800)
    val PendingLight = Color(0xFFFFB74D)
    val Processing = Color(0xFF2196F3)
    val ProcessingLight = Color(0xFF64B5F6)
}

/**
 * Financial App Colors - trust-inducing palette.
 */
object FinancialColors {
    val TrustBlue = Color(0xFF0046AD)
    val TrustBlueLight = Color(0xFF3373C4)
    val SuccessGreen = Color(0xFF00A651)
    val SuccessGreenLight = Color(0xFF4CAF50)
    val AccentOrange = Color(0xFFFF6D00)
    val AccentOrangeLight = Color(0xFFFFAB40)
}

/**
 * Transaction Colors for money flow visualization.
 */
object TransactionColors {
    // Money received (credit)
    val MoneyIn = Color(0xFF00A651)
    val MoneyInLight = Color(0xFF4CAF50)
    val MoneyInBackground = Color(0xFFE8F5E9)
    val MoneyInBackgroundDark = Color(0xFF1B3D1B)

    // Money sent (debit)
    val MoneyOut = Color(0xFFE60000)
    val MoneyOutLight = Color(0xFFFF5252)
    val MoneyOutBackground = Color(0xFFFFEBEE)
    val MoneyOutBackgroundDark = Color(0xFF3D1B1B)
}

/**
 * Surface Colors for light theme.
 */
object LightSurfaceColors {
    val Background = Color(0xFFFFFBFE)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF5F5F5)
    val OnBackground = Color(0xFF1C1B1F)
    val OnSurface = Color(0xFF1C1B1F)
    val OnSurfaceVariant = Color(0xFF49454F)
    val Outline = Color(0xFF79747E)
}

/**
 * Surface Colors for dark theme.
 */
object DarkSurfaceColors {
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF1E1E1E)
    val SurfaceVariant = Color(0xFF2D2D2D)
    val OnBackground = Color(0xFFE6E1E5)
    val OnSurface = Color(0xFFE6E1E5)
    val OnSurfaceVariant = Color(0xFFCAC4D0)
    val Outline = Color(0xFF938F99)
}

/**
 * Splash screen background color.
 */
val SplashBackground = MomoBrandColors.MomoYellow
