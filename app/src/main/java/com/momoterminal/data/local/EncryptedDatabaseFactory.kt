package com.momoterminal.data.local

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.sqlcipher.database.SupportFactory
import timber.log.Timber
import java.security.SecureRandom

/**
 * Factory for creating encrypted Room database instances using SQLCipher.
 *
 * This class manages the database encryption passphrase lifecycle:
 * 1. Generates a strong random passphrase on first run
 * 2. Stores the passphrase securely using EncryptedSharedPreferences
 * 3. Retrieves the passphrase for subsequent database access
 *
 * Security considerations:
 * - Passphrase is 32 bytes (256 bits) generated using SecureRandom
 * - Passphrase is stored encrypted via Android Keystore
 * - SQLCipher provides AES-256 database encryption
 *
 * Usage:
 * ```kotlin
 * val factory = EncryptedDatabaseFactory.getSupportFactory(context)
 * Room.databaseBuilder(context, MomoDatabase::class.java, DATABASE_NAME)
 *     .openHelperFactory(factory)
 *     .build()
 * ```
 */
object EncryptedDatabaseFactory {

    private const val ENCRYPTED_PREFS_NAME = "momo_db_encryption"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    private const val PASSPHRASE_LENGTH_BYTES = 32 // 256 bits

    /**
     * Creates a SupportFactory for encrypted Room database using SQLCipher.
     *
     * @param context Application context
     * @return SupportFactory that can be used with Room's openHelperFactory()
     */
    fun getSupportFactory(context: Context): SupportSQLiteOpenHelper.Factory {
        val passphrase = getOrCreatePassphrase(context)
        return SupportFactory(passphrase)
    }

    /**
     * Gets the existing passphrase or creates a new one if not present.
     * The passphrase is stored securely using EncryptedSharedPreferences.
     *
     * @param context Application context
     * @return Passphrase as ByteArray for SQLCipher
     */
    private fun getOrCreatePassphrase(context: Context): ByteArray {
        val encryptedPrefs = getEncryptedPrefs(context)

        val storedPassphrase = encryptedPrefs.getString(KEY_DB_PASSPHRASE, null)

        return if (storedPassphrase != null) {
            Timber.d("Using existing database encryption passphrase")
            Base64.decode(storedPassphrase, Base64.NO_WRAP)
        } else {
            Timber.d("Generating new database encryption passphrase")
            val newPassphrase = generateSecurePassphrase()
            val encodedPassphrase = Base64.encodeToString(newPassphrase, Base64.NO_WRAP)
            
            encryptedPrefs.edit()
                .putString(KEY_DB_PASSPHRASE, encodedPassphrase)
                .apply()
            
            newPassphrase
        }
    }

    /**
     * Generates a cryptographically secure random passphrase.
     *
     * @return 32-byte (256-bit) random passphrase
     */
    private fun generateSecurePassphrase(): ByteArray {
        val secureRandom = SecureRandom()
        val passphrase = ByteArray(PASSPHRASE_LENGTH_BYTES)
        secureRandom.nextBytes(passphrase)
        return passphrase
    }

    /**
     * Creates EncryptedSharedPreferences for storing the database passphrase.
     *
     * @param context Application context
     * @return EncryptedSharedPreferences instance
     */
    private fun getEncryptedPrefs(context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Checks if the database passphrase exists.
     * Useful for migration scenarios.
     *
     * @param context Application context
     * @return true if passphrase exists, false otherwise
     */
    fun hasExistingPassphrase(context: Context): Boolean {
        val encryptedPrefs = getEncryptedPrefs(context)
        return encryptedPrefs.getString(KEY_DB_PASSPHRASE, null) != null
    }

    /**
     * Clears the database passphrase.
     * WARNING: This will make the encrypted database inaccessible.
     * Only use during complete app data reset.
     *
     * @param context Application context
     */
    fun clearPassphrase(context: Context) {
        Timber.w("Clearing database encryption passphrase - database will become inaccessible")
        val encryptedPrefs = getEncryptedPrefs(context)
        encryptedPrefs.edit()
            .remove(KEY_DB_PASSPHRASE)
            .apply()
    }
}
