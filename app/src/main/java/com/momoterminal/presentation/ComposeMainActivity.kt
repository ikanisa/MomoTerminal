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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.momoterminal.auth.AuthViewModel
import com.momoterminal.auth.SessionManager
import com.momoterminal.presentation.components.error.AppErrorBoundary
import com.momoterminal.presentation.navigation.NavGraph
import com.momoterminal.presentation.navigation.Screen
import com.momoterminal.presentation.theme.MomoTerminalTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Main Activity using Jetpack Compose.
 * Hosts the navigation graph and bottom navigation bar.
 * Wrapped with AppErrorBoundary for global error handling.
 * Handles authentication state and session management.
 */
@AndroidEntryPoint
class ComposeMainActivity : ComponentActivity() {
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MomoTerminalTheme {
                // Wrap the entire app with error boundary for crash recovery
                AppErrorBoundary(
                    onError = { throwable ->
                        Timber.e(throwable, "Error caught by AppErrorBoundary")
                    }
                ) {
                    MomoTerminalApp(sessionManager = sessionManager)
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Record activity when app comes to foreground
        sessionManager.onAppForeground()
    }
    
    override fun onPause() {
        super.onPause()
        // Handle app going to background
        sessionManager.onAppBackground()
    }
}

@Composable
fun MomoTerminalApp(
    sessionManager: SessionManager,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val authUiState by authViewModel.uiState.collectAsState()
    val sessionState by sessionManager.sessionState.collectAsState()
    
    // Check if user is authenticated
    val isAuthenticated = authUiState.isAuthenticated
    
    // Handle session expiration
    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionManager.SessionState.Expired -> {
                // Navigate to PIN entry for session unlock
                navController.navigate(Screen.PinEntry.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                }
            }
            is SessionManager.SessionState.LoggedOut -> {
                // Navigate to login
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                }
            }
            else -> {}
        }
    }
    
    // Record user activity on navigation changes
    LaunchedEffect(currentDestination) {
        if (!Screen.isAuthScreen(currentDestination?.route)) {
            sessionManager.recordActivity()
        }
    }
    
    // Determine if we should show bottom nav (only for main screens when authenticated)
    val showBottomNav = isAuthenticated && Screen.bottomNavItems.any { screen ->
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
                modifier = Modifier.padding(paddingValues),
                isAuthenticated = isAuthenticated
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
