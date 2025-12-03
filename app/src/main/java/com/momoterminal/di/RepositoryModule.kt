package com.momoterminal.di

import com.momoterminal.data.repository.CountryRepository
import com.momoterminal.data.repository.CountryRepositoryImpl
import com.momoterminal.data.repository.TransactionRepositoryImpl
import com.momoterminal.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

/**
 * Hilt module for repository dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository
    
    /**
     * Binds CountryRepositoryImpl to CountryRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindCountryRepository(
        impl: CountryRepositoryImpl
    ): CountryRepository

    companion object {
        @Provides
        @Singleton
        fun provideCountryRepository(
            supabaseClient: SupabaseClient
        ): CountryRepository = CountryRepository(supabaseClient)
    }
}
