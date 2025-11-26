package com.momoterminal.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for Android Keystore operations.
 * Manages encryption keys stored in the Android Keystore.
 */
@Singleton
class KeystoreHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }
    
    /**
     * Get or create the encryption key for data encryption.
     */
    fun getOrCreateEncryptionKey(keyAlias: String = DEFAULT_KEY_ALIAS): SecretKey {
        // Check if key already exists
        if (keyStore.containsAlias(keyAlias)) {
            val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey?.let { return it }
        }
        
        // Generate new key
        return generateEncryptionKey(keyAlias)
    }
    
    /**
     * Generate a new AES-256 encryption key.
     */
    private fun generateEncryptionKey(keyAlias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Allow background operations
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setInvalidatedByBiometricEnrollment(false)
                }
            }
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    /**
     * Check if a key exists in the Keystore.
     */
    fun hasKey(keyAlias: String = DEFAULT_KEY_ALIAS): Boolean {
        return try {
            keyStore.containsAlias(keyAlias)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Delete a key from the Keystore.
     */
    fun deleteKey(keyAlias: String = DEFAULT_KEY_ALIAS) {
        try {
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
            }
        } catch (e: Exception) {
            // Log error but don't throw - key may not exist
        }
    }
    
    /**
     * Rotate the encryption key by deleting old key and creating new one.
     * Note: Data encrypted with old key will no longer be decryptable.
     */
    fun rotateKey(keyAlias: String = DEFAULT_KEY_ALIAS): SecretKey {
        deleteKey(keyAlias)
        return generateEncryptionKey(keyAlias)
    }
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DEFAULT_KEY_ALIAS = "momo_terminal_key"
    }
}
