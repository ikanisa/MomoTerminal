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
    
    @Provides
    @Singleton
    fun provideSupabaseAuth(): io.github.jan.supabase.gotrue.Auth {
        return com.momoterminal.supabase.SupabaseClientConfig.auth
    }

    @Provides
    @Singleton
    fun provideSupabasePostgrest(): io.github.jan.supabase.postgrest.Postgrest {
        return com.momoterminal.supabase.SupabaseClientConfig.postgrest
    }

    /**
     * Provides SupabaseAuthService for authentication operations.
     */
    @Provides
    @Singleton
    fun provideSupabaseAuthService(auth: io.github.jan.supabase.gotrue.Auth): SupabaseAuthService {
        return SupabaseAuthService(auth)
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
