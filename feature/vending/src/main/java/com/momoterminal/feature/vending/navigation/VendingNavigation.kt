package com.momoterminal.feature.vending.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.momoterminal.feature.vending.ui.code.CodeDisplayScreen
import com.momoterminal.feature.vending.ui.detail.MachineDetailScreen
import com.momoterminal.feature.vending.ui.event.EventOrderScreen
import com.momoterminal.feature.vending.ui.help.VendingHelpScreen
import com.momoterminal.feature.vending.ui.history.OrderHistoryScreen
import com.momoterminal.feature.vending.ui.machines.MachinesScreen
import com.momoterminal.feature.vending.ui.payment.PaymentConfirmationScreen

sealed class VendingDestination(val route: String) {
    object Root : VendingDestination("vending")
    object Machines : VendingDestination("vending/machines")
    object MachineDetail : VendingDestination("vending/machine/{machineId}") {
        fun createRoute(machineId: String) = "vending/machine/$machineId"
    }
    object Payment : VendingDestination("vending/payment/{machineId}/{productId}") {
        fun createRoute(machineId: String, productId: String) = "vending/payment/$machineId/$productId"
    }
    object CodeDisplay : VendingDestination("vending/code/{orderId}") {
        fun createRoute(orderId: String) = "vending/code/$orderId"
    }
    object OrderHistory : VendingDestination("vending/orders")
    object EventOrder : VendingDestination("vending/event/{eventId}") {
        fun createRoute(eventId: String) = "vending/event/$eventId"
    }
    object Help : VendingDestination("vending/help")
}

fun NavGraphBuilder.vendingNavGraph(
    navController: NavHostController,
    onNavigateBack: () -> Unit
) {
    navigation(
        startDestination = VendingDestination.Machines.route,
        route = VendingDestination.Root.route
    ) {
        composable(VendingDestination.Machines.route) {
            MachinesScreen(
                onNavigateToDetail = { machineId ->
                    navController.navigate(VendingDestination.MachineDetail.createRoute(machineId))
                },
                onNavigateToEventOrder = {
                    navController.navigate(VendingDestination.EventOrder.createRoute("default"))
                },
                onNavigateToHistory = {
                    navController.navigate(VendingDestination.OrderHistory.route)
                },
                onNavigateToHelp = {
                    navController.navigate(VendingDestination.Help.route)
                },
                onNavigateBack = onNavigateBack
            )
        }

        composable(
            route = VendingDestination.MachineDetail.route,
            arguments = listOf(navArgument("machineId") { type = NavType.StringType })
        ) { backStackEntry ->
            val machineId = backStackEntry.arguments?.getString("machineId") ?: return@composable
            MachineDetailScreen(
                machineId = machineId,
                onNavigateToPayment = { mId, productId ->
                    navController.navigate(VendingDestination.Payment.createRoute(mId, productId))
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = VendingDestination.Payment.route,
            arguments = listOf(
                navArgument("machineId") { type = NavType.StringType },
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val machineId = backStackEntry.arguments?.getString("machineId") ?: return@composable
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            PaymentConfirmationScreen(
                machineId = machineId,
                productId = productId,
                onNavigateToCode = { orderId ->
                    navController.navigate(VendingDestination.CodeDisplay.createRoute(orderId)) {
                        popUpTo(VendingDestination.Machines.route)
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = VendingDestination.CodeDisplay.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
            CodeDisplayScreen(
                orderId = orderId,
                onNavigateToHistory = {
                    navController.navigate(VendingDestination.OrderHistory.route) {
                        popUpTo(VendingDestination.Machines.route)
                    }
                },
                onNavigateHome = {
                    navController.popBackStack(VendingDestination.Machines.route, false)
                }
            )
        }

        composable(VendingDestination.OrderHistory.route) {
            OrderHistoryScreen(
                onNavigateToCode = { orderId ->
                    navController.navigate(VendingDestination.CodeDisplay.createRoute(orderId))
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = VendingDestination.EventOrder.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            EventOrderScreen(
                eventId = eventId,
                onNavigateToCode = { orderId ->
                    navController.navigate(VendingDestination.CodeDisplay.createRoute(orderId)) {
                        popUpTo(VendingDestination.Machines.route)
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(VendingDestination.Help.route) {
            VendingHelpScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
