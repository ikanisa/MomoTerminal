package com.momoterminal.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MomoTeal,
    onPrimary = Color.White,
    primaryContainer = MomoTealContainer,
    onPrimaryContainer = MomoTealContainerDark,
    secondary = MomoGold,
    onSecondary = Color.White,
    secondaryContainer = MomoGoldContainer,
    onSecondaryContainer = MomoGoldContainerDark,
    tertiary = MomoLime,
    onTertiary = Color.White,
    tertiaryContainer = MomoLimeContainer,
    onTertiaryContainer = MomoLimeContainerDark,
    error = MomoError,
    onError = Color.White,
    errorContainer = MomoErrorContainer,
    onErrorContainer = MomoErrorContainerDark,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

private val DarkColorScheme = darkColorScheme(
    primary = MomoTealDark,
    onPrimary = MomoTealContainerDark,
    primaryContainer = MomoTealContainerDark,
    onPrimaryContainer = MomoTealLight,
    secondary = MomoGoldDark,
    onSecondary = MomoGoldContainerDark,
    secondaryContainer = MomoGoldContainerDark,
    onSecondaryContainer = MomoGoldDark,
    tertiary = MomoLimeDark,
    onTertiary = MomoLimeContainerDark,
    tertiaryContainer = MomoLimeContainerDark,
    onTertiaryContainer = MomoLimeDark,
    error = MomoErrorDark,
    onError = MomoErrorContainerDark,
    errorContainer = MomoErrorContainerDark,
    onErrorContainer = MomoErrorDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155)
)

// Extended colors for glass surfaces
data class MomoExtendedColors(
    val surfaceGlass: Color,
    val surfaceGlassElevated: Color,
    val glassBorder: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val credit: Color,
    val debit: Color,
    val warning: Color,
    val tokenAccent: Color
)

val LocalMomoColors = staticCompositionLocalOf {
    MomoExtendedColors(
        surfaceGlass = SurfaceGlassLight,
        surfaceGlassElevated = SurfaceGlassElevatedLight,
        glassBorder = GlassBorderLight,
        gradientStart = GradientStartLight,
        gradientEnd = GradientEndLight,
        credit = CreditGreen,
        debit = DebitRed,
        warning = MomoWarning,
        tokenAccent = MomoGold
    )
}

@Composable
fun MomoTerminalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val extendedColors = if (darkTheme) {
        MomoExtendedColors(
            surfaceGlass = SurfaceGlassDark,
            surfaceGlassElevated = SurfaceGlassElevatedDark,
            glassBorder = GlassBorderDark,
            gradientStart = GradientStartDark,
            gradientEnd = GradientEndDark,
            credit = CreditGreenDark,
            debit = DebitRedDark,
            warning = MomoWarningDark,
            tokenAccent = MomoGoldDark
        )
    } else {
        MomoExtendedColors(
            surfaceGlass = SurfaceGlassLight,
            surfaceGlassElevated = SurfaceGlassElevatedLight,
            glassBorder = GlassBorderLight,
            gradientStart = GradientStartLight,
            gradientEnd = GradientEndLight,
            credit = CreditGreen,
            debit = DebitRed,
            warning = MomoWarning,
            tokenAccent = MomoGold
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalMomoColors provides extendedColors,
        LocalMomoElevation provides MomoElevation(),
        LocalMomoSpacing provides MomoSpacing(),
        LocalMomoSizing provides MomoSizing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MomoTypography,
            shapes = MomoShapes,
            content = content
        )
    }
}

// Convenience accessors
object MomoTheme {
    val colors: MomoExtendedColors
        @Composable get() = LocalMomoColors.current
    
    val elevation: MomoElevation
        @Composable get() = LocalMomoElevation.current
    
    val spacing: MomoSpacing
        @Composable get() = LocalMomoSpacing.current
    
    val sizing: MomoSizing
        @Composable get() = LocalMomoSizing.current
}
