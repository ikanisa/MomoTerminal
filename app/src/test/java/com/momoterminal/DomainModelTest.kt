package com.momoterminal

import com.momoterminal.domain.model.Provider
import com.momoterminal.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for domain models.
 */
class DomainModelTest {
    
    @Test
    fun `Provider MTN generates correct USSD code`() {
        val ussd = Provider.MTN.generateUssdCode("0244123456", 50.00)
        assertEquals("*170*1*1*0244123456*50.00#", ussd)
    }
    
    @Test
    fun `Provider VODAFONE generates correct USSD code`() {
        val ussd = Provider.VODAFONE.generateUssdCode("0201234567", 100.00)
        assertEquals("*110*1*0201234567*100.00#", ussd)
    }
    
    @Test
    fun `Provider AIRTELTIGO generates correct USSD code`() {
        val ussd = Provider.AIRTELTIGO.generateUssdCode("0271234567", 75.50)
        assertEquals("*500*1*0271234567*75.50#", ussd)
    }
    
    @Test
    fun `Provider fromSender detects MTN correctly`() {
        assertEquals(Provider.MTN, Provider.fromSender("MTN MoMo"))
        assertEquals(Provider.MTN, Provider.fromSender("MTN"))
        assertEquals(Provider.MTN, Provider.fromSender("MoMo"))
    }
    
    @Test
    fun `Provider fromSender detects Vodafone correctly`() {
        assertEquals(Provider.VODAFONE, Provider.fromSender("Vodafone"))
        assertEquals(Provider.VODAFONE, Provider.fromSender("VodaCash"))
    }
    
    @Test
    fun `Provider fromSender detects AirtelTigo correctly`() {
        assertEquals(Provider.AIRTELTIGO, Provider.fromSender("AirtelTigo"))
        assertEquals(Provider.AIRTELTIGO, Provider.fromSender("ATMoney"))
    }
    
    @Test
    fun `Provider fromSender returns null for unknown sender`() {
        assertEquals(null, Provider.fromSender("Unknown"))
        assertEquals(null, Provider.fromSender("RandomSender"))
    }
    
    @Test
    fun `SyncStatus fromValue returns correct status`() {
        assertEquals(SyncStatus.PENDING, SyncStatus.fromValue("PENDING"))
        assertEquals(SyncStatus.SENT, SyncStatus.fromValue("SENT"))
        assertEquals(SyncStatus.FAILED, SyncStatus.fromValue("FAILED"))
    }
    
    @Test
    fun `SyncStatus fromValue returns PENDING for unknown value`() {
        assertEquals(SyncStatus.PENDING, SyncStatus.fromValue("UNKNOWN"))
        assertEquals(SyncStatus.PENDING, SyncStatus.fromValue(""))
    }
    
    @Test
    fun `SyncStatus value returns correct string`() {
        assertEquals("PENDING", SyncStatus.PENDING.value)
        assertEquals("SENT", SyncStatus.SENT.value)
        assertEquals("FAILED", SyncStatus.FAILED.value)
    }
}
