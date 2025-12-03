package com.momoterminal.core.network.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit client singleton for API communication.
 */
object ApiClient {

    private const val DEFAULT_TIMEOUT = 30L

    @Volatile
    private var retrofit: Retrofit? = null

    @Volatile
    private var apiService: MomoApiService? = null

    /**
     * Creates and returns the API service instance.
     * Uses a configurable base URL for flexibility.
     */
    fun getApiService(baseUrl: String): MomoApiService {
        if (apiService == null || retrofit?.baseUrl()?.toString() != baseUrl) {
            synchronized(this) {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .addInterceptor(loggingInterceptor)
                    .build()

                retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                apiService = retrofit?.create(MomoApiService::class.java)
            }
        }
        return apiService!!
    }

    /**
     * Reset the API client (useful for changing endpoints).
     */
    fun reset() {
        synchronized(this) {
            retrofit = null
            apiService = null
        }
    }
}
