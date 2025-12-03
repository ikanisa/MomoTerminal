package com.momoterminal.core.database

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// DataStore extension for user preferences
private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

/**
 * Secure data storage implementation combining EncryptedSharedPreferences
 * for sensitive data (tokens, secrets) and DataStore for user preferences.
 * 
 * Uses AES-256-GCM encryption via Android Keystore for maximum security.
 */
@Singleton
class SecureDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ==================== Encrypted SharedPreferences ====================

    /**
     * MasterKey for encryption using AES256_GCM scheme.
     */
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    /**
     * Encrypted SharedPreferences for storing sensitive data like auth tokens.
     */
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ==================== Auth Token Operations ====================

    /**
     * Saves the authentication token securely.
     */
    fun saveAuthToken(token: String) {
        try {
            encryptedPrefs.edit()
                .putString(KEY_AUTH_TOKEN, token)
                .apply()
            Timber.d("Auth token saved securely")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save auth token")
        }
    }

    /**
     * Retrieves the authentication token.
     */
    fun getAuthToken(): String? {
        return try {
            encryptedPrefs.getString(KEY_AUTH_TOKEN, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve auth token")
            null
        }
    }

    /**
     * Checks if an auth token exists.
     */
    fun hasAuthToken(): Boolean {
        return !getAuthToken().isNullOrBlank()
    }

    /**
     * Clears the authentication token.
     */
    fun clearAuthToken() {
        try {
            encryptedPrefs.edit()
                .remove(KEY_AUTH_TOKEN)
                .apply()
            Timber.d("Auth token cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear auth token")
        }
    }

    // ==================== Refresh Token Operations ====================

    /**
     * Saves the refresh token securely.
     */
    fun saveRefreshToken(token: String) {
        try {
            encryptedPrefs.edit()
                .putString(KEY_REFRESH_TOKEN, token)
                .apply()
            Timber.d("Refresh token saved securely")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save refresh token")
        }
    }

    /**
     * Retrieves the refresh token.
     */
    fun getRefreshToken(): String? {
        return try {
            encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve refresh token")
            null
        }
    }

    /**
     * Clears the refresh token.
     */
    fun clearRefreshToken() {
        try {
            encryptedPrefs.edit()
                .remove(KEY_REFRESH_TOKEN)
                .apply()
            Timber.d("Refresh token cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear refresh token")
        }
    }

    // ==================== Session Operations ====================

    /**
     * Saves the session ID securely.
     */
    fun saveSessionId(sessionId: String) {
        try {
            encryptedPrefs.edit()
                .putString(KEY_SESSION_ID, sessionId)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save session ID")
        }
    }

    /**
     * Retrieves the session ID.
     */
    fun getSessionId(): String? {
        return try {
            encryptedPrefs.getString(KEY_SESSION_ID, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve session ID")
            null
        }
    }

    /**
     * Clears the session ID.
     */
    fun clearSessionId() {
        try {
            encryptedPrefs.edit()
                .remove(KEY_SESSION_ID)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear session ID")
        }
    }

    // ==================== PIN/Passcode Operations ====================

    /**
     * Saves the user's PIN hash securely.
     * Note: Always hash the PIN before storing.
     */
    fun savePinHash(pinHash: String) {
        try {
            encryptedPrefs.edit()
                .putString(KEY_PIN_HASH, pinHash)
                .apply()
            Timber.d("PIN hash saved securely")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save PIN hash")
        }
    }

    /**
     * Retrieves the PIN hash.
     */
    fun getPinHash(): String? {
        return try {
            encryptedPrefs.getString(KEY_PIN_HASH, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve PIN hash")
            null
        }
    }

    /**
     * Checks if a PIN has been set.
     */
    fun hasPinSet(): Boolean {
        return !getPinHash().isNullOrBlank()
    }

    /**
     * Clears the PIN hash.
     */
    fun clearPinHash() {
        try {
            encryptedPrefs.edit()
                .remove(KEY_PIN_HASH)
                .apply()
            Timber.d("PIN hash cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear PIN hash")
        }
    }

    // ==================== Clear All Sensitive Data ====================

    /**
     * Clears all encrypted data. Used during logout.
     */
    fun clearAllEncryptedData() {
        try {
            encryptedPrefs.edit().clear().apply()
            Timber.d("All encrypted data cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear encrypted data")
        }
    }

    // ==================== DataStore for User Preferences ====================

    private val dataStore: DataStore<Preferences> = context.userPreferencesDataStore

    /**
     * Flow of user preference indicating if biometric login is enabled.
     */
    val biometricEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PREF_BIOMETRIC_ENABLED] ?: false
    }

    /**
     * Flow of user preference for notification settings.
     */
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PREF_NOTIFICATIONS_ENABLED] ?: true
    }

    /**
     * Flow of user's preferred language.
     */
    val preferredLanguage: Flow<String> = dataStore.data.map { preferences ->
        preferences[PREF_LANGUAGE] ?: "en"
    }

    /**
     * Flow of dark mode preference.
     */
    val darkModeEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PREF_DARK_MODE] ?: false
    }

    /**
     * Flow indicating if onboarding has been completed.
     */
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PREF_ONBOARDING_COMPLETED] ?: false
    }

    /**
     * Sets biometric login preference.
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PREF_BIOMETRIC_ENABLED] = enabled
        }
    }

    /**
     * Sets notification preference.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PREF_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * Sets preferred language.
     */
    suspend fun setPreferredLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PREF_LANGUAGE] = language
        }
    }

    /**
     * Sets dark mode preference.
     */
    suspend fun setDarkModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PREF_DARK_MODE] = enabled
        }
    }

    /**
     * Sets onboarding completed flag.
     */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PREF_ONBOARDING_COMPLETED] = completed
        }
    }

    /**
     * Clears all user preferences.
     */
    suspend fun clearAllPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Clears all data including encrypted data and preferences.
     */
    suspend fun clearAllData() {
        clearAllEncryptedData()
        clearAllPreferences()
        Timber.d("All secure data cleared")
    }

    companion object {
        // Encrypted SharedPreferences file name
        private const val ENCRYPTED_PREFS_NAME = "momo_secure_data"

        // Encrypted preference keys
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_PIN_HASH = "pin_hash"

        // DataStore preference keys
        private val PREF_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val PREF_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val PREF_LANGUAGE = stringPreferencesKey("preferred_language")
        private val PREF_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val PREF_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
