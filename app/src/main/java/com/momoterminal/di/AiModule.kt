package com.momoterminal.di

import com.momoterminal.ai.AiSmsParserService
import com.momoterminal.sms.MomoSmsParser
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
    fun provideAiSmsParserService(
        smsParser: MomoSmsParser
    ): AiSmsParserService {
        return AiSmsParserService(smsParser)
    }
}
