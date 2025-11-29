package com.momoterminal.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage implementation using EncryptedSharedPreferences.
 * Provides secure storage for sensitive data like API tokens and merchant codes.
 */
@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Save the merchant code securely.
     */
    fun saveMerchantCode(code: String) {
        encryptedPrefs.edit()
            .putString(KEY_MERCHANT_CODE, code)
            .apply()
    }
    
    /**
     * Get the stored merchant code.
     */
    fun getMerchantCode(): String? {
        return encryptedPrefs.getString(KEY_MERCHANT_CODE, null)
    }
    
    /**
     * Save the API token securely.
     */
    fun saveApiToken(token: String) {
        encryptedPrefs.edit()
            .putString(KEY_API_TOKEN, token)
            .apply()
    }
    
    /**
     * Get the stored API token.
     */
    fun getApiToken(): String? {
        return encryptedPrefs.getString(KEY_API_TOKEN, null)
    }
    
    /**
     * Save the API endpoint securely.
     */
    fun saveApiEndpoint(endpoint: String) {
        encryptedPrefs.edit()
            .putString(KEY_API_ENDPOINT, endpoint)
            .apply()
    }
    
    /**
     * Get the stored API endpoint.
     */
    fun getApiEndpoint(): String? {
        return encryptedPrefs.getString(KEY_API_ENDPOINT, null)
    }
    
    /**
     * Save the device ID securely.
     */
    fun saveDeviceId(deviceId: String) {
        encryptedPrefs.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .apply()
    }
    
    /**
     * Get the stored device ID.
     */
    fun getDeviceId(): String? {
        return encryptedPrefs.getString(KEY_DEVICE_ID, null)
    }
    
    /**
     * Save the API secret securely.
     */
    fun saveApiSecret(secret: String) {
        encryptedPrefs.edit()
            .putString(KEY_API_SECRET, secret)
            .apply()
    }
    
    /**
     * Get the stored API secret.
     */
    fun getApiSecret(): String? {
        return encryptedPrefs.getString(KEY_API_SECRET, null)
    }
    
    // ==================== Authentication Token Storage ====================
    
    /**
     * Save the refresh token for authentication.
     */
    fun saveRefreshToken(token: String) {
        encryptedPrefs.edit()
            .putString(KEY_REFRESH_TOKEN, token)
            .apply()
    }
    
    /**
     * Get the stored refresh token.
     */
    fun getRefreshToken(): String? {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    }
    
    /**
     * Save the token expiry timestamp.
     */
    fun saveTokenExpiry(expiryTimeMs: Long) {
        encryptedPrefs.edit()
            .putLong(KEY_TOKEN_EXPIRY, expiryTimeMs)
            .apply()
    }
    
    /**
     * Get the stored token expiry timestamp.
     */
    fun getTokenExpiry(): Long {
        return encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0L)
    }
    
    /**
     * Save the authenticated user ID.
     */
    fun saveUserId(userId: String) {
        encryptedPrefs.edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }
    
    /**
     * Get the stored user ID.
     */
    fun getUserId(): String? {
        return encryptedPrefs.getString(KEY_USER_ID, null)
    }
    
    /**
     * Save the authenticated user's phone number.
     */
    fun saveUserPhoneNumber(phoneNumber: String) {
        encryptedPrefs.edit()
            .putString(KEY_USER_PHONE, phoneNumber)
            .apply()
    }
    
    /**
     * Get the stored user's phone number.
     */
    fun getUserPhoneNumber(): String? {
        return encryptedPrefs.getString(KEY_USER_PHONE, null)
    }
    
    /**
     * Check if the app is configured with essential settings.
     */
    fun isConfigured(): Boolean {
        return !getApiEndpoint().isNullOrBlank() && !getMerchantCode().isNullOrBlank()
    }
    
    /**
     * Clear all stored secure data.
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }
    
    /**
     * Clear only authentication-related data (for logout).
     */
    fun clearAuthData() {
        encryptedPrefs.edit()
            .remove(KEY_API_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_PHONE)
            .apply()
    }
    
    companion object {
        private const val SECURE_PREFS_NAME = "momo_secure_prefs"
        private const val KEY_MERCHANT_CODE = "merchant_code"
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_API_ENDPOINT = "api_endpoint"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_API_SECRET = "api_secret"
        
        // Authentication-specific keys
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_PHONE = "user_phone"
    }
}
