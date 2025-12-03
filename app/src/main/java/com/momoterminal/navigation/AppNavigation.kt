package com.momoterminal.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.momoterminal.feature.auth.ui.AuthScreen
import com.momoterminal.feature.payment.ui.PaymentScreen
import com.momoterminal.feature.settings.ui.SettingsScreen
import com.momoterminal.feature.transactions.ui.TransactionsScreen

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Payment : Screen("payment")
    data object Transactions : Screen("transactions")
    data object Settings : Screen("settings")
    data object NfcScan : Screen("nfc_scan")
    data object Wallet : Screen("wallet")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Auth.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            PaymentScreen()
        }

        composable(Screen.Payment.route) {
            PaymentScreen()
        }

        composable(Screen.Transactions.route) {
            TransactionsScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(Screen.NfcScan.route) {
            com.momoterminal.feature.nfc.ui.NfcScanScreen(
                onScanSuccess = { /* Handle success */ },
                onScanError = { /* Handle error */ },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Wallet.route) {
            com.momoterminal.feature.wallet.ui.WalletScreen(
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route)
                }
            )
        }
    }
}
