package com.momoterminal.feature.vending.di

import com.momoterminal.feature.vending.data.*
import com.momoterminal.feature.vending.domain.repository.VendingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VendingModule {
    @Provides @Singleton
    fun provideVendingApiService(retrofit: Retrofit): VendingApiService =
        retrofit.create(VendingApiService::class.java)
    
    @Provides @Singleton
    fun provideVendingRepository(apiService: VendingApiService): VendingRepository =
        VendingRepositoryImpl(apiService)
}
