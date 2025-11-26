package com.momoterminal.ui.components.adaptive

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Window size class for MomoTerminal adaptive layouts.
 */
enum class MomoWindowSizeClass {
    /**
     * Compact - Phone in portrait mode
     */
    COMPACT,

    /**
     * Medium - Phone in landscape or small tablet
     */
    MEDIUM,

    /**
     * Expanded - Large tablet or foldable
     */
    EXPANDED
}

/**
 * CompositionLocal for window size class.
 */
val LocalWindowSizeClass = compositionLocalOf { MomoWindowSizeClass.COMPACT }

/**
 * Calculate the MomoWindowSizeClass based on the current window size.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun calculateMomoWindowSizeClass(): MomoWindowSizeClass {
    val context = LocalContext.current
    val activity = context as? Activity

    return if (activity != null) {
        val windowSizeClass = calculateWindowSizeClass(activity)
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> MomoWindowSizeClass.COMPACT
            WindowWidthSizeClass.Medium -> MomoWindowSizeClass.MEDIUM
            WindowWidthSizeClass.Expanded -> MomoWindowSizeClass.EXPANDED
            else -> MomoWindowSizeClass.COMPACT
        }
    } else {
        MomoWindowSizeClass.COMPACT
    }
}

/**
 * Data class representing a navigation item.
 */
data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val contentDescription: String = label
)

/**
 * Adaptive navigation composable that switches between:
 * - COMPACT: NavigationBar (bottom navigation)
 * - MEDIUM: NavigationRail (side rail)
 * - EXPANDED: PermanentNavigationDrawer (side drawer)
 *
 * @param items The navigation items to display
 * @param selectedIndex The currently selected item index
 * @param onItemSelected Callback when an item is selected
 * @param modifier Modifier for the container
 * @param content The main content
 */
@Composable
fun AdaptiveNavigation(
    items: List<NavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val windowSizeClass = LocalWindowSizeClass.current

    when (windowSizeClass) {
        MomoWindowSizeClass.COMPACT -> {
            // Bottom navigation for phones
            Column(modifier = modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    content()
                }

                NavigationBar {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selectedIndex == index) item.selectedIcon else item.icon,
                                    contentDescription = item.contentDescription
                                )
                            },
                            label = { Text(item.label) },
                            selected = selectedIndex == index,
                            onClick = { onItemSelected(index) }
                        )
                    }
                }
            }
        }

        MomoWindowSizeClass.MEDIUM -> {
            // Navigation rail for medium screens
            Row(modifier = modifier.fillMaxSize()) {
                NavigationRail {
                    items.forEachIndexed { index, item ->
                        NavigationRailItem(
                            icon = {
                                Icon(
                                    imageVector = if (selectedIndex == index) item.selectedIcon else item.icon,
                                    contentDescription = item.contentDescription
                                )
                            },
                            label = { Text(item.label) },
                            selected = selectedIndex == index,
                            onClick = { onItemSelected(index) }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    content()
                }
            }
        }

        MomoWindowSizeClass.EXPANDED -> {
            // Permanent navigation drawer for large screens
            PermanentNavigationDrawer(
                drawerContent = {
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        items.forEachIndexed { index, item ->
                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        imageVector = if (selectedIndex == index) item.selectedIcon else item.icon,
                                        contentDescription = item.contentDescription
                                    )
                                },
                                label = { Text(item.label) },
                                selected = selectedIndex == index,
                                onClick = { onItemSelected(index) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                },
                modifier = modifier
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Adaptive list-detail layout for master-detail patterns.
 * Shows list and detail side-by-side on larger screens.
 *
 * @param listContent The list content
 * @param detailContent The detail content (shown when an item is selected)
 * @param showDetail Whether to show the detail panel
 * @param modifier Modifier for the container
 */
@Composable
fun AdaptiveListDetailLayout(
    listContent: @Composable () -> Unit,
    detailContent: @Composable () -> Unit,
    showDetail: Boolean,
    modifier: Modifier = Modifier
) {
    val windowSizeClass = LocalWindowSizeClass.current

    when (windowSizeClass) {
        MomoWindowSizeClass.COMPACT -> {
            // Stack layout - show one at a time
            Box(modifier = modifier.fillMaxSize()) {
                if (showDetail) {
                    detailContent()
                } else {
                    listContent()
                }
            }
        }

        MomoWindowSizeClass.MEDIUM, MomoWindowSizeClass.EXPANDED -> {
            // Side-by-side layout
            Row(modifier = modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(if (windowSizeClass == MomoWindowSizeClass.MEDIUM) 0.4f else 0.35f)
                        .fillMaxHeight()
                ) {
                    listContent()
                }

                Box(
                    modifier = Modifier
                        .weight(if (windowSizeClass == MomoWindowSizeClass.MEDIUM) 0.6f else 0.65f)
                        .fillMaxHeight()
                ) {
                    if (showDetail) {
                        detailContent()
                    }
                }
            }
        }
    }
}

/**
 * Provider for window size class that automatically calculates the current size.
 *
 * @param content The content that can access the window size class
 */
@Composable
fun WindowSizeClassProvider(
    content: @Composable () -> Unit
) {
    val windowSizeClass = calculateMomoWindowSizeClass()
    CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
        content()
    }
}

/**
 * Check if the current layout is compact (phone).
 */
@Composable
fun isCompactLayout(): Boolean {
    return LocalWindowSizeClass.current == MomoWindowSizeClass.COMPACT
}

/**
 * Check if the current layout is expanded (tablet/foldable).
 */
@Composable
fun isExpandedLayout(): Boolean {
    return LocalWindowSizeClass.current == MomoWindowSizeClass.EXPANDED
}

/**
 * Get the recommended number of columns for a grid based on window size.
 */
@Composable
fun adaptiveGridColumns(): Int {
    return when (LocalWindowSizeClass.current) {
        MomoWindowSizeClass.COMPACT -> 1
        MomoWindowSizeClass.MEDIUM -> 2
        MomoWindowSizeClass.EXPANDED -> 3
    }
}
