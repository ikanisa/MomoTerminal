package com.momoterminal.di

import com.momoterminal.ai.AiSmsParserService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing AI-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    
    /**
     * Provides AiSmsParserService for AI-powered SMS parsing.
     */
    @Provides
    @Singleton
    fun provideAiSmsParserService(): AiSmsParserService {
        return AiSmsParserService()
    }
}
