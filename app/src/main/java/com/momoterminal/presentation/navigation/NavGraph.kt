package com.momoterminal.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.momoterminal.capabilities.CapabilitiesDemoScreen
import com.momoterminal.designsystem.motion.MotionTokens
import com.momoterminal.feature.vending.navigation.vendingNavGraph
import com.momoterminal.presentation.screens.auth.ForgotPinScreen
import com.momoterminal.presentation.screens.auth.LoginScreen
import com.momoterminal.presentation.screens.auth.PinScreen
import com.momoterminal.presentation.screens.auth.RegisterScreen
import com.momoterminal.presentation.screens.home.HomeScreen
import com.momoterminal.presentation.screens.home.HomeViewModel
import com.momoterminal.presentation.screens.nfc.NfcTerminalScreen
import com.momoterminal.presentation.screens.settings.SettingsScreen
import com.momoterminal.presentation.screens.transaction.TransactionDetailScreen
import com.momoterminal.presentation.screens.transactions.TransactionsScreen
import com.momoterminal.feature.vending.navigation.VendingDestination
import com.momoterminal.feature.vending.navigation.vendingNavGraph

/**
 * Main navigation graph for the app.
 * Uses MomoTerminal motion system for polished, "money-safe" transitions.
 * 
 * Transition principles:
 * - Standard screens: 300ms horizontal slide with fade
 * - Financial screens: 450ms with scale for emphasis
 * - Quick utilities: 200ms for snappy response
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
        // Standard screen transitions using motion tokens
        enterTransition = {
            fadeIn(animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOut)) + 
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOutExpo)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)) + 
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOut)) + 
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOutExpo)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)) + 
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)
            )
        }
    ) {
        // Login screen - uses financial transition (slower, more deliberate)
        composable(
            route = Screen.Login.route,
            enterTransition = {
                fadeIn(tween(MotionTokens.FINANCIAL, easing = MotionTokens.EaseFinancial)) +
                scaleIn(initialScale = 0.95f, animationSpec = tween(MotionTokens.FINANCIAL, easing = MotionTokens.EaseFinancial))
            },
            exitTransition = {
                fadeOut(tween(MotionTokens.STANDARD))
            }
        ) {
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
        
        // PIN entry screen - uses financial transition
        composable(
            route = Screen.PinEntry.route,
            enterTransition = {
                fadeIn(tween(MotionTokens.FINANCIAL, easing = MotionTokens.EaseFinancial)) +
                scaleIn(initialScale = 0.95f, animationSpec = tween(MotionTokens.FINANCIAL, easing = MotionTokens.EaseFinancial))
            }
        ) {
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
                    navController.navigate(Screen.NfcTerminal.route)
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToVending = {
                    navController.navigate(VendingDestination.Root.route)
                }
            )
        }
        
        // NFC Terminal screen - dedicated NFC payment flow with motion system
        composable(
            route = Screen.NfcTerminal.route,
            enterTransition = {
                fadeIn(tween(MotionTokens.FINANCIAL, easing = MotionTokens.EaseFinancial)) +
                scaleIn(initialScale = 0.95f, animationSpec = tween(MotionTokens.FINANCIAL, easing = MotionTokens.EaseFinancial))
            },
            exitTransition = {
                fadeOut(tween(MotionTokens.STANDARD)) +
                scaleOut(targetScale = 0.95f, animationSpec = tween(MotionTokens.STANDARD))
            }
        ) {
            val viewModel: HomeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val nfcState by viewModel.nfcState.collectAsState()
            
            NfcTerminalScreen(
                amount = uiState.amount.toDoubleOrNull() ?: 0.0,
                currencySymbol = uiState.currencySymbol,
                nfcState = nfcState,
                onActivate = { viewModel.activatePayment() },
                onCancel = { 
                    viewModel.cancelPayment()
                    navController.popBackStack()
                },
                onComplete = {
                    viewModel.cancelPayment()
                    navController.popBackStack()
                }
            )
        }
        
        // Transactions screen (History) - quick transition
        composable(
            route = Screen.Transactions.route,
            enterTransition = {
                fadeIn(tween(MotionTokens.QUICK)) + slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOutExpo)
                )
            },
            exitTransition = {
                fadeOut(tween(MotionTokens.QUICK)) + slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)
                )
            }
        ) {
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
                onNavigateToVending = {
                    navController.navigate(Screen.Vending.route)
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
        
        // Transaction detail screen - financial transition (important info)
        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.LongType }
            ),
            enterTransition = {
                fadeIn(tween(MotionTokens.FINANCIAL, easing = MotionTokens.EaseFinancial)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(MotionTokens.FINANCIAL, easing = MotionTokens.EaseOutBack))
            },
            exitTransition = {
                fadeOut(tween(MotionTokens.STANDARD)) +
                scaleOut(targetScale = 0.92f, animationSpec = tween(MotionTokens.STANDARD))
            }
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            TransactionDetailScreen(
                transactionId = transactionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Wallet screen - financial transition
        composable(
            route = Screen.Wallet.route,
            enterTransition = {
                fadeIn(tween(MotionTokens.FINANCIAL, easing = MotionTokens.EaseFinancial)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(MotionTokens.FINANCIAL, easing = MotionTokens.EaseOutExpo)
                )
            }
        ) {
            com.momoterminal.presentation.screens.wallet.WalletScreen(
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route)
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
        
        // Vending navigation graph
        vendingNavGraph(
            navController = navController,
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}
