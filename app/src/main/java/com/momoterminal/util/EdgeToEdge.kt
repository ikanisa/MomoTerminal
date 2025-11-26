package com.momoterminal.util

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Enable edge-to-edge display for the activity.
 * Makes status bar and navigation bar transparent and draws content behind them.
 *
 * @param lightStatusBar Whether to use light status bar icons (for light backgrounds)
 * @param lightNavigationBar Whether to use light navigation bar icons (for light backgrounds)
 */
fun Activity.enableEdgeToEdge(
    lightStatusBar: Boolean = false,
    lightNavigationBar: Boolean = false
) {
    // Enable edge-to-edge
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // Make bars transparent
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT

    // Handle navigation bar scrim for older devices
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }

    // Configure system bar appearance
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = lightStatusBar
        isAppearanceLightNavigationBars = lightNavigationBar
    }
}

/**
 * Enable immersive mode for fullscreen display.
 * Hides both status bar and navigation bar.
 */
fun Activity.enableImmersiveMode() {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    WindowInsetsControllerCompat(window, window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

/**
 * Disable immersive mode and show system bars.
 */
fun Activity.disableImmersiveMode() {
    WindowCompat.setDecorFitsSystemWindows(window, true)

    WindowInsetsControllerCompat(window, window.decorView).apply {
        show(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * Keep screen on while this composable is displayed.
 */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

// Composable modifiers for inset handling

/**
 * Add padding for all system bars (status bar and navigation bar).
 */
fun Modifier.systemBarsPadding(): Modifier = composed {
    this.padding(WindowInsets.systemBars.asPaddingValues())
}

/**
 * Add padding for status bar only.
 */
fun Modifier.statusBarsPadding(): Modifier = composed {
    this.padding(WindowInsets.statusBars.asPaddingValues())
}

/**
 * Add padding for navigation bar only.
 */
fun Modifier.navigationBarsPadding(): Modifier = composed {
    this.padding(WindowInsets.navigationBars.asPaddingValues())
}

/**
 * Add padding for IME (keyboard).
 */
fun Modifier.imePadding(): Modifier = composed {
    this.padding(WindowInsets.ime.asPaddingValues())
}

/**
 * Add padding for safe drawing area.
 */
fun Modifier.safeDrawingPadding(): Modifier = composed {
    this.padding(WindowInsets.safeDrawing.asPaddingValues())
}

/**
 * Get status bars padding values.
 */
@Composable
fun statusBarsPaddingValues(): PaddingValues = WindowInsets.statusBars.asPaddingValues()

/**
 * Get navigation bars padding values.
 */
@Composable
fun navigationBarsPaddingValues(): PaddingValues = WindowInsets.navigationBars.asPaddingValues()

/**
 * Get system bars padding values.
 */
@Composable
fun systemBarsPaddingValues(): PaddingValues = WindowInsets.systemBars.asPaddingValues()

/**
 * Get IME padding values.
 */
@Composable
fun imePaddingValues(): PaddingValues = WindowInsets.ime.asPaddingValues()

/**
 * Edge-to-edge Scaffold that handles system bar padding automatically.
 *
 * @param modifier The modifier for the scaffold
 * @param topBar The top bar composable
 * @param bottomBar The bottom bar composable
 * @param floatingActionButton The FAB composable
 * @param content The main content
 */
@Composable
fun EdgeToEdgeScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = content
    )
}
