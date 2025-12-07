package com.momoterminal.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.momoterminal.core.database.EncryptedDatabaseFactory
import com.momoterminal.core.database.MomoDatabase
import com.momoterminal.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.momoterminal.core.database.migration.MIGRATION_4_5
import com.momoterminal.core.database.migration.MIGRATION_5_6

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "momoterminal_database"

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN amount REAL DEFAULT NULL")
            db.execSQL("ALTER TABLE transactions ADD COLUMN currency TEXT DEFAULT 'GHS'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN transactionId TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE transactions ADD COLUMN merchantCode TEXT DEFAULT NULL")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS webhook_configs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    url TEXT NOT NULL,
                    phoneNumber TEXT NOT NULL,
                    apiKey TEXT NOT NULL,
                    hmacSecret TEXT NOT NULL,
                    isActive INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sms_delivery_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    webhookId INTEGER NOT NULL,
                    phoneNumber TEXT NOT NULL,
                    sender TEXT NOT NULL,
                    message TEXT NOT NULL,
                    status TEXT NOT NULL,
                    responseCode INTEGER DEFAULT NULL,
                    responseBody TEXT DEFAULT NULL,
                    retryCount INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    sentAt INTEGER DEFAULT NULL,
                    FOREIGN KEY (webhookId) REFERENCES webhook_configs(id) ON DELETE CASCADE
                )
            """.trimIndent())

            db.execSQL("CREATE INDEX IF NOT EXISTS index_sms_delivery_logs_webhookId ON sms_delivery_logs(webhookId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sms_delivery_logs_status ON sms_delivery_logs(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sms_delivery_logs_createdAt ON sms_delivery_logs(createdAt)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS token_wallets (
                    id TEXT PRIMARY KEY NOT NULL,
                    userId TEXT NOT NULL,
                    balance INTEGER NOT NULL DEFAULT 0,
                    currency TEXT NOT NULL DEFAULT 'CREDITS',
                    walletType TEXT NOT NULL DEFAULT 'CREDITS',
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING'
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS token_transactions (
                    id TEXT PRIMARY KEY NOT NULL,
                    walletId TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    balanceBefore INTEGER NOT NULL,
                    balanceAfter INTEGER NOT NULL,
                    reference TEXT,
                    referenceType TEXT,
                    description TEXT,
                    metadata TEXT,
                    createdAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    FOREIGN KEY (walletId) REFERENCES token_wallets(id) ON DELETE CASCADE
                )
            """.trimIndent())

            db.execSQL("CREATE INDEX IF NOT EXISTS index_token_transactions_walletId ON token_transactions(walletId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_token_transactions_createdAt ON token_transactions(createdAt)")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create sms_transactions table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sms_transactions (
                    id TEXT PRIMARY KEY NOT NULL,
                    raw_message TEXT NOT NULL,
                    sender TEXT NOT NULL,
                    amount REAL NOT NULL,
                    currency TEXT NOT NULL DEFAULT 'GHS',
                    type TEXT NOT NULL DEFAULT 'UNKNOWN',
                    balance REAL,
                    reference TEXT,
                    timestamp INTEGER NOT NULL,
                    synced INTEGER NOT NULL DEFAULT 0,
                    wallet_credited INTEGER NOT NULL DEFAULT 0,
                    retry_count INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sms_transactions_reference ON sms_transactions(reference)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sms_transactions_timestamp ON sms_transactions(timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sms_transactions_synced ON sms_transactions(synced)")

            // Create nfc_tags table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS nfc_tags (
                    tagId TEXT PRIMARY KEY NOT NULL,
                    entityType TEXT NOT NULL,
                    entityId TEXT NOT NULL,
                    metadata TEXT,
                    lastScanned INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MomoDatabase {
        val supportFactory = EncryptedDatabaseFactory.getSupportFactory(context)
        return Room.databaseBuilder(context, MomoDatabase::class.java, DATABASE_NAME)
            .openHelperFactory(supportFactory)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: MomoDatabase): TransactionDao = database.transactionDao()

    @Provides
    @Singleton
    fun provideWebhookConfigDao(database: MomoDatabase): WebhookConfigDao = database.webhookConfigDao()

    @Provides
    @Singleton
    fun provideSmsDeliveryLogDao(database: MomoDatabase): SmsDeliveryLogDao = database.smsDeliveryLogDao()

    @Provides
    @Singleton
    fun provideWalletDao(database: MomoDatabase): WalletDao = database.walletDao()

    @Provides
    @Singleton
    fun provideSmsTransactionDao(database: MomoDatabase): SmsTransactionDao = database.smsTransactionDao()

    @Provides
    @Singleton
    fun provideNfcTagDao(database: MomoDatabase): NfcTagDao = database.nfcTagDao()

    @Provides
    @Singleton
    fun provideTokenDao(database: MomoDatabase): TokenDao = database.tokenDao()
}
