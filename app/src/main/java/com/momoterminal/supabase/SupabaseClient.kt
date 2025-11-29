package com.momoterminal.supabase

import com.momoterminal.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import timber.log.Timber

/**
 * Supabase client configuration singleton.
 * Provides access to Supabase services (Auth, Postgrest).
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
}
