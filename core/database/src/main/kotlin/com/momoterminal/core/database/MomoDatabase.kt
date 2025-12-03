package com.momoterminal.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.momoterminal.core.database.dao.*
import com.momoterminal.core.database.entity.*

@Database(
    entities = [
        TransactionEntity::class,
        WebhookConfigEntity::class,
        SmsDeliveryLogEntity::class,
        TokenWalletEntity::class,
        TokenTransactionEntity::class,
        SmsTransactionEntity::class,
        NfcTagEntity::class,
        TokenEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class MomoDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun webhookConfigDao(): WebhookConfigDao
    abstract fun smsDeliveryLogDao(): SmsDeliveryLogDao
    abstract fun walletDao(): WalletDao
    abstract fun smsTransactionDao(): SmsTransactionDao
    abstract fun nfcTagDao(): NfcTagDao
    abstract fun tokenDao(): TokenDao
}
