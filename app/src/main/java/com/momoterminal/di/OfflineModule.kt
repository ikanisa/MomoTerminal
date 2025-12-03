package com.momoterminal.di

import android.content.Context
import com.momoterminal.data.local.dao.TransactionDao
import com.momoterminal.data.local.dao.WalletDao
import com.momoterminal.offline.OfflineFirstManager
import com.momoterminal.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OfflineModule {
    
    @Provides
    @Singleton
    fun provideOfflineFirstManager(
        @ApplicationContext context: Context,
        networkMonitor: NetworkMonitor,
        transactionDao: TransactionDao,
        walletDao: WalletDao
    ): OfflineFirstManager {
        return OfflineFirstManager(context, networkMonitor, transactionDao, walletDao)
    }
}
