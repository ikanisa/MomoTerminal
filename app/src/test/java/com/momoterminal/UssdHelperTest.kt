package com.momoterminal

import com.momoterminal.feature.nfc.NfcPaymentData
import com.momoterminal.ussd.UssdHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for UssdHelper.
 */
class UssdHelperTest {

    @Test
    fun `generate MTN USSD code with valid inputs`() {
        val ussdCode = UssdHelper.generateUssdCode(
            NfcPaymentData.Provider.MTN,
            "12345",
            5000.0
        )
        assertTrue(ussdCode.contains("12345"))
        assertTrue(ussdCode.contains("5000"))
        assertTrue(ussdCode.startsWith("*"))
        assertTrue(ussdCode.endsWith("#"))
    }

    @Test
    fun `generate Orange USSD code with valid inputs`() {
        val ussdCode = UssdHelper.generateUssdCode(
            NfcPaymentData.Provider.ORANGE,
            "MERCH001",
            10000.0
        )
        assertTrue(ussdCode.contains("MERCH001"))
        assertTrue(ussdCode.contains("10000"))
    }

    @Test
    fun `generate Airtel USSD code with valid inputs`() {
        val ussdCode = UssdHelper.generateUssdCode(
            NfcPaymentData.Provider.AIRTEL,
            "55555",
            2500.0
        )
        assertTrue(ussdCode.contains("55555"))
        assertTrue(ussdCode.contains("2500"))
    }

    @Test
    fun `create payment data returns valid object`() {
        val paymentData = UssdHelper.createPaymentData(
            NfcPaymentData.Provider.MTN,
            "MERCH001",
            7500.0,
            "RWF"
        )
        
        assertEquals(750000L, paymentData.amountInMinorUnits)
        assertEquals(7500.0, paymentData.getDisplayAmount(), 0.01)
        assertEquals("MERCH001", paymentData.merchantPhone)
        assertEquals("RWF", paymentData.currency)
    }
}
