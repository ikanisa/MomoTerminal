package com.momoterminal.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.momoterminal.presentation.navigation.NavGraph
import com.momoterminal.presentation.navigation.Screen
import com.momoterminal.presentation.theme.MomoTerminalTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity using Jetpack Compose.
 * Hosts the navigation graph and bottom navigation bar.
 */
@AndroidEntryPoint
class ComposeMainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MomoTerminalTheme {
                MomoTerminalApp()
            }
        }
    }
}

@Composable
fun MomoTerminalApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Determine if we should show bottom nav (only for main screens)
    val showBottomNav = Screen.bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomNav) {
                    MomoBottomNavigation(
                        items = Screen.bottomNavItems,
                        currentRoute = currentDestination?.route,
                        onItemClick = { screen ->
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            NavGraph(
                navController = navController,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun MomoBottomNavigation(
    items: List<Screen>,
    currentRoute: String?,
    onItemClick: (Screen) -> Unit
) {
    NavigationBar {
        items.forEach { screen ->
            val selected = currentRoute == screen.route
            
            NavigationBarItem(
                icon = {
                    val icon = if (selected) {
                        screen.selectedIcon
                    } else {
                        screen.unselectedIcon
                    }
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = screen.title
                        )
                    }
                },
                label = { Text(screen.title) },
                selected = selected,
                onClick = { onItemClick(screen) }
            )
        }
    }
}
