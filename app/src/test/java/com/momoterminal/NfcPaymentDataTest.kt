package com.momoterminal

import com.momoterminal.feature.nfc.NfcPaymentData
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
            merchantPhone = "12345",
            amountInMinorUnits = 500000L, // 5000 RWF
            currency = "RWF",
            countryCode = "RW",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertEquals(5000.0, paymentData.getDisplayAmount(), 0.001)
        assertEquals("5000", paymentData.getWholeAmount())
    }

    @Test
    fun `payment data to USSD string contains tel prefix`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "12345",
            amountInMinorUnits = 500000L,
            currency = "RWF",
            countryCode = "RW",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertTrue(paymentData.toUssdString().startsWith("tel:"))
    }

    @Test
    fun `payment data generates correct raw USSD for Rwanda`() {
        val paymentData = NfcPaymentData(
            merchantPhone = "12345",
            amountInMinorUnits = 1000000L, // 10000 RWF
            currency = "RWF",
            countryCode = "RW",
            provider = NfcPaymentData.Provider.MTN
        )
        
        val rawUssd = paymentData.getRawUssdCode()
        assertEquals("*182*8*1*12345*10000#", rawUssd)
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
            merchantPhone = "12345",
            amountInMinorUnits = 5000L,
            currency = "RWF"
        )
        assertTrue(paymentData.isValid())
    }
    
    @Test
    fun `toMinorUnits converts correctly`() {
        assertEquals(500000L, NfcPaymentData.toMinorUnits(5000.0))
        assertEquals(755000L, NfcPaymentData.toMinorUnits(7550.0))
    }
    
    @Test
    fun `fromAmount creates NfcPaymentData correctly`() {
        val paymentData = NfcPaymentData.fromAmount(
            merchantPhone = "12345",
            amount = 5000.0,
            currency = "RWF",
            countryCode = "RW",
            provider = NfcPaymentData.Provider.MTN
        )
        
        assertEquals(500000L, paymentData.amountInMinorUnits)
        assertEquals(5000.0, paymentData.getDisplayAmount(), 0.001)
    }
}
