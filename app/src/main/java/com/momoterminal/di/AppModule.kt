package com.momoterminal.di

import android.content.Context
import android.content.SharedPreferences
import com.momoterminal.core.common.config.AppConfig
import com.momoterminal.sync.SyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for application-scoped CoroutineScope.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Hilt module providing application-level dependencies.
 * These dependencies are scoped to the application lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val PREFS_NAME = "momo_terminal_prefs"

    /**
     * Provides SharedPreferences for non-sensitive data.
     * For sensitive data, use SecureStorage instead.
     */
    @Provides
    @Singleton
    @Named("regularPrefs")
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Provides the application context.
     */
    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context {
        return context
    }

    /**
     * Provides AppConfig for gateway configuration.
     */
    @Provides
    @Singleton
    fun provideAppConfig(
        @ApplicationContext context: Context
    ): AppConfig {
        return AppConfig(context)
    }

    /**
     * Provides an application-scoped CoroutineScope.
     * Uses SupervisorJob to prevent child failures from canceling the scope.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}
