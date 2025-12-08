package com.momoterminal.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class representing all screens in the app.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    // Authentication screens
    data object Login : Screen(
        route = "login",
        title = "Login"
    )
    
    data object Register : Screen(
        route = "register",
        title = "Register"
    )
    
    data object PinEntry : Screen(
        route = "pin_entry",
        title = "Enter PIN"
    )
    
    data object ForgotPin : Screen(
        route = "forgot_pin",
        title = "Reset PIN"
    )
    
    // Bottom navigation screens
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    data object Terminal : Screen(
        route = "terminal",
        title = "Terminal",
        selectedIcon = Icons.Outlined.Nfc,
        unselectedIcon = Icons.Outlined.Nfc
    )
    
    data object Transactions : Screen(
        route = "transactions",
        title = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )
    
    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
    
    // Detail screens (not in bottom nav)
    data object TransactionDetail : Screen(
        route = "transaction/{transactionId}",
        title = "Transaction Details"
    ) {
        fun createRoute(transactionId: Long): String = "transaction/$transactionId"
    }
    
    data object Wallet : Screen(
        route = "wallet",
        title = "Wallet",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet
    )
    
    // Capabilities Demo screen - demonstrates various Android app capabilities
    data object CapabilitiesDemo : Screen(
        route = "capabilities_demo",
        title = "Capabilities Demo",
        selectedIcon = Icons.Filled.Build,
        unselectedIcon = Icons.Outlined.Build
    )
    
    // Motion System Showcase - demonstrates motion design system
    data object MotionShowcase : Screen(
        route = "motion_showcase",
        title = "Motion System",
        selectedIcon = Icons.Filled.Animation,
        unselectedIcon = Icons.Outlined.Animation
    )
    
    // NFC Terminal - dedicated NFC payment screen
    data object NfcTerminal : Screen(
        route = "nfc_terminal",
        title = "NFC Terminal",
        selectedIcon = Icons.Outlined.Nfc,
        unselectedIcon = Icons.Outlined.Nfc
    )
    
    companion object {
        /**
         * All screens that appear in the bottom navigation bar.
         * History moved to Settings, Wallet added to main nav.
         */
        val bottomNavItems = listOf(Home, Wallet, Settings)
        
        /**
         * Authentication screens.
         */
        val authScreens = listOf(Login, Register, PinEntry, ForgotPin)
        
        /**
         * Get screen from route.
         */
        fun fromRoute(route: String?): Screen? {
            return when {
                route == null -> null
                route == Login.route -> Login
                route == Register.route -> Register
                route == PinEntry.route -> PinEntry
                route == ForgotPin.route -> ForgotPin
                route == Home.route -> Home
                route == Terminal.route -> Terminal
                route == Transactions.route -> Transactions
                route == Settings.route -> Settings
                route == CapabilitiesDemo.route -> CapabilitiesDemo
                route == MotionShowcase.route -> MotionShowcase
                route == NfcTerminal.route -> NfcTerminal
                route.startsWith("transaction/") -> TransactionDetail
                else -> null
            }
        }
        
        /**
         * Check if a route is an authentication screen.
         */
        fun isAuthScreen(route: String?): Boolean {
            return route in listOf(Login.route, Register.route, PinEntry.route, ForgotPin.route)
        }
    }
}
