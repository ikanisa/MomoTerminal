package com.momoterminal.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Custom MomoColors for transaction-specific colors.
 * These colors are used for financial transactions and status indicators.
 */
data class MomoColors(
    val moneyIn: Color,
    val moneyOut: Color,
    val success: Color,
    val error: Color,
    val warning: Color,
    val trustBlue: Color,
    val accentOrange: Color,
    val pending: Color,
    val processing: Color
)

/**
 * Light theme MomoColors for transaction displays.
 */
private val LightMomoColors = MomoColors(
    moneyIn = Color(0xFF00A651),      // Success green for incoming money
    moneyOut = Color(0xFFE60000),      // Error red for outgoing money
    success = Color(0xFF00A651),       // Success green
    error = Color(0xFFE60000),         // Error red
    warning = Color(0xFFFF9800),       // Warning orange
    trustBlue = Color(0xFF0046AD),     // Trust blue for financial apps
    accentOrange = Color(0xFFFF6D00),  // Accent orange
    pending = Color(0xFFFF9800),       // Pending orange
    processing = Color(0xFF2196F3)     // Processing blue
)

/**
 * Dark theme MomoColors for transaction displays.
 */
private val DarkMomoColors = MomoColors(
    moneyIn = Color(0xFF4CAF50),       // Lighter success green
    moneyOut = Color(0xFFFF5252),      // Lighter error red
    success = Color(0xFF4CAF50),       // Lighter success green
    error = Color(0xFFFF5252),         // Lighter error red
    warning = Color(0xFFFFB74D),       // Lighter warning orange
    trustBlue = Color(0xFF3373C4),     // Lighter trust blue
    accentOrange = Color(0xFFFFAB40),  // Lighter accent orange
    pending = Color(0xFFFFB74D),       // Lighter pending orange
    processing = Color(0xFF64B5F6)     // Lighter processing blue
)

/**
 * CompositionLocal for MomoColors.
 */
val LocalMomoColors = staticCompositionLocalOf { LightMomoColors }

/**
 * Light color scheme for MomoTerminal.
 * Financial app colors with trust blue, success green, and accent orange.
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFFCC00),             // MoMo Yellow
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFFFFF8DC),
    onPrimaryContainer = Color(0xFF3D3000),
    secondary = Color(0xFF0046AD),           // Trust Blue
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E3FF),
    onSecondaryContainer = Color(0xFF001B3E),
    tertiary = Color(0xFF00A651),            // Success Green
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC8FFC4),
    onTertiaryContainer = Color(0xFF002106),
    error = Color(0xFFE60000),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
)

/**
 * Dark color scheme for MomoTerminal.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFE066),             // Lighter MoMo Yellow
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF5C4B00),
    onPrimaryContainer = Color(0xFFFFE066),
    secondary = Color(0xFF3373C4),           // Lighter Trust Blue
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF004494),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = Color(0xFF4CAF50),            // Lighter Success Green
    onTertiary = Color(0xFF003910),
    tertiaryContainer = Color(0xFF005319),
    onTertiaryContainer = Color(0xFFC8FFC4),
    error = Color(0xFFFF5252),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
)

/**
 * MomoTerminal Theme with dynamic color support for Android 12+.
 * Provides edge-to-edge system bar configuration and custom MomoColors.
 *
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use dynamic color (Android 12+)
 * @param content The composable content
 */
@Composable
fun MomoTerminalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val momoColors = if (darkTheme) DarkMomoColors else LightMomoColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Configure edge-to-edge display
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalMomoColors provides momoColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MomoTypography,
            shapes = MomoShapes,
            content = content
        )
    }
}

/**
 * Access the current MomoColors from the theme.
 */
object MomoTheme {
    val colors: MomoColors
        @Composable
        get() = LocalMomoColors.current
}
