package com.momoterminal

import com.momoterminal.nfc.NfcPaymentData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for NfcPaymentData.
 */
class NfcPaymentDataTest {

    @Test
    fun `create MTN payment data`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 5000L,
            currency = "GHS",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertEquals(50.00, paymentData.getDisplayAmount(), 0.001)
        assertTrue(paymentData.toUssdString().startsWith("tel:"))
    }

    @Test
    fun `create Vodafone payment data`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0201234567",
            amountInMinorUnits = 10000L,
            currency = "GHS",
            provider = NfcPaymentData.Provider.VODAFONE
        )
        
        assertEquals(100.00, paymentData.getDisplayAmount(), 0.001)
    }

    @Test
    fun `create Airtel payment data`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0271234567",
            amountInMinorUnits = 7550L,
            currency = "RWF",
            provider = NfcPaymentData.Provider.AIRTEL
        )
        
        assertEquals(75.50, paymentData.getDisplayAmount(), 0.001)
    }

    @Test
    fun `payment data to USSD string contains tel prefix`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "1234567890",
            amountInMinorUnits = 5000L,
            currency = "GHS",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertTrue(paymentData.toUssdString().startsWith("tel:"))
    }

    @Test
    fun `payment data timestamp is set`() {
        val beforeTime = System.currentTimeMillis()
        
        val paymentData = NfcPaymentData(
            merchantPhone = "123",
            amountInMinorUnits = 5000L,
            currency = "RWF"
        )
        
        val afterTime = System.currentTimeMillis()
        
        assertTrue(paymentData.timestamp >= beforeTime)
        assertTrue(paymentData.timestamp <= afterTime)
    }
    
    @Test
    fun `payment data isValid returns true for valid data`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 5000L,
            currency = "GHS",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertTrue(paymentData.isValid())
    }
    
    @Test
    fun `payment data toPaymentUri generates correct format`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 5000L,
            currency = "GHS",
            provider = NfcPaymentData.Provider.MTN
        )
        
        val uri = paymentData.toPaymentUri()
        
        assertTrue(uri.startsWith("momo://pay?"))
        assertTrue(uri.contains("to=0244123456"))
        assertTrue(uri.contains("amount=50.00"))
        assertTrue(uri.contains("provider=MTN"))
    }
    
    @Test
    fun `getDisplayAmount converts minor units to main currency unit`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInMinorUnits = 12345L,
            currency = "GHS"
        )
        
        assertEquals(123.45, paymentData.getDisplayAmount(), 0.001)
    }
    
    @Test
    fun `toMinorUnits converts double correctly`() {
        assertEquals(5000L, NfcPaymentData.toMinorUnits(50.00))
        assertEquals(7550L, NfcPaymentData.toMinorUnits(75.50))
        assertEquals(1L, NfcPaymentData.toMinorUnits(0.01))
    }
    
    @Test
    fun `fromAmount creates NfcPaymentData correctly`() {
        val paymentData = NfcPaymentData.fromAmount(
            merchantPhone = "0244123456",
            amount = 50.00,
            currency = "RWF",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertEquals(5000L, paymentData.amountInMinorUnits)
        assertEquals(50.00, paymentData.getDisplayAmount(), 0.001)
    }
}
