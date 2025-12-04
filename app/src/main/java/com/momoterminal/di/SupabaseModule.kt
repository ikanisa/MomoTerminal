package com.momoterminal.di

import com.momoterminal.auth.WhatsAppOtpService
import com.momoterminal.auth.WhatsAppOtpServiceImpl
import com.momoterminal.supabase.EdgeFunctionsApi
import com.momoterminal.supabase.SupabaseAuthService
import com.momoterminal.supabase.SupabaseClientConfig
import com.momoterminal.supabase.SupabasePaymentRepository
import com.momoterminal.core.common.PhoneNumberValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

/**
 * Hilt module providing app-specific Supabase services.
 * Note: SupabaseClient, Auth, and Postgrest are provided by core:network module
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    
    @Provides
    @Singleton
    fun provideEdgeFunctionsApi(): EdgeFunctionsApi {
        return SupabaseClientConfig.edgeFunctionsApi
    }

    @Provides
    @Singleton
    fun provideSupabaseAuthService(
        auth: io.github.jan.supabase.gotrue.Auth,
        edgeFunctionsApi: EdgeFunctionsApi
    ): SupabaseAuthService {
        return SupabaseAuthService(auth, edgeFunctionsApi)
    }
    
    @Provides
    @Singleton
    fun provideSupabasePaymentRepository(postgrest: Postgrest): SupabasePaymentRepository {
        return SupabasePaymentRepository(postgrest)
    }
    
    @Provides
    @Singleton
    fun providePhoneNumberValidator(): PhoneNumberValidator {
        return PhoneNumberValidator
    }
    
    @Provides
    @Singleton
    fun provideWhatsAppOtpService(
        supabaseAuthService: SupabaseAuthService,
        phoneNumberValidator: PhoneNumberValidator
    ): WhatsAppOtpService {
        return WhatsAppOtpServiceImpl(supabaseAuthService, phoneNumberValidator)
    }
}
