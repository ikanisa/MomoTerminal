package com.momoterminal

import com.momoterminal.api.NfcPaymentData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for NfcPaymentData.
 */
class NfcPaymentDataTest {

    @Test
    fun `create MTN USSD string`() {
        val merchantCode = "0244123456"
        val amount = 50.00
        
        val ussdString = NfcPaymentData.createMtnUssd(merchantCode, amount)
        
        assertEquals("*170*1*1*0244123456*50.00#", ussdString)
    }

    @Test
    fun `create Vodafone USSD string`() {
        val merchantCode = "0201234567"
        val amount = 100.00
        
        val ussdString = NfcPaymentData.createVodafoneUssd(merchantCode, amount)
        
        assertEquals("*110*1*0201234567*100.00#", ussdString)
    }

    @Test
    fun `create AirtelTigo USSD string`() {
        val merchantCode = "0271234567"
        val amount = 75.50
        
        val ussdString = NfcPaymentData.createAirtelTigoUssd(merchantCode, amount)
        
        assertEquals("*500*1*0271234567*75.50#", ussdString)
    }

    @Test
    fun `payment data to NDEF payload contains tel prefix`() {
        val paymentData = NfcPaymentData(
            amount = 50.00,
            merchantCode = "1234567890",
            ussdCode = "*170*1*1*1234567890*50.00#"
        )
        
        val payload = paymentData.toNdefPayload()
        
        assertTrue(payload.startsWith("tel:"))
    }

    @Test
    fun `payment data default currency is GHS`() {
        val paymentData = NfcPaymentData(
            amount = 50.00,
            merchantCode = "123",
            ussdCode = "*170#"
        )
        
        assertEquals("GHS", paymentData.currency)
    }

    @Test
    fun `payment data timestamp is set`() {
        val beforeTime = System.currentTimeMillis()
        
        val paymentData = NfcPaymentData(
            amount = 50.00,
            merchantCode = "123",
            ussdCode = "*170#"
        )
        
        val afterTime = System.currentTimeMillis()
        
        assertTrue(paymentData.timestamp >= beforeTime)
        assertTrue(paymentData.timestamp <= afterTime)
    }
}
