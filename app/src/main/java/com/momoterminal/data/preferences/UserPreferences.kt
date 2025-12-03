package com.momoterminal.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User preferences data class.
 */
data class UserPreferencesData(
    // Profile (from WhatsApp registration)
    val phoneNumber: String = "",
    val countryCode: String = "RW",
    val merchantName: String = "",
    
    // Mobile Money config (can differ from profile country)
    val momoCountryCode: String = "",
    val merchantPhone: String = "",
    
    // Settings
    val biometricEnabled: Boolean = false,
    val smsAutoSyncEnabled: Boolean = true,
    val keepScreenOn: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val autoLockTimeout: Int = 5
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = PREFERENCES_NAME
    )
    
    /**
     * Combined flow of all user preferences.
     */
    val userPreferencesFlow: Flow<UserPreferencesData> = context.dataStore.data.map { prefs ->
        UserPreferencesData(
            phoneNumber = prefs[KEY_PHONE_NUMBER] ?: "",
            countryCode = prefs[KEY_COUNTRY_CODE] ?: "RW",
            merchantName = prefs[KEY_MERCHANT_NAME] ?: "",
            momoCountryCode = prefs[KEY_MOMO_COUNTRY_CODE] ?: prefs[KEY_COUNTRY_CODE] ?: "RW",
            merchantPhone = prefs[KEY_MERCHANT_PHONE] ?: "",
            biometricEnabled = prefs[KEY_BIOMETRIC_ENABLED] ?: false,
            smsAutoSyncEnabled = prefs[KEY_SMS_AUTO_SYNC_ENABLED] ?: true,
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: false,
            vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: true,
            autoLockTimeout = prefs[KEY_AUTO_LOCK_TIMEOUT] ?: DEFAULT_AUTO_LOCK_TIMEOUT
        )
    }
    
    /**
     * Update user profile from registration.
     */
    suspend fun updateProfile(
        phoneNumber: String,
        countryCode: String,
        merchantName: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PHONE_NUMBER] = phoneNumber
            prefs[KEY_COUNTRY_CODE] = countryCode
            prefs[KEY_MERCHANT_NAME] = merchantName
            // Default MoMo country to profile country if not set
            if (prefs[KEY_MOMO_COUNTRY_CODE].isNullOrEmpty()) {
                prefs[KEY_MOMO_COUNTRY_CODE] = countryCode
            }
        }
    }
    
    /**
     * Update Mobile Money configuration.
     * MoMo country can differ from profile country.
     */
    suspend fun updateMomoConfig(
        momoCountryCode: String,
        momoPhoneNumber: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MOMO_COUNTRY_CODE] = momoCountryCode
            prefs[KEY_MERCHANT_PHONE] = momoPhoneNumber
        }
    }
    
    /**
     * Update biometric setting.
     */
    suspend fun updateBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }
    
    // Individual flows for backward compatibility
    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }
    val smsAutoSyncEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_SMS_AUTO_SYNC_ENABLED] ?: true }
    val keepScreenOnEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_KEEP_SCREEN_ON] ?: false }
    val vibrationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_VIBRATION_ENABLED] ?: true }
    val autoLockTimeoutFlow: Flow<Int> = context.dataStore.data.map { it[KEY_AUTO_LOCK_TIMEOUT] ?: DEFAULT_AUTO_LOCK_TIMEOUT }
    
    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }
    
    suspend fun setSmsAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SMS_AUTO_SYNC_ENABLED] = enabled }
    }
    
    suspend fun setKeepScreenOnEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_KEEP_SCREEN_ON] = enabled }
    }
    
    fun keepScreenOnEnabledBlocking(): Boolean = runBlocking { keepScreenOnEnabledFlow.first() }
    
    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATION_ENABLED] = enabled }
    }
    
    fun vibrationEnabledBlocking(): Boolean = runBlocking { vibrationEnabledFlow.first() }
    
    suspend fun setAutoLockTimeout(timeoutMinutes: Int) {
        context.dataStore.edit { it[KEY_AUTO_LOCK_TIMEOUT] = timeoutMinutes }
    }
    
    // Device UUID
    suspend fun saveDeviceUuid(uuid: String) {
        context.dataStore.edit { it[KEY_DEVICE_UUID] = uuid }
    }
    
    suspend fun getDeviceUuid(): String? = context.dataStore.data.first()[KEY_DEVICE_UUID]
    
    // Clear all
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
    
    companion object {
        private const val PREFERENCES_NAME = "user_preferences"
        
        // Profile keys
        private val KEY_PHONE_NUMBER = stringPreferencesKey("phone_number")
        private val KEY_COUNTRY_CODE = stringPreferencesKey("country_code")
        private val KEY_MERCHANT_NAME = stringPreferencesKey("merchant_name")
        
        // MoMo config keys
        private val KEY_MOMO_COUNTRY_CODE = stringPreferencesKey("momo_country_code")
        private val KEY_MERCHANT_PHONE = stringPreferencesKey("merchant_phone")
        
        // Settings keys
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_SMS_AUTO_SYNC_ENABLED = booleanPreferencesKey("sms_auto_sync_enabled")
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val KEY_AUTO_LOCK_TIMEOUT = intPreferencesKey("auto_lock_timeout_minutes")
        private val KEY_DEVICE_UUID = stringPreferencesKey("device_uuid")
        
        const val DEFAULT_AUTO_LOCK_TIMEOUT = 5
    }
}
