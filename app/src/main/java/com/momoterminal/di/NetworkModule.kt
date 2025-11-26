package com.momoterminal.di

import com.momoterminal.BuildConfig
import com.momoterminal.data.remote.api.MomoApiService
import com.momoterminal.data.remote.interceptor.AuthInterceptor
import com.momoterminal.data.remote.interceptor.NetworkInterceptor
import com.momoterminal.security.SecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing network-related dependencies.
 * Configures Retrofit, OkHttpClient, and API services.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val DEFAULT_TIMEOUT = 30L
    private const val DEFAULT_BASE_URL = "https://api.momoterminal.com/"

    /**
     * Provides HttpLoggingInterceptor for debugging network calls.
     * Only logs body in debug builds to prevent sensitive data exposure.
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            // Only enable detailed logging in debug builds to prevent sensitive data exposure
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * Provides AuthInterceptor for adding authentication headers.
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(
        secureStorage: SecureStorage
    ): AuthInterceptor {
        return AuthInterceptor(secureStorage)
    }

    /**
     * Provides NetworkInterceptor for network status checking.
     */
    @Provides
    @Singleton
    fun provideNetworkInterceptor(): NetworkInterceptor {
        return NetworkInterceptor()
    }

    /**
     * Provides OkHttpClient with interceptors and timeouts.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        networkInterceptor: NetworkInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(networkInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Provides Retrofit instance configured with OkHttpClient and Gson converter.
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides MomoApiService for API calls.
     */
    @Provides
    @Singleton
    fun provideMomoApiService(
        retrofit: Retrofit
    ): MomoApiService {
        return retrofit.create(MomoApiService::class.java)
    }
}
