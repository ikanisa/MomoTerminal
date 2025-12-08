package com.momoterminal.core.data.di

import com.momoterminal.core.data.repository.*
import com.momoterminal.core.domain.repository.SettingsRepository
import com.momoterminal.core.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCountryRepository(impl: CountryRepositoryImpl): CountryRepository

    @Binds
    @Singleton
    abstract fun bindWalletRepository(impl: WalletRepositoryImpl): WalletRepository

    @Binds
    @Singleton
    abstract fun bindSmsRepository(impl: SmsRepositoryImpl): SmsRepository

    @Binds
    @Singleton
    abstract fun bindNfcRepository(impl: NfcRepositoryImpl): NfcRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryStub): SettingsRepository
}
