package com.momoterminal

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
            UssdHelper.Provider.MTN_MOMO,
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
            UssdHelper.Provider.VODAFONE_CASH,
            merchantCode,
            amount
        )
        
        assertEquals("*110*1*0987654321*100.50#", ussdCode)
    }

    @Test
    fun `generate AirtelTigo USSD code with valid inputs`() {
        val merchantCode = "5555555555"
        val amount = 25.75
        
        val ussdCode = UssdHelper.generateUssdCode(
            UssdHelper.Provider.AIRTELTIGO_MONEY,
            merchantCode,
            amount
        )
        
        assertEquals("*500*1*5555555555*25.75#", ussdCode)
    }

    @Test
    fun `generate custom USSD code`() {
        val baseCode = "*999*"
        val merchantCode = "12345"
        val amount = 10.00
        
        val ussdCode = UssdHelper.generateCustomUssd(baseCode, merchantCode, amount)
        
        assertEquals("*999*12345*10.00#", ussdCode)
    }

    @Test
    fun `amount formatting with decimal places`() {
        val merchantCode = "123"
        val amount = 5.0
        
        val ussdCode = UssdHelper.generateUssdCode(
            UssdHelper.Provider.MTN_MOMO,
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
            UssdHelper.Provider.MTN_MOMO,
            merchantCode,
            amount
        )
        
        assertEquals(75.00, paymentData.amount, 0.01)
        assertEquals(merchantCode, paymentData.merchantCode)
        assertEquals("GHS", paymentData.currency)
        assertTrue(paymentData.ussdCode.contains("*170*1*1*"))
    }
}
