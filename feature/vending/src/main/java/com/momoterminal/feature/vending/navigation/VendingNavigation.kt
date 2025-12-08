package com.momoterminal.feature.vending.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.momoterminal.feature.vending.ui.code.CodeDisplayScreen
import com.momoterminal.feature.vending.ui.detail.MachineDetailScreen
import com.momoterminal.feature.vending.ui.help.VendingHelpScreen
import com.momoterminal.feature.vending.ui.history.OrderHistoryScreen
import com.momoterminal.feature.vending.ui.machines.MachinesScreen
import com.momoterminal.feature.vending.ui.payment.PaymentConfirmationScreen

sealed class VendingDestination(val route: String) {
    object Machines : VendingDestination("vending/machines")
    object MachineDetail : VendingDestination("vending/machine/{machineId}") {
        fun createRoute(machineId: String) = "vending/machine/$machineId"
    }
    object Payment : VendingDestination("vending/payment/{machineId}") {
        fun createRoute(machineId: String) = "vending/payment/$machineId"
    }
    object CodeDisplay : VendingDestination("vending/code/{orderId}") {
        fun createRoute(orderId: String) = "vending/code/$orderId"
    }
    object OrderHistory : VendingDestination("vending/orders")
    object Help : VendingDestination("vending/help")
}

fun NavGraphBuilder.vendingNavGraph(
    navController: NavHostController,
    onNavigateToTopUp: () -> Unit
) {
    composable(VendingDestination.Machines.route) {
        MachinesScreen(
            onMachineClick = { machineId ->
                navController.navigate(VendingDestination.MachineDetail.createRoute(machineId))
            },
            onOrderHistoryClick = {
                navController.navigate(VendingDestination.OrderHistory.route)
            },
            onHelpClick = {
                navController.navigate(VendingDestination.Help.route)
            },
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(
        route = VendingDestination.MachineDetail.route,
        arguments = listOf(navArgument("machineId") { type = NavType.StringType })
    ) { backStackEntry ->
        val machineId = backStackEntry.arguments?.getString("machineId") ?: return@composable
        MachineDetailScreen(
            machineId = machineId,
            onBuyClick = {
                navController.navigate(VendingDestination.Payment.createRoute(machineId))
            },
            onNavigateBack = { navController.popBackStack() },
            onGetDirections = { /* Handle directions */ }
        )
    }

    composable(
        route = VendingDestination.Payment.route,
        arguments = listOf(navArgument("machineId") { type = NavType.StringType })
    ) { backStackEntry ->
        val machineId = backStackEntry.arguments?.getString("machineId") ?: return@composable
        PaymentConfirmationScreen(
            machineId = machineId,
            onConfirmPayment = { orderId ->
                navController.navigate(VendingDestination.CodeDisplay.createRoute(orderId)) {
                    popUpTo(VendingDestination.Machines.route)
                }
            },
            onNavigateToTopUp = onNavigateToTopUp,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(
        route = VendingDestination.CodeDisplay.route,
        arguments = listOf(navArgument("orderId") { type = NavType.StringType })
    ) { backStackEntry ->
        val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
        CodeDisplayScreen(
            orderId = orderId,
            onNavigateBack = {
                navController.popBackStack(VendingDestination.Machines.route, false)
            },
            onGetDirections = { machineId -> },
            onHelp = { navController.navigate(VendingDestination.Help.route) }
        )
    }

    composable(VendingDestination.OrderHistory.route) {
        OrderHistoryScreen(
            onOrderClick = { orderId ->
                navController.navigate(VendingDestination.CodeDisplay.createRoute(orderId))
            },
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(VendingDestination.Help.route) {
        VendingHelpScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
