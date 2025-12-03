package com.momoterminal.feature.auth

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
        // Token refresh buffer (refresh 5 minutes before expiry)
        private const val REFRESH_BUFFER_MS = 5 * 60 * 1000L
    }

    init {
        // Check for existing valid token on initialization
        checkExistingToken()
    }

    private fun checkExistingToken() {
        val token = secureStorage.getApiToken()
        val expiry = secureStorage.getTokenExpiry()
        
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
        secureStorage.saveRefreshToken(refreshToken)
        secureStorage.saveTokenExpiry(expiryTime)
        
        _accessToken.value = accessToken
        _isAuthenticated.value = true
    }

    /**
     * Save user information after successful authentication.
     */
    fun saveUserInfo(userId: String, phoneNumber: String) {
        secureStorage.saveUserId(userId)
        secureStorage.saveUserPhoneNumber(phoneNumber)
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
        return secureStorage.getRefreshToken()
    }

    /**
     * Get the stored user ID.
     */
    fun getUserId(): String? {
        return secureStorage.getUserId()
    }

    /**
     * Get the stored phone number.
     */
    fun getPhoneNumber(): String? {
        return secureStorage.getUserPhoneNumber()
    }

    /**
     * Check if the access token needs to be refreshed.
     */
    fun needsRefresh(): Boolean {
        val expiry = secureStorage.getTokenExpiry()
        val currentTime = System.currentTimeMillis()
        return expiry > 0 && (expiry - REFRESH_BUFFER_MS) < currentTime
    }

    /**
     * Check if the access token has expired.
     */
    fun isTokenExpired(): Boolean {
        val expiry = secureStorage.getTokenExpiry()
        return expiry > 0 && expiry < System.currentTimeMillis()
    }

    /**
     * Update the access token after refresh.
     */
    fun updateAccessToken(newToken: String, expiresInSeconds: Long) {
        val expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000)
        
        secureStorage.saveApiToken(newToken)
        secureStorage.saveTokenExpiry(expiryTime)
        
        _accessToken.value = newToken
    }

    /**
     * Clear all stored tokens and user data (logout).
     */
    fun clearTokens() {
        secureStorage.clearAuthData()
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
}
