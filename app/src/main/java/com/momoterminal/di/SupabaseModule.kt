package com.momoterminal.di

import com.momoterminal.auth.WhatsAppOtpService
import com.momoterminal.auth.WhatsAppOtpServiceImpl
import com.momoterminal.supabase.SupabaseAuthService
import com.momoterminal.util.PhoneNumberValidator
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
    
    /**
     * Provides PhoneNumberValidator for phone number formatting and validation.
     */
    @Provides
    @Singleton
    fun providePhoneNumberValidator(): PhoneNumberValidator {
        return PhoneNumberValidator()
    }
    
    /**
     * Provides WhatsAppOtpService for WhatsApp OTP operations.
     */
    @Provides
    @Singleton
    fun provideWhatsAppOtpService(
        supabaseAuthService: SupabaseAuthService,
        phoneNumberValidator: PhoneNumberValidator
    ): WhatsAppOtpService {
        return WhatsAppOtpServiceImpl(supabaseAuthService, phoneNumberValidator)
    }
}
