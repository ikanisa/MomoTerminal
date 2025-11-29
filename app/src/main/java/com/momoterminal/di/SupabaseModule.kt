package com.momoterminal.di

import com.momoterminal.supabase.SupabaseAuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Supabase-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    
    /**
     * Provides SupabaseAuthService for authentication operations.
     */
    @Provides
    @Singleton
    fun provideSupabaseAuthService(): SupabaseAuthService {
        return SupabaseAuthService()
    }
}
