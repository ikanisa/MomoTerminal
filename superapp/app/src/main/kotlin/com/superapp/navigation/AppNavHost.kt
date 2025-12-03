package com.superapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.superapp.feature.featurea.FeatureAScreen

sealed class Screen(val route: String) {
    data object FeatureA : Screen("feature_a")
    data object FeatureADetail : Screen("feature_a/{entityId}") {
        fun createRoute(entityId: String) = "feature_a/$entityId"
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.FeatureA.route
    ) {
        composable(Screen.FeatureA.route) {
            FeatureAScreen(
                onNavigateToDetail = { entityId ->
                    navController.navigate(Screen.FeatureADetail.createRoute(entityId))
                }
            )
        }

        composable(Screen.FeatureADetail.route) {
            // Detail screen placeholder
        }
    }
}
