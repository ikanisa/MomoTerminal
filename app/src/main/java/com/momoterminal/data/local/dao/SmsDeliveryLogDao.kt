package com.momoterminal.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.momoterminal.data.local.entity.SmsDeliveryLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SmsDeliveryLog operations.
 */
@Dao
interface SmsDeliveryLogDao {
    
    /**
     * Insert a new delivery log entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SmsDeliveryLogEntity): Long
    
    /**
     * Update an existing delivery log entry.
     */
    @Update
    suspend fun update(log: SmsDeliveryLogEntity)
    
    /**
     * Get a delivery log by ID.
     */
    @Query("SELECT * FROM sms_delivery_logs WHERE id = :id")
    suspend fun getById(id: Long): SmsDeliveryLogEntity?
    
    /**
     * Get all delivery logs as a Flow.
     */
    @Query("SELECT * FROM sms_delivery_logs ORDER BY createdAt DESC")
    fun getAllLogs(): Flow<List<SmsDeliveryLogEntity>>
    
    /**
     * Get delivery logs with pagination.
     */
    @Query("SELECT * FROM sms_delivery_logs ORDER BY createdAt DESC")
    fun getLogsPagingSource(): PagingSource<Int, SmsDeliveryLogEntity>
    
    /**
     * Get delivery logs for a specific webhook.
     */
    @Query("SELECT * FROM sms_delivery_logs WHERE webhookId = :webhookId ORDER BY createdAt DESC")
    fun getLogsByWebhook(webhookId: Long): Flow<List<SmsDeliveryLogEntity>>
    
    /**
     * Get delivery logs by status.
     */
    @Query("SELECT * FROM sms_delivery_logs WHERE status = :status ORDER BY createdAt DESC")
    fun getLogsByStatus(status: String): Flow<List<SmsDeliveryLogEntity>>
    
    /**
     * Get pending delivery logs that need to be retried.
     */
    @Query("""
        SELECT * FROM sms_delivery_logs 
        WHERE (status = 'pending' OR status = 'failed') 
        AND retryCount < :maxRetries
        ORDER BY createdAt ASC
    """)
    suspend fun getPendingLogs(maxRetries: Int = 5): List<SmsDeliveryLogEntity>
    
    /**
     * Get pending logs for a specific webhook.
     */
    @Query("""
        SELECT * FROM sms_delivery_logs 
        WHERE webhookId = :webhookId 
        AND (status = 'pending' OR status = 'failed') 
        AND retryCount < :maxRetries
        ORDER BY createdAt ASC
    """)
    suspend fun getPendingLogsByWebhook(webhookId: Long, maxRetries: Int = 5): List<SmsDeliveryLogEntity>
    
    /**
     * Update the status of a delivery log.
     */
    @Query("""
        UPDATE sms_delivery_logs 
        SET status = :status, 
            responseCode = :responseCode, 
            responseBody = :responseBody,
            retryCount = :retryCount,
            sentAt = :sentAt
        WHERE id = :id
    """)
    suspend fun updateDeliveryStatus(
        id: Long,
        status: String,
        responseCode: Int?,
        responseBody: String?,
        retryCount: Int,
        sentAt: Long?
    )
    
    /**
     * Increment retry count for a log.
     */
    @Query("UPDATE sms_delivery_logs SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Long)
    
    /**
     * Get count of pending deliveries.
     */
    @Query("SELECT COUNT(*) FROM sms_delivery_logs WHERE status = 'pending' OR status = 'failed'")
    fun getPendingCount(): Flow<Int>
    
    /**
     * Get count of deliveries by status.
     */
    @Query("SELECT COUNT(*) FROM sms_delivery_logs WHERE status = :status")
    fun getCountByStatus(status: String): Flow<Int>
    
    /**
     * Delete old delivery logs.
     */
    @Query("DELETE FROM sms_delivery_logs WHERE createdAt < :timestamp AND status = 'sent'")
    suspend fun deleteOldLogs(timestamp: Long): Int
    
    /**
     * Delete all logs for a webhook.
     */
    @Query("DELETE FROM sms_delivery_logs WHERE webhookId = :webhookId")
    suspend fun deleteByWebhook(webhookId: Long)
    
    /**
     * Get filtered logs with pagination.
     */
    @Query("""
        SELECT * FROM sms_delivery_logs 
        WHERE (:webhookId IS NULL OR webhookId = :webhookId)
          AND (:status IS NULL OR status = :status)
          AND (:startTime IS NULL OR createdAt >= :startTime)
          AND (:endTime IS NULL OR createdAt <= :endTime)
        ORDER BY createdAt DESC
    """)
    fun getFilteredLogsPagingSource(
        webhookId: Long?,
        status: String?,
        startTime: Long?,
        endTime: Long?
    ): PagingSource<Int, SmsDeliveryLogEntity>
    
    /**
     * Get filtered logs as a Flow.
     */
    @Query("""
        SELECT * FROM sms_delivery_logs 
        WHERE (:webhookId IS NULL OR webhookId = :webhookId)
          AND (:status IS NULL OR status = :status)
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    fun getFilteredLogs(
        webhookId: Long?,
        status: String?,
        limit: Int = 100
    ): Flow<List<SmsDeliveryLogEntity>>
    
    /**
     * Get recent logs limited to a certain count.
     */
    @Query("SELECT * FROM sms_delivery_logs ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 50): Flow<List<SmsDeliveryLogEntity>>
}
