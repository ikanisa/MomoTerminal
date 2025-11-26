package com.momoterminal.di

import android.content.Context
import com.momoterminal.security.EncryptionManager
import com.momoterminal.security.KeystoreHelper
import com.momoterminal.security.SecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing security-related dependencies.
 * Configures EncryptedSharedPreferences, Keystore, and encryption utilities.
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
}
