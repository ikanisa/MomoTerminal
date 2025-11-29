package com.momoterminal.presentation.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.momoterminal.nfc.NfcPaymentData
import com.momoterminal.nfc.NfcState
import com.momoterminal.presentation.screens.terminal.TerminalScreen
import com.momoterminal.presentation.screens.terminal.TerminalViewModel
import com.momoterminal.presentation.theme.MomoTerminalTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for TerminalScreen using Compose testing.
 */
@RunWith(AndroidJUnit4::class)
class TerminalScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val uiState = MutableStateFlow(
        TerminalViewModel.TerminalUiState(
            amount = "",
            selectedProvider = NfcPaymentData.Provider.MTN,
            merchantPhone = "0244123456",
            isConfigured = true,
            isPaymentActive = false
        )
    )
    
    private val nfcState = MutableStateFlow<NfcState>(NfcState.Ready)

    @Test
    fun terminalScreen_displaysNumericKeypad() {
        composeTestRule.setContent {
            MomoTerminalTheme {
                TerminalScreen(
                    uiState = uiState.value,
                    nfcState = nfcState.value,
                    onDigitClick = {},
                    onBackspaceClick = {},
                    onClearClick = {},
                    onProviderSelected = {},
                    onActivatePayment = {},
                    onCancelPayment = {},
                    onSettingsClick = {}
                )
            }
        }

        // Verify number buttons are displayed
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("4").assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        composeTestRule.onNodeWithText("6").assertIsDisplayed()
        composeTestRule.onNodeWithText("7").assertIsDisplayed()
        composeTestRule.onNodeWithText("8").assertIsDisplayed()
        composeTestRule.onNodeWithText("9").assertIsDisplayed()
        composeTestRule.onNodeWithText("0").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_displaysAmountDisplay() {
        uiState.value = uiState.value.copy(amount = "100")
        
        composeTestRule.setContent {
            MomoTerminalTheme {
                TerminalScreen(
                    uiState = uiState.value,
                    nfcState = nfcState.value,
                    onDigitClick = {},
                    onBackspaceClick = {},
                    onClearClick = {},
                    onProviderSelected = {},
                    onActivatePayment = {},
                    onCancelPayment = {},
                    onSettingsClick = {}
                )
            }
        }

        // Amount should be displayed
        composeTestRule.onNodeWithText("100", substring = true).assertIsDisplayed()
    }

    @Test
    fun terminalScreen_digitClick_callsCallback() {
        var clickedDigit = ""
        
        composeTestRule.setContent {
            MomoTerminalTheme {
                TerminalScreen(
                    uiState = uiState.value,
                    nfcState = nfcState.value,
                    onDigitClick = { clickedDigit = it },
                    onBackspaceClick = {},
                    onClearClick = {},
                    onProviderSelected = {},
                    onActivatePayment = {},
                    onCancelPayment = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("5").performClick()
        
        assert(clickedDigit == "5")
    }

    @Test
    fun terminalScreen_displaysMerchantPhone() {
        composeTestRule.setContent {
            MomoTerminalTheme {
                TerminalScreen(
                    uiState = uiState.value,
                    nfcState = nfcState.value,
                    onDigitClick = {},
                    onBackspaceClick = {},
                    onClearClick = {},
                    onProviderSelected = {},
                    onActivatePayment = {},
                    onCancelPayment = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("0244123456", substring = true).assertIsDisplayed()
    }

    @Test
    fun terminalScreen_showsNfcReadyState() {
        composeTestRule.setContent {
            MomoTerminalTheme {
                TerminalScreen(
                    uiState = uiState.value,
                    nfcState = NfcState.Ready,
                    onDigitClick = {},
                    onBackspaceClick = {},
                    onClearClick = {},
                    onProviderSelected = {},
                    onActivatePayment = {},
                    onCancelPayment = {},
                    onSettingsClick = {}
                )
            }
        }

        // NFC ready indicator should be visible
        composeTestRule.onNodeWithText("NFC", substring = true, ignoreCase = true).assertExists()
    }

    @Test
    fun terminalScreen_showsProviderSelector() {
        composeTestRule.setContent {
            MomoTerminalTheme {
                TerminalScreen(
                    uiState = uiState.value,
                    nfcState = nfcState.value,
                    onDigitClick = {},
                    onBackspaceClick = {},
                    onClearClick = {},
                    onProviderSelected = {},
                    onActivatePayment = {},
                    onCancelPayment = {},
                    onSettingsClick = {}
                )
            }
        }

        // Provider options should be available
        composeTestRule.onNodeWithText("MTN", substring = true).assertExists()
    }

    @Test
    fun terminalScreen_activePaymentShowsCancelButton() {
        uiState.value = uiState.value.copy(
            amount = "50",
            isPaymentActive = true
        )
        nfcState.value = NfcState.Active(
            NfcPaymentData(
                merchantPhone = "0244123456",
                amountInPesewas = 5000L, // 50 GHS in pesewas
                currency = "GHS",
                provider = NfcPaymentData.Provider.MTN
            )
        )
        
        composeTestRule.setContent {
            MomoTerminalTheme {
                TerminalScreen(
                    uiState = uiState.value,
                    nfcState = nfcState.value,
                    onDigitClick = {},
                    onBackspaceClick = {},
                    onClearClick = {},
                    onProviderSelected = {},
                    onActivatePayment = {},
                    onCancelPayment = {},
                    onSettingsClick = {}
                )
            }
        }

        // Cancel button should be visible during active payment
        composeTestRule.onNodeWithText("Cancel", substring = true, ignoreCase = true).assertExists()
    }
}
