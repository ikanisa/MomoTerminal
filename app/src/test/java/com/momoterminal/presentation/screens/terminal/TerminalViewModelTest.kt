package com.momoterminal.presentation.screens.terminal

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.momoterminal.config.AppConfig
import com.momoterminal.nfc.NfcManager
import com.momoterminal.nfc.NfcPaymentData
import com.momoterminal.nfc.NfcState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TerminalViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TerminalViewModelTest {

    private lateinit var nfcManager: NfcManager
    private lateinit var appConfig: AppConfig
    private lateinit var viewModel: TerminalViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val nfcStateFlow = MutableStateFlow<NfcState>(NfcState.Ready)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        nfcManager = mockk(relaxed = true)
        appConfig = mockk(relaxed = true)
        
        every { nfcManager.nfcState } returns nfcStateFlow
        every { nfcManager.isNfcAvailable() } returns true
        coEvery { appConfig.getMerchantPhone() } returns "0244123456"
        coEvery { appConfig.isConfigured() } returns true
        
        viewModel = TerminalViewModel(nfcManager, appConfig)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty amount`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertThat(viewModel.uiState.value.amount).isEmpty()
    }

    @Test
    fun `onDigitClick appends digit to amount`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onDigitClick("5")
        assertThat(viewModel.uiState.value.amount).isEqualTo("5")
        
        viewModel.onDigitClick("0")
        assertThat(viewModel.uiState.value.amount).isEqualTo("50")
    }

    @Test
    fun `onDigitClick prevents leading zeros`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onDigitClick("0")
        assertThat(viewModel.uiState.value.amount).isEmpty()
    }

    @Test
    fun `onDigitClick limits amount length to 10 characters`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Add 10 digits
        repeat(10) { viewModel.onDigitClick("1") }
        assertThat(viewModel.uiState.value.amount).hasLength(10)
        
        // Try to add 11th digit
        viewModel.onDigitClick("2")
        assertThat(viewModel.uiState.value.amount).hasLength(10)
    }

    @Test
    fun `onBackspaceClick removes last digit`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onDigitClick("5")
        viewModel.onDigitClick("0")
        assertThat(viewModel.uiState.value.amount).isEqualTo("50")
        
        viewModel.onBackspaceClick()
        assertThat(viewModel.uiState.value.amount).isEqualTo("5")
    }

    @Test
    fun `onBackspaceClick does nothing on empty amount`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onBackspaceClick()
        assertThat(viewModel.uiState.value.amount).isEmpty()
    }

    @Test
    fun `onClearClick clears amount`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onDigitClick("1")
        viewModel.onDigitClick("0")
        viewModel.onDigitClick("0")
        assertThat(viewModel.uiState.value.amount).isEqualTo("100")
        
        viewModel.onClearClick()
        assertThat(viewModel.uiState.value.amount).isEmpty()
    }

    @Test
    fun `onProviderSelected updates selected provider`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onProviderSelected(NfcPaymentData.Provider.VODAFONE)
        assertThat(viewModel.uiState.value.selectedProvider).isEqualTo(NfcPaymentData.Provider.VODAFONE)
        
        viewModel.onProviderSelected(NfcPaymentData.Provider.AIRTEL)
        assertThat(viewModel.uiState.value.selectedProvider).isEqualTo(NfcPaymentData.Provider.AIRTEL)
    }

    @Test
    fun `activatePayment calls nfcManager when amount is valid`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onDigitClick("5")
        viewModel.onDigitClick("0")
        viewModel.activatePayment()
        
        verify { nfcManager.activatePayment(any()) }
    }

    @Test
    fun `activatePayment does not call nfcManager when amount is empty`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.activatePayment()
        
        verify(exactly = 0) { nfcManager.activatePayment(any()) }
    }

    @Test
    fun `activatePayment sets isPaymentActive to true`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onDigitClick("5")
        viewModel.onDigitClick("0")
        viewModel.activatePayment()
        
        assertThat(viewModel.uiState.value.isPaymentActive).isTrue()
    }

    @Test
    fun `cancelPayment calls nfcManager cancelPayment`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.cancelPayment()
        
        verify { nfcManager.cancelPayment() }
    }

    @Test
    fun `cancelPayment sets isPaymentActive to false`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onDigitClick("5")
        viewModel.onDigitClick("0")
        viewModel.activatePayment()
        assertThat(viewModel.uiState.value.isPaymentActive).isTrue()
        
        viewModel.cancelPayment()
        assertThat(viewModel.uiState.value.isPaymentActive).isFalse()
    }

    @Test
    fun `isNfcAvailable returns nfcManager availability`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        every { nfcManager.isNfcAvailable() } returns true
        assertThat(viewModel.isNfcAvailable()).isTrue()
        
        every { nfcManager.isNfcAvailable() } returns false
        assertThat(viewModel.isNfcAvailable()).isFalse()
    }

    @Test
    fun `isAmountValid returns false for empty amount`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertThat(viewModel.isAmountValid()).isFalse()
    }

    @Test
    fun `isAmountValid returns true for positive amount`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onDigitClick("5")
        assertThat(viewModel.isAmountValid()).isTrue()
    }

    @Test
    fun `nfcState emits values from nfcManager`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.nfcState.test {
            assertThat(awaitItem()).isEqualTo(NfcState.Ready)
            
            nfcStateFlow.value = NfcState.Activating
            assertThat(awaitItem()).isEqualTo(NfcState.Activating)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState loads config on initialization`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertThat(viewModel.uiState.value.merchantPhone).isEqualTo("0244123456")
        assertThat(viewModel.uiState.value.isConfigured).isTrue()
    }

    @Test
    fun `default provider is MTN`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertThat(viewModel.uiState.value.selectedProvider).isEqualTo(NfcPaymentData.Provider.MTN)
    }
}
