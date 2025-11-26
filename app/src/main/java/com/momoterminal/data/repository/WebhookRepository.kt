package com.momoterminal.data.repository

import com.momoterminal.data.local.dao.SmsDeliveryLogDao
import com.momoterminal.data.local.dao.WebhookConfigDao
import com.momoterminal.data.local.entity.SmsDeliveryLogEntity
import com.momoterminal.data.local.entity.WebhookConfigEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for webhook configuration and delivery log operations.
 * Provides a clean API for the UI layer to interact with webhook data.
 */
@Singleton
class WebhookRepository @Inject constructor(
    private val webhookConfigDao: WebhookConfigDao,
    private val smsDeliveryLogDao: SmsDeliveryLogDao
) {
    
    // ============= Webhook Config Operations =============
    
    /**
     * Get all webhook configurations as a Flow.
     */
    fun getAllWebhooks(): Flow<List<WebhookConfigEntity>> {
        return webhookConfigDao.getAllWebhooks()
    }
    
    /**
     * Get all active webhooks as a Flow.
     */
    fun getActiveWebhooks(): Flow<List<WebhookConfigEntity>> {
        return webhookConfigDao.getActiveWebhooks()
    }
    
    /**
     * Get count of active webhooks.
     */
    fun getActiveWebhookCount(): Flow<Int> {
        return webhookConfigDao.getActiveCount()
    }
    
    /**
     * Get a webhook by ID.
     */
    suspend fun getWebhookById(id: Long): WebhookConfigEntity? {
        return webhookConfigDao.getById(id)
    }
    
    /**
     * Save a webhook configuration (insert or update).
     */
    suspend fun saveWebhook(webhook: WebhookConfigEntity): Long {
        return if (webhook.id == 0L) {
            webhookConfigDao.insert(webhook)
        } else {
            webhookConfigDao.update(webhook)
            webhook.id
        }
    }
    
    /**
     * Delete a webhook configuration.
     */
    suspend fun deleteWebhook(webhook: WebhookConfigEntity) {
        webhookConfigDao.delete(webhook)
    }
    
    /**
     * Delete a webhook by ID.
     */
    suspend fun deleteWebhookById(id: Long) {
        webhookConfigDao.deleteById(id)
    }
    
    /**
     * Toggle the active state of a webhook.
     */
    suspend fun setWebhookActive(id: Long, isActive: Boolean) {
        webhookConfigDao.setActive(id, isActive)
    }
    
    /**
     * Check if a webhook URL already exists.
     */
    suspend fun isUrlDuplicate(url: String, excludeId: Long = 0): Boolean {
        return webhookConfigDao.countByUrl(url, excludeId) > 0
    }
    
    // ============= Delivery Log Operations =============
    
    /**
     * Get all delivery logs as a Flow.
     */
    fun getAllDeliveryLogs(): Flow<List<SmsDeliveryLogEntity>> {
        return smsDeliveryLogDao.getAllLogs()
    }
    
    /**
     * Get recent delivery logs.
     */
    fun getRecentDeliveryLogs(limit: Int = 50): Flow<List<SmsDeliveryLogEntity>> {
        return smsDeliveryLogDao.getRecentLogs(limit)
    }
    
    /**
     * Get delivery logs for a specific webhook.
     */
    fun getLogsByWebhook(webhookId: Long): Flow<List<SmsDeliveryLogEntity>> {
        return smsDeliveryLogDao.getLogsByWebhook(webhookId)
    }
    
    /**
     * Get delivery logs by status.
     */
    fun getLogsByStatus(status: String): Flow<List<SmsDeliveryLogEntity>> {
        return smsDeliveryLogDao.getLogsByStatus(status)
    }
    
    /**
     * Get a delivery log by ID.
     */
    suspend fun getDeliveryLogById(id: Long): SmsDeliveryLogEntity? {
        return smsDeliveryLogDao.getById(id)
    }
    
    /**
     * Get count of pending deliveries.
     */
    fun getPendingDeliveryCount(): Flow<Int> {
        return smsDeliveryLogDao.getPendingCount()
    }
    
    /**
     * Get count of deliveries by status.
     */
    fun getDeliveryCountByStatus(status: String): Flow<Int> {
        return smsDeliveryLogDao.getCountByStatus(status)
    }
    
    /**
     * Delete old delivery logs (older than specified timestamp).
     */
    suspend fun deleteOldLogs(timestamp: Long): Int {
        return smsDeliveryLogDao.deleteOldLogs(timestamp)
    }
    
    /**
     * Get the PagingSource for paginated delivery logs.
     */
    fun getDeliveryLogsPagingSource() = smsDeliveryLogDao.getLogsPagingSource()
    
    /**
     * Get filtered delivery logs PagingSource.
     */
    fun getFilteredDeliveryLogsPagingSource(
        webhookId: Long?,
        status: String?,
        startTime: Long?,
        endTime: Long?
    ) = smsDeliveryLogDao.getFilteredLogsPagingSource(webhookId, status, startTime, endTime)
}
