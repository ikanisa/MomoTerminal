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
 * 
 * Note: Amounts are stored in pesewas (smallest currency unit) to avoid
 * floating-point precision errors. 1 GHS = 100 pesewas.
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
            amountInMinorUnits = 5000L, // 50 GHS
            currency = "RWF",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertThat(paymentData.isValid()).isTrue()
    }

    @Test
    fun `NfcPaymentData isValid returns false for empty merchant phone`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "",
            amountInMinorUnits = 5000L,
            currency = "RWF",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertThat(paymentData.isValid()).isFalse()
    }

    @Test
    fun `NfcPaymentData isValid returns false for zero amount`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 0L,
            currency = "RWF",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertThat(paymentData.isValid()).isFalse()
    }

    @Test
    fun `NfcPaymentData isValid returns false for negative amount`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = -5000L,
            currency = "RWF",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertThat(paymentData.isValid()).isFalse()
    }

    @Test
    fun `NfcPaymentData toPaymentUri generates correct URI format`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 5000L,
            currency = "RWF",
            provider = NfcPaymentData.Provider.MTN
        )
        
        val uri = paymentData.toPaymentUri()
        assertThat(uri).startsWith("momo://pay?")
        assertThat(uri).contains("to=0244123456")
        assertThat(uri).contains("amount=50.00")
        assertThat(uri).contains("currency=RWF")
        assertThat(uri).contains("provider=MTN")
    }

    @Test
    fun `NfcPaymentData toUssdString generates valid USSD`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 5000L,
            currency = "RWF",
            provider = NfcPaymentData.Provider.MTN
        )
        
        val ussd = paymentData.toUssdString()
        assertThat(ussd).startsWith("tel:")
        assertThat(ussd).contains("0244123456")
        assertThat(ussd).contains("50.00")
    }

    @Test
    fun `NfcPaymentData toUssdString contains merchant phone`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0201234567",
            amountInMinorUnits = 10000L,
            currency = "RWF",
            provider = NfcPaymentData.Provider.VODAFONE
        )
        
        val ussd = paymentData.toUssdString()
        assertThat(ussd).startsWith("tel:")
        assertThat(ussd).contains("0201234567")
    }

    @Test
    fun `NfcPaymentData toUssdString contains formatted amount`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0271234567",
            amountInMinorUnits = 7500L,
            currency = "RWF",
            provider = NfcPaymentData.Provider.AIRTEL
        )
        
        val ussd = paymentData.toUssdString()
        assertThat(ussd).startsWith("tel:")
        assertThat(ussd).contains("75.00")
    }

    @Test
    fun `NfcPaymentData currency is set correctly`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 5000L,
            currency = "RWF"
        )
        
        assertThat(paymentData.currency).isEqualTo("RWF")
    }

    @Test
    fun `NfcPaymentData default provider is MTN`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 5000L,
            currency = "RWF"
        )
        
        assertThat(paymentData.provider).isEqualTo(NfcPaymentData.Provider.MTN)
    }

    @Test
    fun `NfcPaymentData timestamp is set`() {
        val beforeTime = System.currentTimeMillis()
        
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 5000L,
            currency = "RWF"
        )
        
        val afterTime = System.currentTimeMillis()
        
        assertThat(paymentData.timestamp).isAtLeast(beforeTime)
        assertThat(paymentData.timestamp).isAtMost(afterTime)
    }

    @Test
    fun `NfcState Ready is initial state`() {
        assertThat(NfcState.Ready).isInstanceOf(NfcState::class.java)
    }

    @Test
    fun `NfcState Active contains payment data`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 5000L,
            currency = "RWF",
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
        assertThat(NfcErrorCode.UNKNOWN_ERROR.description).isNotEmpty()
    }

    @Test
    fun `NfcPaymentData with reference includes it in URI`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 5000L,
            currency = "RWF",
            reference = "REF123"
        )
        
        val uri = paymentData.toPaymentUri()
        assertThat(uri).contains("ref=REF123")
    }

    @Test
    fun `NfcPaymentData without reference omits it from URI`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 5000L,
            currency = "RWF",
            reference = null
        )
        
        val uri = paymentData.toPaymentUri()
        assertThat(uri).doesNotContain("ref=")
    }
    
    @Test
    fun `NfcPaymentData getDisplayAmount converts minor units to main`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 12345L,
            currency = "RWF"
        )
        
        assertThat(paymentData.getDisplayAmount()).isWithin(0.001).of(123.45)
    }
    
    @Test
    fun `NfcPaymentData toMinorUnits converts correctly`() {
        assertThat(NfcPaymentData.toMinorUnits(50.00)).isEqualTo(5000L)
        assertThat(NfcPaymentData.toMinorUnits(123.45)).isEqualTo(12345L)
    }
}

/**
 * Additional NfcPaymentData Provider tests.
 */
class NfcPaymentDataProviderTest {

    @Test
    fun `Provider fromString returns MTN for mtn`() {
        val provider = NfcPaymentData.Provider.fromString("mtn")
        assertThat(provider).isEqualTo(NfcPaymentData.Provider.MTN)
    }

    @Test
    fun `Provider fromString returns VODAFONE for vodafone`() {
        val provider = NfcPaymentData.Provider.fromString("vodafone")
        assertThat(provider).isEqualTo(NfcPaymentData.Provider.VODAFONE)
    }

    @Test
    fun `Provider fromString returns MTN as default for unknown`() {
        val provider = NfcPaymentData.Provider.fromString("unknown")
        assertThat(provider).isEqualTo(NfcPaymentData.Provider.MTN)
    }

    @Test
    fun `Provider displayName is not empty`() {
        NfcPaymentData.Provider.entries.forEach { provider ->
            assertThat(provider.displayName).isNotEmpty()
        }
    }

    @Test
    fun `Provider colorHex is valid hex format`() {
        NfcPaymentData.Provider.entries.forEach { provider ->
            assertThat(provider.colorHex).startsWith("#")
            assertThat(provider.colorHex).hasLength(7)
        }
    }
}
