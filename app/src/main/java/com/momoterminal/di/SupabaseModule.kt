package com.momoterminal.di

import com.momoterminal.auth.WhatsAppOtpService
import com.momoterminal.auth.WhatsAppOtpServiceImpl
import com.momoterminal.supabase.EdgeFunctionsApi
import com.momoterminal.supabase.SupabaseAuthService
import com.momoterminal.supabase.SupabaseClientConfig
import com.momoterminal.supabase.SupabasePaymentRepository
import com.momoterminal.util.PhoneNumberValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
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
    fun provideSupabaseAuth(): io.github.jan.supabase.gotrue.Auth {
        return SupabaseClientConfig.auth
    }

    @Provides
    @Singleton
    fun provideSupabasePostgrest(): io.github.jan.supabase.postgrest.Postgrest {
        return SupabaseClientConfig.postgrest
    }
    
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
