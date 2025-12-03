package com.momoterminal.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.momoterminal.core.database.entity.WebhookConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for WebhookConfig operations.
 */
@Dao
interface WebhookConfigDao {
    
    /**
     * Insert a new webhook configuration.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(webhook: WebhookConfigEntity): Long
    
    /**
     * Update an existing webhook configuration.
     */
    @Update
    suspend fun update(webhook: WebhookConfigEntity)
    
    /**
     * Delete a webhook configuration.
     */
    @Delete
    suspend fun delete(webhook: WebhookConfigEntity)
    
    /**
     * Delete a webhook by ID.
     */
    @Query("DELETE FROM webhook_configs WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * Get all webhook configurations as a Flow.
     */
    @Query("SELECT * FROM webhook_configs ORDER BY createdAt DESC")
    fun getAllWebhooks(): Flow<List<WebhookConfigEntity>>
    
    /**
     * Get all webhook configurations (non-Flow version).
     */
    @Query("SELECT * FROM webhook_configs ORDER BY createdAt DESC")
    suspend fun getAllWebhooksList(): List<WebhookConfigEntity>
    
    /**
     * Get only active webhook configurations.
     */
    @Query("SELECT * FROM webhook_configs WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveWebhooks(): Flow<List<WebhookConfigEntity>>
    
    /**
     * Get only active webhook configurations (non-Flow version).
     */
    @Query("SELECT * FROM webhook_configs WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveWebhooksList(): List<WebhookConfigEntity>
    
    /**
     * Get a webhook by ID.
     */
    @Query("SELECT * FROM webhook_configs WHERE id = :id")
    suspend fun getById(id: Long): WebhookConfigEntity?
    
    /**
     * Get webhooks matching a phone number.
     * Used for routing SMS to the correct webhook(s).
     */
    @Query("""
        SELECT * FROM webhook_configs 
        WHERE isActive = 1 AND phoneNumber = :phoneNumber
    """)
    suspend fun getWebhooksByPhoneNumber(phoneNumber: String): List<WebhookConfigEntity>
    
    /**
     * Toggle the active state of a webhook.
     */
    @Query("UPDATE webhook_configs SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)
    
    /**
     * Get count of active webhooks.
     */
    @Query("SELECT COUNT(*) FROM webhook_configs WHERE isActive = 1")
    fun getActiveCount(): Flow<Int>
    
    /**
     * Check if a webhook with the given URL already exists.
     */
    @Query("SELECT COUNT(*) FROM webhook_configs WHERE url = :url AND id != :excludeId")
    suspend fun countByUrl(url: String, excludeId: Long = 0): Int
}
