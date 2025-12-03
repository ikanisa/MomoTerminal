package com.momoterminal.core.network.supabase

import com.momoterminal.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Supabase client configuration singleton.
 * Provides access to Supabase services (Auth, Postgrest, Edge Functions).
 */
object SupabaseClientConfig {
    
    /**
     * Lazy-initialized Supabase client instance.
     * Configured with GoTrue (Auth) and Postgrest (Database) plugins.
     */
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                // Configure Auth plugin
                // Auto-refresh tokens when they expire
                autoLoadFromStorage = true
                autoSaveToStorage = true
            }
            
            install(Postgrest) {
                // Configure Postgrest plugin for database operations
            }
        }.also {
            Timber.d("Supabase client initialized: ${BuildConfig.SUPABASE_URL}")
        }
    }
    
    /**
     * Get Auth client for authentication operations.
     */
    val auth: Auth
        get() = client.auth
    
    /**
     * Get Postgrest client for database operations.
     */
    val postgrest: Postgrest
        get() = client.postgrest
    
    /**
     * HTTP client for Edge Functions
     */
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Retrofit instance for Edge Functions API
     */
    val edgeFunctionsApi: EdgeFunctionsApi by lazy {
        Retrofit.Builder()
            .baseUrl("${BuildConfig.SUPABASE_URL}/functions/v1/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EdgeFunctionsApi::class.java)
    }
}
