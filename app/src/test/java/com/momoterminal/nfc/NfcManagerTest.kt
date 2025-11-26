package com.momoterminal.nfc

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import android.content.Context
import io.mockk.every

/**
 * Unit tests for NfcManager.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NfcManagerTest {

    private lateinit var context: Context
    private lateinit var nfcManager: NfcManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        // Note: NfcManager requires Android context, so we'll test the non-Android parts
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `NfcPaymentData isValid returns true for valid data`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amount = "50",
            currency = "GHS",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertThat(paymentData.isValid()).isTrue()
    }

    @Test
    fun `NfcPaymentData isValid returns false for empty merchant phone`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "",
            amount = "50",
            currency = "GHS",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertThat(paymentData.isValid()).isFalse()
    }

    @Test
    fun `NfcPaymentData isValid returns false for empty amount`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amount = "",
            currency = "GHS",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertThat(paymentData.isValid()).isFalse()
    }

    @Test
    fun `NfcPaymentData isValid returns false for zero amount`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amount = "0",
            currency = "GHS",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertThat(paymentData.isValid()).isFalse()
    }

    @Test
    fun `NfcPaymentData isValid returns false for negative amount`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amount = "-50",
            currency = "GHS",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertThat(paymentData.isValid()).isFalse()
    }

    @Test
    fun `NfcPaymentData toPaymentUri generates correct MTN URI`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amount = "50.00",
            currency = "GHS",
            provider = NfcPaymentData.Provider.MTN
        )
        
        val uri = paymentData.toPaymentUri()
        assertThat(uri).startsWith("tel:")
        assertThat(uri).contains("*170*")
    }

    @Test
    fun `NfcPaymentData toPaymentUri generates correct Vodafone URI`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0201234567",
            amount = "100.00",
            currency = "GHS",
            provider = NfcPaymentData.Provider.VODAFONE
        )
        
        val uri = paymentData.toPaymentUri()
        assertThat(uri).startsWith("tel:")
        assertThat(uri).contains("*110*")
    }

    @Test
    fun `NfcPaymentData toPaymentUri generates correct AirtelTigo URI`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0271234567",
            amount = "75.00",
            currency = "GHS",
            provider = NfcPaymentData.Provider.AIRTELTIGO
        )
        
        val uri = paymentData.toPaymentUri()
        assertThat(uri).startsWith("tel:")
        assertThat(uri).contains("*500*")
    }

    @Test
    fun `NfcPaymentData toUssdString generates correct USSD format`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amount = "50.00",
            currency = "GHS",
            provider = NfcPaymentData.Provider.MTN
        )
        
        val ussd = paymentData.toUssdString()
        assertThat(ussd).isNotEmpty()
        assertThat(ussd).endsWith("#")
    }

    @Test
    fun `NfcState Ready is initial state`() {
        assertThat(NfcState.Ready).isInstanceOf(NfcState::class.java)
    }

    @Test
    fun `NfcState Active contains payment data`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amount = "50",
            currency = "GHS",
            provider = NfcPaymentData.Provider.MTN
        )
        
        val activeState = NfcState.Active(paymentData)
        assertThat(activeState.paymentData).isEqualTo(paymentData)
    }

    @Test
    fun `NfcState Success contains transaction ID`() {
        val successState = NfcState.Success("TXN-12345678")
        assertThat(successState.transactionId).isEqualTo("TXN-12345678")
    }

    @Test
    fun `NfcState Error contains message and code`() {
        val errorState = NfcState.Error("Connection failed", NfcErrorCode.CONNECTION_LOST)
        assertThat(errorState.message).isEqualTo("Connection failed")
        assertThat(errorState.code).isEqualTo(NfcErrorCode.CONNECTION_LOST)
    }

    @Test
    fun `NfcState Processing contains progress`() {
        val processingState = NfcState.Processing(0.5f)
        assertThat(processingState.progress).isEqualTo(0.5f)
    }

    @Test
    fun `NfcErrorCode has correct descriptions`() {
        assertThat(NfcErrorCode.INVALID_AMOUNT.description).isNotEmpty()
        assertThat(NfcErrorCode.CONNECTION_LOST.description).isNotEmpty()
        assertThat(NfcErrorCode.TIMEOUT.description).isNotEmpty()
        assertThat(NfcErrorCode.UNKNOWN.description).isNotEmpty()
    }
}

/**
 * Additional NfcPaymentData Provider tests.
 */
class NfcPaymentDataProviderTest {

    @Test
    fun `Provider MTN has correct USSD prefix`() {
        assertThat(NfcPaymentData.Provider.MTN.ussdPrefix).isEqualTo("*170*1*1*")
    }

    @Test
    fun `Provider VODAFONE has correct USSD prefix`() {
        assertThat(NfcPaymentData.Provider.VODAFONE.ussdPrefix).isEqualTo("*110*1*")
    }

    @Test
    fun `Provider AIRTELTIGO has correct USSD prefix`() {
        assertThat(NfcPaymentData.Provider.AIRTELTIGO.ussdPrefix).isEqualTo("*500*1*")
    }

    @Test
    fun `Provider displayName is not empty`() {
        NfcPaymentData.Provider.entries.forEach { provider ->
            assertThat(provider.displayName).isNotEmpty()
        }
    }

    @Test
    fun `all providers have unique USSD prefixes`() {
        val prefixes = NfcPaymentData.Provider.entries.map { it.ussdPrefix }
        assertThat(prefixes.toSet()).hasSize(NfcPaymentData.Provider.entries.size)
    }
}
