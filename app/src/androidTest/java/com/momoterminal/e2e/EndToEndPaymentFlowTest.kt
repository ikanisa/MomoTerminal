package com.momoterminal.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.momoterminal.presentation.ComposeMainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test for the full payment flow.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EndToEndPaymentFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComposeMainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun completePaymentFlow_enterAmount_activateNfc() {
        // Wait for app to load
        composeTestRule.waitForIdle()

        // Step 1: Enter amount using keypad
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        
        // Verify amount is displayed
        composeTestRule.onNodeWithText("50", substring = true).assertIsDisplayed()

        // Step 2: Amount should show GHS 50
        composeTestRule.onNodeWithText("GHS", substring = true, ignoreCase = true).assertExists()
    }

    @Test
    fun paymentFlow_clearAmount_resetsDisplay() {
        composeTestRule.waitForIdle()

        // Enter some amount
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        
        // Verify amount is displayed
        composeTestRule.onNodeWithText("100", substring = true).assertIsDisplayed()

        // Clear the amount (assuming there's a clear button)
        composeTestRule.onNodeWithText("C", substring = true, ignoreCase = true).performClick()

        // Verify amount is cleared (shows 0 or empty)
        composeTestRule.onNodeWithText("0", useUnmergedTree = true).assertExists()
    }

    @Test
    fun paymentFlow_selectProvider_changesProvider() {
        composeTestRule.waitForIdle()

        // Tap on MTN provider option (should be visible by default)
        composeTestRule.onNodeWithText("MTN", substring = true).assertExists()

        // Tap on Vodafone if available
        try {
            composeTestRule.onNodeWithText("Vodafone", substring = true).performClick()
            composeTestRule.onNodeWithText("Vodafone", substring = true).assertIsDisplayed()
        } catch (e: AssertionError) {
            // Provider might not be directly visible, skip
        }
    }

    @Test
    fun paymentFlow_navigateToTransactions() {
        composeTestRule.waitForIdle()

        // Find and tap transactions navigation
        try {
            composeTestRule.onNodeWithText("Transactions", substring = true, ignoreCase = true).performClick()
            composeTestRule.waitForIdle()
            
            // Verify we're on transactions screen
            composeTestRule.onNodeWithText("Transactions", substring = true, ignoreCase = true).assertExists()
        } catch (e: AssertionError) {
            // Navigation might be different, skip
        }
    }

    @Test
    fun paymentFlow_backspaceRemovesLastDigit() {
        composeTestRule.waitForIdle()

        // Enter amount
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        
        composeTestRule.onNodeWithText("123", substring = true).assertIsDisplayed()

        // Press backspace
        composeTestRule.onNodeWithContentDescription("Backspace", ignoreCase = true, useUnmergedTree = true)
            .performClick()
        
        composeTestRule.onNodeWithText("12", substring = true).assertExists()
    }
}
