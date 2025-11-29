package com.momoterminal.di

import com.momoterminal.api.AuthApiService
import com.momoterminal.api.AuthInterceptor
import com.momoterminal.auth.AuthRepository
import com.momoterminal.auth.SessionManager
import com.momoterminal.auth.TokenManager
import com.momoterminal.security.BiometricHelper
import com.momoterminal.security.SecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Hilt module providing authentication-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    /**
     * Provides TokenManager for secure token storage.
     */
    @Provides
    @Singleton
    fun provideTokenManager(
        secureStorage: SecureStorage
    ): TokenManager {
        return TokenManager(secureStorage)
    }

    /**
     * Provides SessionManager for session state management.
     */
    @Provides
    @Singleton
    fun provideSessionManager(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        tokenManager: TokenManager
    ): SessionManager {
        return SessionManager(context, tokenManager)
    }

    /**
     * Provides AuthInterceptor for HTTP authentication headers.
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenManager: TokenManager,
        authRepositoryProvider: Provider<AuthRepository>
    ): AuthInterceptor {
        return AuthInterceptor(tokenManager, authRepositoryProvider)
    }

    /**
     * Provides AuthApiService for authentication API calls.
     */
    @Provides
    @Singleton
    fun provideAuthApiService(
        okHttpClient: OkHttpClient
    ): AuthApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.momoterminal.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    /**
     * Provides AuthRepository for authentication operations.
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        authApiService: AuthApiService,
        tokenManager: TokenManager,
        sessionManager: SessionManager
    ): AuthRepository {
        return AuthRepository(authApiService, tokenManager, sessionManager)
    }
}
