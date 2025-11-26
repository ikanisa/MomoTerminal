package com.momoterminal.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.momoterminal.presentation.navigation.MomoNavHost
import com.momoterminal.presentation.navigation.Screen
import com.momoterminal.presentation.theme.MomoTerminalTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Navigation tests for app navigation flows.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun appStartsAtHomeScreen() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            
            MomoTerminalTheme {
                MomoNavHost(navController = navController)
            }
        }

        // Verify we start at home/terminal screen
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.Terminal.route)
    }

    @Test
    fun navigateToTransactionsScreen() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            
            MomoTerminalTheme {
                MomoNavHost(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Navigate to transactions
        navController.navigate(Screen.Transactions.route)
        composeTestRule.waitForIdle()

        // Verify current destination
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.Transactions.route)
    }

    @Test
    fun navigateToSettingsScreen() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            
            MomoTerminalTheme {
                MomoNavHost(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Navigate to settings
        navController.navigate(Screen.Settings.route)
        composeTestRule.waitForIdle()

        // Verify current destination
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.Settings.route)
    }

    @Test
    fun navigateBackFromTransactions() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            
            MomoTerminalTheme {
                MomoNavHost(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Navigate to transactions
        navController.navigate(Screen.Transactions.route)
        composeTestRule.waitForIdle()

        // Navigate back
        navController.popBackStack()
        composeTestRule.waitForIdle()

        // Should be back at terminal
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.Terminal.route)
    }

    @Test
    fun backStackIsCorrectAfterMultipleNavigations() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            
            MomoTerminalTheme {
                MomoNavHost(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Navigate: Terminal -> Transactions -> Settings
        navController.navigate(Screen.Transactions.route)
        composeTestRule.waitForIdle()
        
        navController.navigate(Screen.Settings.route)
        composeTestRule.waitForIdle()

        // Verify current is Settings
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.Settings.route)

        // Pop back should go to Transactions
        navController.popBackStack()
        composeTestRule.waitForIdle()
        
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.Transactions.route)

        // Pop back should go to Terminal
        navController.popBackStack()
        composeTestRule.waitForIdle()
        
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.Terminal.route)
    }
}
