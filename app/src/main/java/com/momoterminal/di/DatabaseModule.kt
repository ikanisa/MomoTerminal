package com.momoterminal.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.momoterminal.data.local.EncryptedDatabaseFactory
import com.momoterminal.data.local.MomoDatabase
import com.momoterminal.data.local.dao.SmsDeliveryLogDao
import com.momoterminal.data.local.dao.TransactionDao
import com.momoterminal.data.local.dao.WebhookConfigDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing database-related dependencies.
 * Configures Room database with SQLCipher encryption and proper migrations.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "momoterminal_database"

    /**
     * Migration from version 1 to 2.
     * Adds new columns to transactions table for enhanced tracking.
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add new columns to transactions table
            db.execSQL("ALTER TABLE transactions ADD COLUMN amount REAL DEFAULT NULL")
            db.execSQL("ALTER TABLE transactions ADD COLUMN currency TEXT DEFAULT 'GHS'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN transactionId TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE transactions ADD COLUMN merchantCode TEXT DEFAULT NULL")

            // Create webhook_configs table
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

            // Create sms_delivery_logs table
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

            // Create indexes for sms_delivery_logs
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sms_delivery_logs_webhookId ON sms_delivery_logs(webhookId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sms_delivery_logs_status ON sms_delivery_logs(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sms_delivery_logs_createdAt ON sms_delivery_logs(createdAt)")
        }
    }

    /**
     * Provides the Room database instance with SQLCipher encryption.
     *
     * Security features:
     * - Database encrypted using SQLCipher with AES-256
     * - Encryption key stored securely via EncryptedSharedPreferences
     * - Proper migrations to preserve user data during updates
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MomoDatabase {
        // Get SQLCipher support factory for encrypted database
        val supportFactory = EncryptedDatabaseFactory.getSupportFactory(context)

        return Room.databaseBuilder(
            context,
            MomoDatabase::class.java,
            DATABASE_NAME
        )
            .openHelperFactory(supportFactory)
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    /**
     * Provides TransactionDao for database operations.
     */
    @Provides
    @Singleton
    fun provideTransactionDao(
        database: MomoDatabase
    ): TransactionDao {
        return database.transactionDao()
    }

    /**
     * Provides WebhookConfigDao for webhook configuration operations.
     */
    @Provides
    @Singleton
    fun provideWebhookConfigDao(
        database: MomoDatabase
    ): WebhookConfigDao {
        return database.webhookConfigDao()
    }

    /**
     * Provides SmsDeliveryLogDao for SMS delivery log operations.
     */
    @Provides
    @Singleton
    fun provideSmsDeliveryLogDao(
        database: MomoDatabase
    ): SmsDeliveryLogDao {
        return database.smsDeliveryLogDao()
    }
}
