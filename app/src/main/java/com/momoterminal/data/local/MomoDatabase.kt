package com.momoterminal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.momoterminal.data.local.dao.SmsDeliveryLogDao
import com.momoterminal.data.local.dao.TransactionDao
import com.momoterminal.data.local.dao.WalletDao
import com.momoterminal.data.local.dao.WebhookConfigDao
import com.momoterminal.data.local.entity.SmsDeliveryLogEntity
import com.momoterminal.data.local.entity.TokenTransactionEntity
import com.momoterminal.data.local.entity.TokenWalletEntity
import com.momoterminal.data.local.entity.TransactionEntity
import com.momoterminal.data.local.entity.WebhookConfigEntity

@Database(
    entities = [
        TransactionEntity::class,
        WebhookConfigEntity::class,
        SmsDeliveryLogEntity::class,
        TokenWalletEntity::class,
        TokenTransactionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MomoDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun webhookConfigDao(): WebhookConfigDao
    abstract fun smsDeliveryLogDao(): SmsDeliveryLogDao
    abstract fun walletDao(): WalletDao
}
