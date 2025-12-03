package com.momoterminal

import com.momoterminal.nfc.NfcPaymentData
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
        val merchantCode = "1234567890"
        val amount = 50.00
        
        val ussdCode = UssdHelper.generateUssdCode(
            NfcPaymentData.Provider.MTN,
            merchantCode,
            amount
        )
        
        assertEquals("*170*1*1*1234567890*50.00#", ussdCode)
    }

    @Test
    fun `generate Vodafone USSD code with valid inputs`() {
        val merchantCode = "0987654321"
        val amount = 100.50
        
        val ussdCode = UssdHelper.generateUssdCode(
            NfcPaymentData.Provider.VODAFONE,
            merchantCode,
            amount
        )
        
        assertEquals("*110*1*0987654321*100.50#", ussdCode)
    }

    @Test
    fun `generate Airtel USSD code with valid inputs`() {
        val merchantCode = "5555555555"
        val amount = 25.75
        
        val ussdCode = UssdHelper.generateUssdCode(
            NfcPaymentData.Provider.AIRTEL,
            merchantCode,
            amount
        )
        
        assertEquals("*500*1*5555555555*25.75#", ussdCode)
    }

    @Test
    fun `amount formatting with decimal places`() {
        val merchantCode = "123"
        val amount = 5.00
        
        val ussdCode = UssdHelper.generateUssdCode(
            NfcPaymentData.Provider.MTN,
            merchantCode,
            amount
        )
        
        assertTrue(ussdCode.contains("5.00"))
    }

    @Test
    fun `create payment data returns valid object`() {
        val merchantCode = "MERCH001"
        val amount = 75.00
        
        val paymentData = UssdHelper.createPaymentData(
            NfcPaymentData.Provider.MTN,
            merchantCode,
            amount,
            "GHS"
        )
        
        assertEquals(7500L, paymentData.amountInMinorUnits)
        assertEquals(75.00, paymentData.getDisplayAmount(), 0.01)
        assertEquals(merchantCode, paymentData.merchantPhone)
        assertEquals("GHS", paymentData.currency)
        assertEquals(NfcPaymentData.Provider.MTN, paymentData.provider)
    }
    
    @Test
    fun `create payment data with different currencies`() {
        val paymentDataRwf = UssdHelper.createPaymentData(
            NfcPaymentData.Provider.MTN,
            "123",
            1000.00,
            "RWF"
        )
        assertEquals("RWF", paymentDataRwf.currency)
        assertEquals(100000L, paymentDataRwf.amountInMinorUnits)
        
        val paymentDataCdf = UssdHelper.createPaymentData(
            NfcPaymentData.Provider.VODACOM,
            "456",
            500.00,
            "CDF"
        )
        assertEquals("CDF", paymentDataCdf.currency)
    }
}
