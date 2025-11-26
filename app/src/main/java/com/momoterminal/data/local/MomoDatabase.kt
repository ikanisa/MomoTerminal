package com.momoterminal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.momoterminal.data.local.dao.SmsDeliveryLogDao
import com.momoterminal.data.local.dao.TransactionDao
import com.momoterminal.data.local.dao.WebhookConfigDao
import com.momoterminal.data.local.entity.SmsDeliveryLogEntity
import com.momoterminal.data.local.entity.TransactionEntity
import com.momoterminal.data.local.entity.WebhookConfigEntity

/**
 * Room Database for MomoTerminal application.
 * Provides the database instance for local transaction storage and webhook management.
 */
@Database(
    entities = [
        TransactionEntity::class,
        WebhookConfigEntity::class,
        SmsDeliveryLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MomoDatabase : RoomDatabase() {
    
    /**
     * Get the TransactionDao for database operations.
     */
    abstract fun transactionDao(): TransactionDao
    
    /**
     * Get the WebhookConfigDao for webhook configuration operations.
     */
    abstract fun webhookConfigDao(): WebhookConfigDao
    
    /**
     * Get the SmsDeliveryLogDao for delivery log operations.
     */
    abstract fun smsDeliveryLogDao(): SmsDeliveryLogDao
}
