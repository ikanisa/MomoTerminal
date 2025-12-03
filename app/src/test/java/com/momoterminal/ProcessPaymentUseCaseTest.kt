package com.momoterminal

import com.momoterminal.domain.model.Provider
import com.momoterminal.domain.usecase.ProcessPaymentUseCase
import com.momoterminal.util.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ProcessPaymentUseCase.
 * 
 * Note: USSD prefixes are defined in Provider enum:
 * - MTN: *182*8*1*
 * - VODAFONE: *110*1*
 * - AIRTELTIGO: *500*1*
 */
class ProcessPaymentUseCaseTest {
    
    private lateinit var useCase: ProcessPaymentUseCase
    
    @Before
    fun setup() {
        useCase = ProcessPaymentUseCase()
    }
    
    @Test
    fun `process payment with MTN provider returns correct USSD code`() {
        val result = useCase(
            provider = Provider.MTN,
            merchantCode = "0244123456",
            amount = 50.00
        )
        
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("*182*8*1*0244123456*50.00#", data.ussdCode)
        assertEquals("tel:*182*8*1*0244123456*50.00#", data.dialString)
        assertEquals(Provider.MTN, data.provider)
    }
    
    @Test
    fun `process payment with Vodafone provider returns correct USSD code`() {
        val result = useCase(
            provider = Provider.VODAFONE,
            merchantCode = "0201234567",
            amount = 100.00
        )
        
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("*110*1*0201234567*100.00#", data.ussdCode)
    }
    
    @Test
    fun `process payment with AirtelTigo provider returns correct USSD code`() {
        val result = useCase(
            provider = Provider.AIRTELTIGO,
            merchantCode = "0271234567",
            amount = 75.50
        )
        
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("*500*1*0271234567*75.50#", data.ussdCode)
    }
    
    @Test
    fun `process payment with empty merchant code returns error`() {
        val result = useCase(
            provider = Provider.MTN,
            merchantCode = "",
            amount = 50.00
        )
        
        assertTrue(result is Result.Error)
    }
    
    @Test
    fun `process payment with zero amount returns error`() {
        val result = useCase(
            provider = Provider.MTN,
            merchantCode = "0244123456",
            amount = 0.0
        )
        
        assertTrue(result is Result.Error)
    }
    
    @Test
    fun `process payment with negative amount returns error`() {
        val result = useCase(
            provider = Provider.MTN,
            merchantCode = "0244123456",
            amount = -10.0
        )
        
        assertTrue(result is Result.Error)
    }
}
