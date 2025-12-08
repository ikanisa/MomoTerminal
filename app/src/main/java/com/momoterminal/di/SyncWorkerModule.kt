package com.momoterminal.di

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.momoterminal.core.common.worker.SmsTransactionSyncScheduler
import com.momoterminal.worker.SmsTransactionSyncWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

/**
 * Hilt module for providing SMS transaction sync scheduler.
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncWorkerModule {
    
    @Provides
    @Singleton
    fun provideSmsTransactionSyncScheduler(): SmsTransactionSyncScheduler {
        return object : SmsTransactionSyncScheduler {
            override fun scheduleSync(context: Context) {
                try {
                    val syncWorkRequest = OneTimeWorkRequestBuilder<SmsTransactionSyncWorker>()
                        .build()
                    
                    WorkManager.getInstance(context).enqueue(syncWorkRequest)
                    Timber.d("SmsTransactionSyncWorker scheduled")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to schedule sync worker - will retry on next app launch")
                }
            }
        }
    }
}
