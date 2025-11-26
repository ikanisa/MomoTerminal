package com.momoterminal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing an SMS delivery log entry.
 * Tracks the delivery status of SMS messages sent to webhooks.
 */
@Entity(
    tableName = "sms_delivery_logs",
    foreignKeys = [
        ForeignKey(
            entity = WebhookConfigEntity::class,
            parentColumns = ["id"],
            childColumns = ["webhookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["webhookId"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class SmsDeliveryLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Reference to the webhook configuration.
     */
    val webhookId: Long,
    
    /**
     * The phone number the SMS was received on.
     */
    val phoneNumber: String,
    
    /**
     * The sender of the SMS (e.g., "MTN MoMo").
     */
    val sender: String,
    
    /**
     * The SMS message content.
     */
    val message: String,
    
    /**
     * Delivery status: "pending", "sent", "failed", "delivered".
     */
    val status: String,
    
    /**
     * HTTP response code from the webhook.
     */
    val responseCode: Int? = null,
    
    /**
     * Response body from the webhook.
     */
    val responseBody: String? = null,
    
    /**
     * Number of retry attempts made.
     */
    val retryCount: Int = 0,
    
    /**
     * Timestamp when the log entry was created.
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * Timestamp when the SMS was successfully sent.
     */
    val sentAt: Long? = null
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_SENT = "sent"
        const val STATUS_FAILED = "failed"
        const val STATUS_DELIVERED = "delivered"
    }
}
