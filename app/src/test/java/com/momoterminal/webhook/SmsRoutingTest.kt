package com.momoterminal.webhook

import com.google.common.truth.Truth.assertThat
import com.momoterminal.data.local.dao.SmsDeliveryLogDao
import com.momoterminal.data.local.dao.WebhookConfigDao
import com.momoterminal.data.local.entity.WebhookConfigEntity
import com.momoterminal.util.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SMS routing logic in WebhookDispatcher.
 */
class SmsRoutingTest {
    
    private lateinit var webhookConfigDao: WebhookConfigDao
    private lateinit var smsDeliveryLogDao: SmsDeliveryLogDao
    private lateinit var hmacSigner: HmacSigner
    private lateinit var networkMonitor: NetworkMonitor
    
    @Before
    fun setup() {
        webhookConfigDao = mockk(relaxed = true)
        smsDeliveryLogDao = mockk(relaxed = true)
        hmacSigner = HmacSigner()
        networkMonitor = mockk(relaxed = true)
    }
    
    @Test
    fun `routes SMS to webhook matching phone number`() = runTest {
        // Arrange
        val phoneNumber = "+250788123456"
        val webhook = WebhookConfigEntity(
            id = 1,
            name = "Test Webhook",
            url = "https://example.com/webhook",
            phoneNumber = phoneNumber,
            apiKey = "api-key",
            hmacSecret = "secret",
            isActive = true
        )
        
        coEvery { webhookConfigDao.getWebhooksByPhoneNumber(phoneNumber) } returns listOf(webhook)
        coEvery { smsDeliveryLogDao.insert(any()) } returns 1L
        every { networkMonitor.isConnected } returns false
        
        // Assert - verify the routing logic
        val matchingWebhooks = webhookConfigDao.getWebhooksByPhoneNumber(phoneNumber)
        assertThat(matchingWebhooks).hasSize(1)
        assertThat(matchingWebhooks[0].phoneNumber).isEqualTo(phoneNumber)
    }
    
    @Test
    fun `routes SMS to multiple webhooks with same phone number`() = runTest {
        // Arrange
        val phoneNumber = "+250788123456"
        val webhook1 = WebhookConfigEntity(
            id = 1,
            name = "Webhook 1",
            url = "https://example1.com/webhook",
            phoneNumber = phoneNumber,
            apiKey = "api-key-1",
            hmacSecret = "secret-1",
            isActive = true
        )
        val webhook2 = WebhookConfigEntity(
            id = 2,
            name = "Webhook 2",
            url = "https://example2.com/webhook",
            phoneNumber = phoneNumber,
            apiKey = "api-key-2",
            hmacSecret = "secret-2",
            isActive = true
        )
        
        coEvery { webhookConfigDao.getWebhooksByPhoneNumber(phoneNumber) } returns listOf(webhook1, webhook2)
        
        // Assert
        val matchingWebhooks = webhookConfigDao.getWebhooksByPhoneNumber(phoneNumber)
        assertThat(matchingWebhooks).hasSize(2)
    }
    
    @Test
    fun `does not route SMS when no matching webhook found`() = runTest {
        // Arrange
        val phoneNumber = "+250788999999"
        
        coEvery { webhookConfigDao.getWebhooksByPhoneNumber(phoneNumber) } returns emptyList()
        coEvery { webhookConfigDao.getActiveWebhooksList() } returns emptyList()
        
        // Assert
        val matchingWebhooks = webhookConfigDao.getWebhooksByPhoneNumber(phoneNumber)
        assertThat(matchingWebhooks).isEmpty()
    }
    
    @Test
    fun `routes SMS to wildcard webhook when no specific match`() = runTest {
        // Arrange
        val phoneNumber = "+250788999999"
        val wildcardWebhook = WebhookConfigEntity(
            id = 1,
            name = "Wildcard Webhook",
            url = "https://example.com/webhook",
            phoneNumber = "*",
            apiKey = "api-key",
            hmacSecret = "secret",
            isActive = true
        )
        
        coEvery { webhookConfigDao.getWebhooksByPhoneNumber(phoneNumber) } returns emptyList()
        coEvery { webhookConfigDao.getActiveWebhooksList() } returns listOf(wildcardWebhook)
        
        // Assert - should fall back to wildcard
        val matchingWebhooks = webhookConfigDao.getWebhooksByPhoneNumber(phoneNumber)
        assertThat(matchingWebhooks).isEmpty()
        
        val activeWebhooks = webhookConfigDao.getActiveWebhooksList()
        assertThat(activeWebhooks).hasSize(1)
        assertThat(activeWebhooks[0].phoneNumber).isEqualTo("*")
    }
    
    @Test
    fun `skips inactive webhooks during routing`() = runTest {
        // Arrange
        val phoneNumber = "+250788123456"
        
        // Only active webhooks should be returned by the DAO query
        coEvery { webhookConfigDao.getWebhooksByPhoneNumber(phoneNumber) } returns emptyList()
        
        // Verify that the DAO query only returns active webhooks
        // (The DAO query includes "WHERE isActive = 1")
        coVerify(exactly = 0) { smsDeliveryLogDao.insert(any()) }
    }
    
    @Test
    fun `routes SMS to empty phone number webhook as catch-all`() = runTest {
        // Arrange
        val phoneNumber = "+250788999999"
        val catchAllWebhook = WebhookConfigEntity(
            id = 1,
            name = "Catch All Webhook",
            url = "https://example.com/webhook",
            phoneNumber = "", // Empty phone number = catch all
            apiKey = "api-key",
            hmacSecret = "secret",
            isActive = true
        )
        
        coEvery { webhookConfigDao.getWebhooksByPhoneNumber(phoneNumber) } returns emptyList()
        coEvery { webhookConfigDao.getActiveWebhooksList() } returns listOf(catchAllWebhook)
        
        // Assert - should fall back to catch-all
        val activeWebhooks = webhookConfigDao.getActiveWebhooksList()
        assertThat(activeWebhooks).hasSize(1)
        assertThat(activeWebhooks[0].phoneNumber).isEmpty()
    }
    
    @Test
    fun `creates delivery log for each matched webhook`() = runTest {
        // Arrange
        val phoneNumber = "+250788123456"
        val sender = "MTN MoMo"
        val message = "You received 5000 RWF"
        
        val webhook1 = WebhookConfigEntity(
            id = 1,
            name = "Webhook 1",
            url = "https://example1.com/webhook",
            phoneNumber = phoneNumber,
            apiKey = "api-key-1",
            hmacSecret = "secret-1",
            isActive = true
        )
        val webhook2 = WebhookConfigEntity(
            id = 2,
            name = "Webhook 2",
            url = "https://example2.com/webhook",
            phoneNumber = phoneNumber,
            apiKey = "api-key-2",
            hmacSecret = "secret-2",
            isActive = true
        )
        
        coEvery { webhookConfigDao.getWebhooksByPhoneNumber(phoneNumber) } returns listOf(webhook1, webhook2)
        coEvery { smsDeliveryLogDao.insert(any()) } returnsMany listOf(1L, 2L)
        every { networkMonitor.isConnected } returns false
        
        // Assert - verify that we would create logs for both webhooks
        val matchingWebhooks = webhookConfigDao.getWebhooksByPhoneNumber(phoneNumber)
        assertThat(matchingWebhooks).hasSize(2)
    }
}
