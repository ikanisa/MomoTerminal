package com.momoterminal.di

import com.momoterminal.sms.patterns.LocalizedSmsParser
import com.momoterminal.sms.patterns.SmsPatternRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object I18nModule {
    
    @Provides
    @Singleton
    fun provideSmsPatternRegistry(): SmsPatternRegistry {
        return SmsPatternRegistry()
    }
    
    @Provides
    @Singleton
    fun provideLocalizedSmsParser(
        patternRegistry: SmsPatternRegistry
    ): LocalizedSmsParser {
        return LocalizedSmsParser(patternRegistry)
    }
}
