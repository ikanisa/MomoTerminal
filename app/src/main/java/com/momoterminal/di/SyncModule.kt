package com.momoterminal.di

import com.momoterminal.data.remote.api.WalletSyncApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideWalletSyncApi(retrofit: Retrofit): WalletSyncApi {
        return retrofit.create(WalletSyncApi::class.java)
    }
}
