package com.momoterminal

import com.momoterminal.nfc.NfcPaymentData
import com.momoterminal.ussd.UssdHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for UssdHelper.
 * 
 * Note: Amounts are stored in pesewas (smallest currency unit) to avoid
 * floating-point precision errors. 1 GHS = 100 pesewas.
 */
class UssdHelperTest {

    @Test
    fun `generate MTN USSD code with valid inputs`() {
        val merchantCode = "1234567890"
        val amountInPesewas = 5000L // 50.00 GHS
        
        val ussdCode = UssdHelper.generateUssdCode(
            UssdHelper.Provider.MTN_MOMO,
            merchantCode,
            amountInPesewas.toDouble()
        )
        
        assertEquals("*170*1*1*1234567890*50.00#", ussdCode)
    }

    @Test
    fun `generate Vodafone USSD code with valid inputs`() {
        val merchantCode = "0987654321"
        val amountInPesewas = 10050L // 100.50 GHS
        
        val ussdCode = UssdHelper.generateUssdCode(
            UssdHelper.Provider.VODAFONE_CASH,
            merchantCode,
            amountInPesewas.toDouble()
        )
        
        assertEquals("*110*1*0987654321*100.50#", ussdCode)
    }

    @Test
    fun `generate AirtelTigo USSD code with valid inputs`() {
        val merchantCode = "5555555555"
        val amountInPesewas = 2575L // 25.75 GHS
        
        val ussdCode = UssdHelper.generateUssdCode(
            UssdHelper.Provider.AIRTELTIGO_MONEY,
            merchantCode,
            amountInPesewas.toDouble()
        )
        
        assertEquals("*500*1*5555555555*25.75#", ussdCode)
    }

    @Test
    fun `generate custom USSD code`() {
        val baseCode = "*999*"
        val merchantCode = "12345"
        val amountInPesewas = 1000L // 10.00 GHS
        
        val ussdCode = UssdHelper.generateCustomUssd(baseCode, merchantCode, amountInPesewas.toDouble())
        
        assertEquals("*999*12345*10.00#", ussdCode)
    }

    @Test
    fun `amount formatting with decimal places`() {
        val merchantCode = "123"
        val amountInPesewas = 500L // 5.00 GHS
        
        val ussdCode = UssdHelper.generateUssdCode(
            UssdHelper.Provider.MTN_MOMO,
            merchantCode,
            amountInPesewas.toDouble()
        )
        
        assertTrue(ussdCode.contains("5.00"))
    }

    @Test
    fun `create payment data returns valid object`() {
        val merchantCode = "MERCH001"
        val amountInPesewas = 7500L // 75.00 GHS
        
        val paymentData = UssdHelper.createPaymentData(
            UssdHelper.Provider.MTN_MOMO,
            merchantCode,
            amountInPesewas.toDouble()
        )
        
        assertEquals(7500L, paymentData.amountInPesewas)
        assertEquals(75.00, paymentData.getDisplayAmount(), 0.01)
        assertEquals(merchantCode, paymentData.merchantPhone)
        assertEquals("GHS", paymentData.currency)
        assertEquals(NfcPaymentData.Provider.MTN, paymentData.provider)
    }
    
    @Test
    fun `create payment data maps provider correctly`() {
        val paymentDataMtn = UssdHelper.createPaymentData(
            UssdHelper.Provider.MTN_MOMO,
            "123",
            1000.0
        )
        assertEquals(NfcPaymentData.Provider.MTN, paymentDataMtn.provider)
        
        val paymentDataVoda = UssdHelper.createPaymentData(
            UssdHelper.Provider.VODAFONE_CASH,
            "123",
            1000.0
        )
        assertEquals(NfcPaymentData.Provider.VODAFONE, paymentDataVoda.provider)
        
        val paymentDataAt = UssdHelper.createPaymentData(
            UssdHelper.Provider.AIRTELTIGO_MONEY,
            "123",
            1000.0
        )
        assertEquals(NfcPaymentData.Provider.AIRTEL_TIGO, paymentDataAt.provider)
    }
}
