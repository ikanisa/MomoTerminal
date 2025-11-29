package com.momoterminal

import com.momoterminal.nfc.NfcPaymentData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for NfcPaymentData.
 * 
 * Note: Amounts are stored in pesewas (smallest currency unit) to avoid
 * floating-point precision errors. 1 GHS = 100 pesewas.
 */
class NfcPaymentDataTest {

    @Test
    fun `create MTN USSD string`() {
        val merchantPhone = "0244123456"
        val amountInPesewas = 5000L // 50.00 GHS
        
        val paymentData = NfcPaymentData(
            merchantPhone = merchantPhone,
            amountInPesewas = amountInPesewas,
            provider = NfcPaymentData.Provider.MTN
        )
        
        // MTN USSD format: tel:*170*1*1*merchantPhone*amount#
        assertEquals("tel:*170*1*1*0244123456*50.00#", paymentData.toUssdString())
    }

    @Test
    fun `create Vodafone USSD string`() {
        val merchantPhone = "0201234567"
        val amountInPesewas = 10000L // 100.00 GHS
        
        val paymentData = NfcPaymentData(
            merchantPhone = merchantPhone,
            amountInPesewas = amountInPesewas,
            provider = NfcPaymentData.Provider.VODAFONE
        )
        
        // Vodafone USSD format: tel:*110*1*merchantPhone*amount#
        assertEquals("tel:*110*1*0201234567*100.00#", paymentData.toUssdString())
    }

    @Test
    fun `create AirtelTigo USSD string`() {
        val merchantPhone = "0271234567"
        val amountInPesewas = 7550L // 75.50 GHS
        
        val paymentData = NfcPaymentData(
            merchantPhone = merchantPhone,
            amountInPesewas = amountInPesewas,
            provider = NfcPaymentData.Provider.AIRTEL_TIGO
        )
        
        // AirtelTigo USSD format: tel:*500*1*merchantPhone*amount#
        assertEquals("tel:*500*1*0271234567*75.50#", paymentData.toUssdString())
    }

    @Test
    fun `payment data to USSD string contains tel prefix`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "1234567890",
            amountInPesewas = 5000L,
            provider = NfcPaymentData.Provider.MTN
        )
        
        val ussdString = paymentData.toUssdString()
        
        assertTrue(ussdString.startsWith("tel:"))
    }

    @Test
    fun `payment data default currency is GHS`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "123",
            amountInPesewas = 5000L
        )
        
        assertEquals("GHS", paymentData.currency)
    }

    @Test
    fun `payment data timestamp is set`() {
        val beforeTime = System.currentTimeMillis()
        
        val paymentData = NfcPaymentData(
            merchantPhone = "123",
            amountInPesewas = 5000L
        )
        
        val afterTime = System.currentTimeMillis()
        
        assertTrue(paymentData.timestamp >= beforeTime)
        assertTrue(paymentData.timestamp <= afterTime)
    }
    
    @Test
    fun `payment data isValid returns true for valid data`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInPesewas = 5000L,
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertTrue(paymentData.isValid())
    }
    
    @Test
    fun `payment data toPaymentUri generates correct format`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInPesewas = 5000L,
            provider = NfcPaymentData.Provider.MTN
        )
        
        val uri = paymentData.toPaymentUri()
        
        assertTrue(uri.startsWith("momo://pay?"))
        assertTrue(uri.contains("to=0244123456"))
        assertTrue(uri.contains("amount=50.00"))
        assertTrue(uri.contains("provider=MTN"))
    }
    
    @Test
    fun `getDisplayAmount converts pesewas to main currency unit`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "0244123456",
            amountInPesewas = 12345L
        )
        
        assertEquals(123.45, paymentData.getDisplayAmount(), 0.001)
    }
    
    @Test
    fun `toPesewas converts double to pesewas correctly`() {
        assertEquals(5000L, NfcPaymentData.toPesewas(50.00))
        assertEquals(7550L, NfcPaymentData.toPesewas(75.50))
        assertEquals(1L, NfcPaymentData.toPesewas(0.01))
    }
    
    @Test
    fun `fromAmount creates NfcPaymentData correctly`() {
        val paymentData = NfcPaymentData.fromAmount(
            merchantPhone = "0244123456",
            amount = 50.00,
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertEquals(5000L, paymentData.amountInPesewas)
        assertEquals(50.00, paymentData.getDisplayAmount(), 0.001)
    }
}
