package com.momoterminal.di

import android.content.Context
import com.momoterminal.data.local.SecureDataStore
import com.momoterminal.security.AppSecurityInitializer
import com.momoterminal.security.CertificatePinnerConfig
import com.momoterminal.security.DeviceSecurityManager
import com.momoterminal.security.EncryptionManager
import com.momoterminal.security.KeystoreHelper
import com.momoterminal.security.PlayIntegrityManager
import com.momoterminal.security.ScreenSecurityManager
import com.momoterminal.security.SecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing security-related dependencies.
 * Configures EncryptedSharedPreferences, Keystore, encryption utilities,
 * and security hardening components.
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    /**
     * Provides SecureStorage for encrypted preferences.
     */
    @Provides
    @Singleton
    fun provideSecureStorage(
        @ApplicationContext context: Context
    ): SecureStorage {
        return SecureStorage(context)
    }

    /**
     * Provides KeystoreHelper for Android Keystore operations.
     */
    @Provides
    @Singleton
    fun provideKeystoreHelper(
        @ApplicationContext context: Context
    ): KeystoreHelper {
        return KeystoreHelper(context)
    }

    /**
     * Provides EncryptionManager for data encryption/decryption.
     */
    @Provides
    @Singleton
    fun provideEncryptionManager(
        keystoreHelper: KeystoreHelper
    ): EncryptionManager {
        return EncryptionManager(keystoreHelper)
    }

    /**
     * Provides CertificatePinnerConfig for SSL certificate pinning.
     */
    @Provides
    @Singleton
    fun provideCertificatePinnerConfig(): CertificatePinnerConfig {
        return CertificatePinnerConfig()
    }

    /**
     * Provides DeviceSecurityManager for root/emulator detection.
     */
    @Provides
    @Singleton
    fun provideDeviceSecurityManager(
        @ApplicationContext context: Context
    ): DeviceSecurityManager {
        return DeviceSecurityManager(context)
    }

    /**
     * Provides PlayIntegrityManager for Play Integrity API integration.
     */
    @Provides
    @Singleton
    fun providePlayIntegrityManager(
        @ApplicationContext context: Context
    ): PlayIntegrityManager {
        return PlayIntegrityManager(context)
    }

    /**
     * Provides ScreenSecurityManager for screen capture prevention.
     */
    @Provides
    @Singleton
    fun provideScreenSecurityManager(): ScreenSecurityManager {
        return ScreenSecurityManager()
    }

    /**
     * Provides AppSecurityInitializer for startup security checks.
     */
    @Provides
    @Singleton
    fun provideAppSecurityInitializer(
        @ApplicationContext context: Context,
        deviceSecurityManager: DeviceSecurityManager
    ): AppSecurityInitializer {
        return AppSecurityInitializer(context, deviceSecurityManager)
    }

    /**
     * Provides SecureDataStore for encrypted data storage.
     */
    @Provides
    @Singleton
    fun provideSecureDataStore(
        @ApplicationContext context: Context
    ): SecureDataStore {
        return SecureDataStore(context)
    }
}
