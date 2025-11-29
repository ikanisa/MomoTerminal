package com.momoterminal.auth

import com.momoterminal.security.SecureStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TokenManager.
 */
class TokenManagerTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var secureStorage: SecureStorage

    @Before
    fun setup() {
        secureStorage = mockk(relaxed = true)
        tokenManager = TokenManager(secureStorage)
    }

    @Test
    fun `saveTokens stores tokens and updates state`() = runTest {
        // Given
        val accessToken = "test_access_token"
        val refreshToken = "test_refresh_token"
        val expiresIn = 3600L

        // When
        tokenManager.saveTokens(accessToken, refreshToken, expiresIn)

        // Then
        verify { secureStorage.saveApiToken(accessToken) }
        assertTrue(tokenManager.isAuthenticated.first())
        assertEquals(accessToken, tokenManager.accessToken.first())
    }

    @Test
    fun `clearTokens removes all tokens and updates state`() = runTest {
        // Given
        every { secureStorage.getApiToken() } returns "test_token"
        
        // When
        tokenManager.clearTokens()

        // Then
        verify { secureStorage.clearAll() }
        assertFalse(tokenManager.isAuthenticated.first())
        assertNull(tokenManager.accessToken.first())
    }

    @Test
    fun `hasValidToken returns false when no token`() {
        // Given
        every { secureStorage.getApiToken() } returns null

        // When
        val result = tokenManager.hasValidToken()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasValidToken returns false when token expired`() {
        // Given
        every { secureStorage.getApiToken() } returns "test_token"
        every { secureStorage.getDeviceId() } returns "0" // Expiry time in the past

        // When
        val result = tokenManager.hasValidToken()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getAccessToken returns token from secure storage`() {
        // Given
        val expectedToken = "test_access_token"
        every { secureStorage.getApiToken() } returns expectedToken

        // When
        val result = tokenManager.getAccessToken()

        // Then
        assertEquals(expectedToken, result)
    }

    @Test
    fun `updateAccessToken updates token and expiry`() {
        // Given
        val newToken = "new_access_token"
        val expiresIn = 3600L

        // When
        tokenManager.updateAccessToken(newToken, expiresIn)

        // Then
        verify { secureStorage.saveApiToken(newToken) }
    }

    @Test
    fun `saveUserInfo stores user data securely`() {
        // Given
        val userId = "user123"
        val phoneNumber = "0201234567"

        // When
        tokenManager.saveUserInfo(userId, phoneNumber)

        // Then
        verify { secureStorage.saveMerchantCode(userId) }
        verify { secureStorage.saveApiEndpoint(phoneNumber) }
    }

    @Test
    fun `needsRefresh returns true when close to expiry`() {
        // Given - token expires in 2 minutes (less than 5 minute buffer)
        val expiryTime = System.currentTimeMillis() + (2 * 60 * 1000)
        every { secureStorage.getDeviceId() } returns expiryTime.toString()
        every { secureStorage.getApiToken() } returns "test_token"

        // Create fresh instance to pick up the mocked values
        val freshTokenManager = TokenManager(secureStorage)

        // When
        val result = freshTokenManager.needsRefresh()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isTokenExpired returns true when past expiry`() {
        // Given - token expired 1 hour ago
        val expiryTime = System.currentTimeMillis() - (60 * 60 * 1000)
        every { secureStorage.getDeviceId() } returns expiryTime.toString()

        // Create fresh instance
        val freshTokenManager = TokenManager(secureStorage)

        // When
        val result = freshTokenManager.isTokenExpired()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isTokenExpired returns false when token still valid`() {
        // Given - token expires in 1 hour
        val expiryTime = System.currentTimeMillis() + (60 * 60 * 1000)
        every { secureStorage.getDeviceId() } returns expiryTime.toString()

        // Create fresh instance
        val freshTokenManager = TokenManager(secureStorage)

        // When
        val result = freshTokenManager.isTokenExpired()

        // Then
        assertFalse(result)
    }
}
