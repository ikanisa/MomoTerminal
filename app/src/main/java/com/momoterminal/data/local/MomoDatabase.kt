package com.momoterminal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.momoterminal.data.local.dao.*
import com.momoterminal.data.local.entity.*

@Database(
    entities = [
        TransactionEntity::class,
        WebhookConfigEntity::class,
        SmsDeliveryLogEntity::class,
        TokenWalletEntity::class,
        TokenTransactionEntity::class,
        SmsTransactionEntity::class,
        NfcTagEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class MomoDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun webhookConfigDao(): WebhookConfigDao
    abstract fun smsDeliveryLogDao(): SmsDeliveryLogDao
    abstract fun walletDao(): WalletDao
    abstract fun smsTransactionDao(): SmsTransactionDao
    abstract fun nfcTagDao(): NfcTagDao
}
