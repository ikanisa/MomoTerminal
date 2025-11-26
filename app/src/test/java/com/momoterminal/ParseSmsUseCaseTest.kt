package com.momoterminal

import com.momoterminal.domain.model.Provider
import com.momoterminal.domain.usecase.ParseSmsUseCase
import com.momoterminal.util.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ParseSmsUseCase.
 */
class ParseSmsUseCaseTest {
    
    private lateinit var useCase: ParseSmsUseCase
    
    @Before
    fun setup() {
        useCase = ParseSmsUseCase()
    }
    
    @Test
    fun `parse valid MTN payment SMS extracts amount`() {
        val sender = "MTN MoMo"
        val body = "You have received GHS 50.00 from 0244123456. Transaction ID: MP123456789"
        
        val result = useCase(sender, body)
        
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(50.00, data.amount, 0.01)
        assertEquals(Provider.MTN, data.provider)
    }
    
    @Test
    fun `parse valid Vodafone payment SMS extracts amount`() {
        val sender = "VodaCash"
        val body = "You have received GHS 100.00. Payment credited to your account"
        
        val result = useCase(sender, body)
        
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(100.00, data.amount, 0.01)
        assertEquals(Provider.VODAFONE, data.provider)
    }
    
    @Test
    fun `parse SMS from unknown sender returns error`() {
        val sender = "UnknownSender"
        val body = "You have received GHS 50.00"
        
        val result = useCase(sender, body)
        
        assertTrue(result is Result.Error)
    }
    
    @Test
    fun `parse SMS without amount indicator returns error`() {
        val sender = "MTN"
        val body = "Hello, this is a test message"
        
        val result = useCase(sender, body)
        
        assertTrue(result is Result.Error)
    }
    
    @Test
    fun `parse SMS extracts transaction ID with standard format`() {
        val sender = "MTN MoMo"
        val body = "You have received GHS 50.00. Transaction ID: MP123456789"
        
        val result = useCase(sender, body)
        
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("MP123456789", data.transactionId)
    }
    
    @Test
    fun `parse SMS extracts phone number`() {
        val sender = "MTN MoMo"
        val body = "You have received GHS 50.00 from 0244123456"
        
        val result = useCase(sender, body)
        
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("0244123456", data.senderPhone)
    }
    
    @Test
    fun `parse SMS without transaction ID returns null for transactionId`() {
        val sender = "MTN MoMo"
        val body = "You have received GHS 50.00"
        
        val result = useCase(sender, body)
        
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertNull(data.transactionId)
    }
}
