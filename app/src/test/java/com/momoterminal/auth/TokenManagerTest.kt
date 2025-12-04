package com.momoterminal.auth

import com.momoterminal.core.security.SecureStorage
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
        verify { secureStorage.saveRefreshToken(refreshToken) }
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
        verify { secureStorage.clearAuthData() }
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
        // Token expired 1 hour ago
        every { secureStorage.getTokenExpiry() } returns System.currentTimeMillis() - (60 * 60 * 1000)

        // Create fresh instance
        val freshTokenManager = TokenManager(secureStorage)

        // When
        val result = freshTokenManager.hasValidToken()

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
        verify { secureStorage.saveUserId(userId) }
        verify { secureStorage.saveUserPhoneNumber(phoneNumber) }
    }

    @Test
    fun `needsRefresh returns true when close to expiry`() {
        // Given - token expires in 2 minutes (less than 5 minute buffer)
        val expiryTime = System.currentTimeMillis() + (2 * 60 * 1000)
        every { secureStorage.getTokenExpiry() } returns expiryTime
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
        every { secureStorage.getTokenExpiry() } returns expiryTime

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
        every { secureStorage.getTokenExpiry() } returns expiryTime

        // Create fresh instance
        val freshTokenManager = TokenManager(secureStorage)

        // When
        val result = freshTokenManager.isTokenExpired()

        // Then
        assertFalse(result)
    }
    
    @Test
    fun `getRefreshToken returns token from secure storage`() {
        // Given
        val expectedToken = "test_refresh_token"
        every { secureStorage.getRefreshToken() } returns expectedToken

        // When
        val result = tokenManager.getRefreshToken()

        // Then
        assertEquals(expectedToken, result)
    }
    
    @Test
    fun `getUserId returns user ID from secure storage`() {
        // Given
        val expectedUserId = "user123"
        every { secureStorage.getUserId() } returns expectedUserId

        // When
        val result = tokenManager.getUserId()

        // Then
        assertEquals(expectedUserId, result)
    }
    
    @Test
    fun `getPhoneNumber returns phone from secure storage`() {
        // Given
        val expectedPhone = "0201234567"
        every { secureStorage.getUserPhoneNumber() } returns expectedPhone

        // When
        val result = tokenManager.getPhoneNumber()

        // Then
        assertEquals(expectedPhone, result)
    }
}
