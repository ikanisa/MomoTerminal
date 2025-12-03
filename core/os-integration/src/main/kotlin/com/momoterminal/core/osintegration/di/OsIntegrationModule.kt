package com.momoterminal.core.osintegration.di

import android.content.Context
import com.momoterminal.core.osintegration.capabilities.BiometricProvider
import com.momoterminal.core.osintegration.capabilities.CameraProvider
import com.momoterminal.core.osintegration.capabilities.LocationProvider
import com.momoterminal.core.osintegration.deeplinks.DeepLinkHandler
import com.momoterminal.core.osintegration.notifications.AppNotificationManager
import com.momoterminal.core.osintegration.shortcuts.AppShortcutManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OsIntegrationModule {
    
    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): AppNotificationManager = AppNotificationManager(context)
    
    @Provides
    @Singleton
    fun provideDeepLinkHandler(): DeepLinkHandler = DeepLinkHandler()
    
    @Provides
    @Singleton
    fun provideShortcutManager(
        @ApplicationContext context: Context
    ): AppShortcutManager = AppShortcutManager(context)
    
    @Provides
    @Singleton
    fun provideLocationProvider(
        @ApplicationContext context: Context
    ): LocationProvider = LocationProvider(context)
    
    @Provides
    @Singleton
    fun provideCameraProvider(
        @ApplicationContext context: Context
    ): CameraProvider = CameraProvider(context)
    
    @Provides
    @Singleton
    fun provideBiometricProvider(
        @ApplicationContext context: Context
    ): BiometricProvider = BiometricProvider(context)
}
