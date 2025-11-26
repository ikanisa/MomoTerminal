package com.momoterminal.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for encryption and decryption operations.
 * Uses AES-256-GCM for secure data encryption.
 */
@Singleton
class EncryptionManager @Inject constructor(
    private val keystoreHelper: KeystoreHelper
) {
    
    private val secureRandom = SecureRandom()
    
    /**
     * Encrypt data using AES-256-GCM.
     * Returns Base64 encoded string containing IV + encrypted data.
     */
    fun encrypt(plainText: String, keyAlias: String = DEFAULT_KEY_ALIAS): String {
        val secretKey = keystoreHelper.getOrCreateEncryptionKey(keyAlias)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Combine IV and encrypted data
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }
    
    /**
     * Decrypt data using AES-256-GCM.
     * Expects Base64 encoded string containing IV + encrypted data.
     */
    fun decrypt(encryptedData: String, keyAlias: String = DEFAULT_KEY_ALIAS): String {
        val secretKey = keystoreHelper.getOrCreateEncryptionKey(keyAlias)
        
        val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
        
        // Extract IV and encrypted data
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encryptedBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
    
    /**
     * Generate a secure random string of specified length.
     */
    fun generateSecureRandom(length: Int = 32): String {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    
    /**
     * Generate SHA-256 hash for data integrity verification.
     */
    fun generateHash(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Verify data integrity by comparing hashes.
     */
    fun verifyHash(data: String, expectedHash: String): Boolean {
        val actualHash = generateHash(data)
        return actualHash.equals(expectedHash, ignoreCase = true)
    }
    
    /**
     * Encrypt sensitive transaction data.
     * Returns encrypted data with integrity hash.
     */
    fun encryptTransactionData(
        sender: String,
        amount: String,
        transactionId: String
    ): EncryptedTransactionData {
        val plainData = "$sender|$amount|$transactionId"
        val encryptedData = encrypt(plainData)
        val hash = generateHash(plainData)
        
        return EncryptedTransactionData(
            encryptedPayload = encryptedData,
            integrityHash = hash
        )
    }
    
    /**
     * Decrypt and verify transaction data.
     * Returns null if integrity check fails.
     */
    fun decryptTransactionData(
        encryptedData: EncryptedTransactionData
    ): DecryptedTransactionData? {
        return try {
            val plainData = decrypt(encryptedData.encryptedPayload)
            
            // Verify integrity
            if (!verifyHash(plainData, encryptedData.integrityHash)) {
                return null
            }
            
            val parts = plainData.split("|")
            if (parts.size != 3) {
                return null
            }
            
            DecryptedTransactionData(
                sender = parts[0],
                amount = parts[1],
                transactionId = parts[2]
            )
        } catch (e: Exception) {
            null
        }
    }
    
    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        private const val DEFAULT_KEY_ALIAS = "momo_terminal_key"
    }
}

/**
 * Data class for encrypted transaction data.
 */
data class EncryptedTransactionData(
    val encryptedPayload: String,
    val integrityHash: String
)

/**
 * Data class for decrypted transaction data.
 */
data class DecryptedTransactionData(
    val sender: String,
    val amount: String,
    val transactionId: String
)
