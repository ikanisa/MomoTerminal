package com.momoterminal.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a webhook configuration.
 * Each webhook can be associated with a specific phone number for routing SMS.
 */
@Entity(tableName = "webhook_configs")
data class WebhookConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Display name for the webhook (e.g., "ibimina SACCO", "easymo Rides").
     */
    val name: String,
    
    /**
     * The webhook endpoint URL.
     */
    val url: String,
    
    /**
     * The associated MoMo phone number for routing.
     */
    val phoneNumber: String,
    
    /**
     * API key for authentication (sent as Bearer token).
     */
    val apiKey: String,
    
    /**
     * Secret for HMAC-SHA256 signature generation.
     */
    val hmacSecret: String,
    
    /**
     * Whether this webhook is currently active.
     */
    val isActive: Boolean = true,
    
    /**
     * Timestamp when the webhook was created.
     */
    val createdAt: Long = System.currentTimeMillis()
)
