package com.momoterminal.core.network.di

import com.momoterminal.core.network.supabase.SupabaseClientConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

/**
 * Hilt module providing Supabase-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return SupabaseClientConfig.client
    }
    
    @Provides
    @Singleton
    fun provideSupabaseAuth(): Auth {
        return SupabaseClientConfig.auth
    }

    @Provides
    @Singleton
    fun provideSupabasePostgrest(): Postgrest {
        return SupabaseClientConfig.postgrest
    }
}
