package com.momoterminal.di

import android.content.Context
import androidx.room.Room
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
 * Configures Room database and DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "momoterminal_database"

    /**
     * Provides the Room database instance.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MomoDatabase {
        return Room.databaseBuilder(
            context,
            MomoDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
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
