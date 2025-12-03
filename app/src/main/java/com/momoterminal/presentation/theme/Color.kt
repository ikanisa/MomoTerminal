package com.momoterminal.presentation.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// MoMo Brand Colors
val MomoYellow = Color(0xFFFFCC00)
val MomoYellowLight = Color(0xFFFFE066)
val MomoYellowDark = Color(0xFFCC9900)

val MomoBlue = Color(0xFF0046AD)
val MomoBlueLight = Color(0xFF3373C4)
val MomoBlueDark = Color(0xFF003080)

// Premium Neutrals
val Neutral50 = Color(0xFFFAFAFA)
val Neutral100 = Color(0xFFF5F5F5)
val Neutral200 = Color(0xFFEEEEEE)
val Neutral300 = Color(0xFFE0E0E0)
val Neutral400 = Color(0xFFBDBDBD)
val Neutral500 = Color(0xFF9E9E9E)
val Neutral600 = Color(0xFF757575)
val Neutral700 = Color(0xFF616161)
val Neutral800 = Color(0xFF424242)
val Neutral900 = Color(0xFF212121)

// Provider Colors
val MtnYellow = Color(0xFFFFCC00)
val VodafoneRed = Color(0xFFE60000)
val AirtelTigoRed = Color(0xFFED1C24)

// Status Colors
val SuccessGreen = Color(0xFF00C853)
val SuccessGreenLight = Color(0xFF69F0AE)
val ErrorRed = Color(0xFFD50000)
val ErrorRedLight = Color(0xFFFF5252)
val WarningOrange = Color(0xFFFFAB00)
val WarningOrangeLight = Color(0xFFFFD740)
val InfoBlue = Color(0xFF0091EA)

// Gradient Definitions for Premium Cards
object MomoGradients {
    val primary = Brush.linearGradient(
        colors = listOf(MomoYellow, MomoYellowLight, MomoYellowDark)
    )
    
    val premiumCard = Brush.linearGradient(
        colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
    )
    
    val success = Brush.linearGradient(
        colors = listOf(SuccessGreen, SuccessGreenLight)
    )
    
    val goldCard = Brush.linearGradient(
        colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFF8C00)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}

// Glassmorphism Colors
object GlassColors {
    val surfaceLight = Color.White.copy(alpha = 0.1f)
    val surfaceMedium = Color.White.copy(alpha = 0.15f)
    val border = Color.White.copy(alpha = 0.2f)
    val borderLight = Color.White.copy(alpha = 0.1f)
}

// Colored Shadows
object ColoredShadows {
    val primary = MomoYellow.copy(alpha = 0.3f)
    val success = SuccessGreen.copy(alpha = 0.3f)
    val error = ErrorRed.copy(alpha = 0.3f)
}

// Light Theme Colors
val LightBackground = Neutral50
val LightSurface = Color.White
val LightSurfaceVariant = Neutral100
val LightOnBackground = Neutral900
val LightOnSurface = Neutral900
val LightOnSurfaceVariant = Neutral700
val LightOutline = Neutral400
val LightPrimary = MomoYellow
val LightOnPrimary = Color.Black // High contrast on yellow
val LightPrimaryContainer = Color(0xFFFFF9C4)
val LightOnPrimaryContainer = Color(0xFF3D3000)
val LightSecondary = MomoBlue
val LightOnSecondary = Color.White
val LightSecondaryContainer = Color(0xFFE3F2FD)
val LightOnSecondaryContainer = MomoBlueDark
val LightTertiary = SuccessGreen
val LightOnTertiary = Color.White
val LightTertiaryContainer = Color(0xFFE8F5E9)
val LightOnTertiaryContainer = Color(0xFF002106)
val LightError = ErrorRed
val LightOnError = Color.White
val LightErrorContainer = Color(0xFFFFEBEE)
val LightOnErrorContainer = Color(0xFF410002)

// Dark Theme Colors
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)
val DarkOnBackground = Neutral100
val DarkOnSurface = Neutral100
val DarkOnSurfaceVariant = Neutral400
val DarkOutline = Neutral600
val DarkPrimary = MomoYellow
val DarkOnPrimary = Color.Black
val DarkPrimaryContainer = Color(0xFF5C4B00)
val DarkOnPrimaryContainer = MomoYellowLight
val DarkSecondary = MomoBlueLight
val DarkOnSecondary = Color.White
val DarkSecondaryContainer = Color(0xFF004494)
val DarkOnSecondaryContainer = Color(0xFFD6E3FF)
val DarkTertiary = SuccessGreenLight
val DarkOnTertiary = Color(0xFF003910)
val DarkTertiaryContainer = Color(0xFF005319)
val DarkOnTertiaryContainer = Color(0xFFC8FFC4)
val DarkError = ErrorRedLight
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

// Transaction Status Colors
val StatusPending = WarningOrange
val StatusSent = SuccessGreen
val StatusFailed = ErrorRed
val StatusProcessing = InfoBlue
