package com.momoterminal.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.momoterminal.capabilities.CapabilitiesDemoScreen
import com.momoterminal.presentation.screens.auth.ForgotPinScreen
import com.momoterminal.presentation.screens.auth.LoginScreen
import com.momoterminal.presentation.screens.auth.PinScreen
import com.momoterminal.presentation.screens.auth.RegisterScreen
import com.momoterminal.presentation.screens.home.HomeScreen
import com.momoterminal.presentation.screens.settings.SettingsScreen
import com.momoterminal.presentation.screens.transaction.TransactionDetailScreen
import com.momoterminal.presentation.screens.transactions.TransactionsScreen

/**
 * Main navigation graph for the app.
 * Defines all navigation destinations and transitions.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
    isAuthenticated: Boolean = false
) {
    // TEMP: Skip auth for testing keypad
    val actualStartDestination = Screen.Home.route

    NavHost(
        navController = navController,
        startDestination = actualStartDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
    ) {
        // Login screen
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPin = {
                    navController.navigate(Screen.ForgotPin.route)
                },
                onShowBiometricPrompt = {
                    // Handled by the screen itself
                }
            )
        }
        
        // Register screen
        composable(route = Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        // PIN entry screen
        composable(route = Screen.PinEntry.route) {
            PinScreen(
                title = "Unlock App",
                subtitle = "Enter your PIN to continue",
                onPinEntered = { pin ->
                    // Handle PIN verification
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PinEntry.route) { inclusive = true }
                    }
                },
                onCancel = {
                    // Go back to login
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.PinEntry.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Forgot PIN screen
        composable(route = Screen.ForgotPin.route) {
            ForgotPinScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ForgotPin.route) { inclusive = true }
                    }
                }
            )
        }

        // Home screen (with integrated terminal)
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToTerminal = {
                    // Terminal is now integrated into Home, no navigation needed
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        // Transactions screen (History)
        composable(route = Screen.Transactions.route) {
            TransactionsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                }
            )
        }
        
        // Settings screen
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { saveState = false }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            )
        }
        
        // Transaction detail screen
        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            TransactionDetailScreen(
                transactionId = transactionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Capabilities Demo screen - demonstrates various Android app capabilities
        composable(route = Screen.CapabilitiesDemo.route) {
            CapabilitiesDemoScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
