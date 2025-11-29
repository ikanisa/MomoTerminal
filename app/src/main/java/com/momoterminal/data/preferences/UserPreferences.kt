package com.momoterminal.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User preferences stored using DataStore.
 * Handles biometric settings and other user preferences.
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = PREFERENCES_NAME
    )
    
    /**
     * Data class representing user preference values.
     */
    data class UserPreferencesData(
        val biometricEnabled: Boolean = false,
        val biometricRequiredForTransactions: Boolean = false,
        val autoLockTimeoutMinutes: Int = DEFAULT_AUTO_LOCK_TIMEOUT
    )
    
    /**
     * Flow of user preferences.
     */
    val userPreferencesFlow: Flow<UserPreferencesData> = context.dataStore.data.map { preferences ->
        UserPreferencesData(
            biometricEnabled = preferences[KEY_BIOMETRIC_ENABLED] ?: false,
            biometricRequiredForTransactions = preferences[KEY_BIOMETRIC_FOR_TRANSACTIONS] ?: false,
            autoLockTimeoutMinutes = preferences[KEY_AUTO_LOCK_TIMEOUT] ?: DEFAULT_AUTO_LOCK_TIMEOUT
        )
    }
    
    /**
     * Flow for biometric enabled setting.
     */
    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_ENABLED] ?: false
    }
    
    /**
     * Flow for biometric required for transactions setting.
     */
    val biometricRequiredForTransactionsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_FOR_TRANSACTIONS] ?: false
    }
    
    /**
     * Flow for auto-lock timeout setting.
     */
    val autoLockTimeoutFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_LOCK_TIMEOUT] ?: DEFAULT_AUTO_LOCK_TIMEOUT
    }
    
    /**
     * Set biometric enabled state.
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }
    
    /**
     * Set biometric required for transactions.
     */
    suspend fun setBiometricRequiredForTransactions(required: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_FOR_TRANSACTIONS] = required
        }
    }
    
    /**
     * Set auto-lock timeout in minutes.
     */
    suspend fun setAutoLockTimeout(timeoutMinutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_LOCK_TIMEOUT] = timeoutMinutes
        }
    }
    
    /**
     * Clear all user preferences.
     */
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    companion object {
        private const val PREFERENCES_NAME = "user_preferences"
        
        // Preference keys
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_BIOMETRIC_FOR_TRANSACTIONS = booleanPreferencesKey("biometric_required_for_transactions")
        private val KEY_AUTO_LOCK_TIMEOUT = intPreferencesKey("auto_lock_timeout_minutes")
        
        // Default values
        const val DEFAULT_AUTO_LOCK_TIMEOUT = 5 // 5 minutes
    }
}
