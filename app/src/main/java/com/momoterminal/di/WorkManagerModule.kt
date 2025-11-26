package com.momoterminal.di

import android.content.Context
import androidx.work.WorkManager
import com.momoterminal.sync.ConflictResolver
import com.momoterminal.sync.SyncManager
import com.momoterminal.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing WorkManager-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    /**
     * Provides WorkManager instance.
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }

    /**
     * Provides SyncManager instance.
     */
    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        networkMonitor: NetworkMonitor
    ): SyncManager {
        return SyncManager(context, networkMonitor)
    }

    /**
     * Provides ConflictResolver instance.
     */
    @Provides
    @Singleton
    fun provideConflictResolver(): ConflictResolver {
        return ConflictResolver()
    }
}
