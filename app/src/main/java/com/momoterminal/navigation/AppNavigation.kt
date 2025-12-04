package com.momoterminal.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.momoterminal.presentation.navigation.NavGraph
import com.momoterminal.presentation.navigation.Screen

/**
 * Legacy navigation wrapper that now delegates to the main NavGraph.
 * Keeping this for compatibility with older entry points.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavGraph(
        navController = navController,
        startDestination = startDestination
    )
}
