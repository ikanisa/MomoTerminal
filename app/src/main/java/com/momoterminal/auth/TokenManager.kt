package com.momoterminal.auth

import com.momoterminal.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages JWT tokens securely using EncryptedSharedPreferences.
 * Handles token storage, retrieval, refresh, and expiration.
 */
@Singleton
class TokenManager @Inject constructor(
    private val secureStorage: SecureStorage
) {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PHONE_NUMBER = "phone_number"
        
        // Token refresh buffer (refresh 5 minutes before expiry)
        private const val REFRESH_BUFFER_MS = 5 * 60 * 1000L
    }

    init {
        // Check for existing valid token on initialization
        checkExistingToken()
    }

    private fun checkExistingToken() {
        val token = secureStorage.getApiToken()
        val expiry = getTokenExpiry()
        
        if (token != null && expiry > System.currentTimeMillis()) {
            _accessToken.value = token
            _isAuthenticated.value = true
        } else if (token != null) {
            // Token exists but expired - clear it
            clearTokens()
        }
    }

    /**
     * Save authentication tokens securely.
     */
    fun saveTokens(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long
    ) {
        val expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000)
        
        secureStorage.saveApiToken(accessToken)
        saveRefreshToken(refreshToken)
        saveTokenExpiry(expiryTime)
        
        _accessToken.value = accessToken
        _isAuthenticated.value = true
    }

    /**
     * Save user information after successful authentication.
     */
    fun saveUserInfo(userId: String, phoneNumber: String) {
        saveSecureValue(KEY_USER_ID, userId)
        saveSecureValue(KEY_PHONE_NUMBER, phoneNumber)
    }

    /**
     * Get the current access token.
     */
    fun getAccessToken(): String? {
        return secureStorage.getApiToken()
    }

    /**
     * Get the refresh token.
     */
    fun getRefreshToken(): String? {
        return getSecureValue(KEY_REFRESH_TOKEN)
    }

    /**
     * Get the stored user ID.
     */
    fun getUserId(): String? {
        return getSecureValue(KEY_USER_ID)
    }

    /**
     * Get the stored phone number.
     */
    fun getPhoneNumber(): String? {
        return getSecureValue(KEY_PHONE_NUMBER)
    }

    /**
     * Check if the access token needs to be refreshed.
     */
    fun needsRefresh(): Boolean {
        val expiry = getTokenExpiry()
        val currentTime = System.currentTimeMillis()
        return expiry > 0 && (expiry - REFRESH_BUFFER_MS) < currentTime
    }

    /**
     * Check if the access token has expired.
     */
    fun isTokenExpired(): Boolean {
        val expiry = getTokenExpiry()
        return expiry > 0 && expiry < System.currentTimeMillis()
    }

    /**
     * Update the access token after refresh.
     */
    fun updateAccessToken(newToken: String, expiresInSeconds: Long) {
        val expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000)
        
        secureStorage.saveApiToken(newToken)
        saveTokenExpiry(expiryTime)
        
        _accessToken.value = newToken
    }

    /**
     * Clear all stored tokens and user data (logout).
     */
    fun clearTokens() {
        secureStorage.clearAll()
        _accessToken.value = null
        _isAuthenticated.value = false
    }

    /**
     * Check if user is currently authenticated with a valid token.
     */
    fun hasValidToken(): Boolean {
        val token = getAccessToken()
        return token != null && !isTokenExpired()
    }

    // Helper methods for secure storage
    private fun saveRefreshToken(token: String) {
        saveSecureValue(KEY_REFRESH_TOKEN, token)
    }

    private fun saveTokenExpiry(expiry: Long) {
        saveSecureValue(KEY_TOKEN_EXPIRY, expiry.toString())
    }

    private fun getTokenExpiry(): Long {
        return getSecureValue(KEY_TOKEN_EXPIRY)?.toLongOrNull() ?: 0L
    }

    private fun saveSecureValue(key: String, value: String) {
        // Using SecureStorage's methods - we can extend it if needed
        when (key) {
            KEY_REFRESH_TOKEN -> secureStorage.saveApiSecret(value)
            KEY_TOKEN_EXPIRY -> secureStorage.saveDeviceId(value)
            KEY_USER_ID -> secureStorage.saveMerchantCode(value)
            KEY_PHONE_NUMBER -> secureStorage.saveApiEndpoint(value)
        }
    }

    private fun getSecureValue(key: String): String? {
        return when (key) {
            KEY_REFRESH_TOKEN -> secureStorage.getApiSecret()
            KEY_TOKEN_EXPIRY -> secureStorage.getDeviceId()
            KEY_USER_ID -> secureStorage.getMerchantCode()
            KEY_PHONE_NUMBER -> secureStorage.getApiEndpoint()
            else -> null
        }
    }
}
