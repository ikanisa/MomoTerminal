package com.momoterminal.auth

import app.cash.turbine.test
import com.momoterminal.api.AuthApiService
import com.momoterminal.api.AuthResponse
import com.momoterminal.api.OtpResponse
import com.momoterminal.api.User
import com.momoterminal.supabase.SupabaseAuthService
import com.momoterminal.supabase.SessionData
import com.momoterminal.supabase.SupabaseUser
import com.momoterminal.supabase.AuthResult as SupabaseAuthResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for AuthRepository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var authApiService: AuthApiService
    private lateinit var supabaseAuthService: SupabaseAuthService
    private lateinit var tokenManager: TokenManager
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        authApiService = mockk(relaxed = true)
        supabaseAuthService = mockk(relaxed = true)
        tokenManager = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        
        authRepository = AuthRepository(authApiService, supabaseAuthService, tokenManager, sessionManager)
    }

    @Test
    fun `login success saves tokens and starts session`() = runTest {
        // Given
        val sessionData = SessionData(
            accessToken = "test_access_token",
            refreshToken = "test_refresh_token",
            expiresIn = 3600L,
            expiresAt = System.currentTimeMillis() / 1000 + 3600,
            user = SupabaseUser(
                id = "user123",
                phone = "+250788123456"
            )
        )
        coEvery { supabaseAuthService.verifyOtp(any(), any()) } returns SupabaseAuthResult.Success(sessionData)

        // When & Then
        authRepository.login("+250788123456", "123456").test {
            // First emission should be Loading
            assertEquals(AuthRepository.AuthResult.Loading, awaitItem())
            
            // Second emission should be Success
            val result = awaitItem()
            assertTrue(result is AuthRepository.AuthResult.Success)
            
            awaitComplete()
        }

        // Verify tokens were saved
        verify { tokenManager.saveTokens("test_access_token", "test_refresh_token", 3600L) }
        verify { tokenManager.saveUserInfo("user123", "+250788123456") }
        verify { sessionManager.startSession() }
    }

    @Test
    fun `login failure returns error`() = runTest {
        // Given
        coEvery { supabaseAuthService.verifyOtp(any(), any()) } returns SupabaseAuthResult.Error("Invalid OTP")

        // When & Then
        authRepository.login("+250788123456", "wrong_otp").test {
            // First emission should be Loading
            assertEquals(AuthRepository.AuthResult.Loading, awaitItem())
            
            // Second emission should be Error
            val result = awaitItem()
            assertTrue(result is AuthRepository.AuthResult.Error)
            
            awaitComplete()
        }
    }

    @Test
    fun `login network error returns error`() = runTest {
        // Given
        coEvery { supabaseAuthService.verifyOtp(any(), any()) } throws Exception("Network error")

        // When & Then
        authRepository.login("+250788123456", "123456").test {
            // First emission should be Loading
            assertEquals(AuthRepository.AuthResult.Loading, awaitItem())
            
            // Second emission should be Error
            val result = awaitItem()
            assertTrue(result is AuthRepository.AuthResult.Error)
            assertTrue((result as AuthRepository.AuthResult.Error).message.contains("Network error"))
            
            awaitComplete()
        }
    }

    @Test
    fun `logout clears tokens and ends session`() {
        // When
        authRepository.logout()

        // Then
        verify { tokenManager.clearTokens() }
        verify { sessionManager.endSession() }
    }

    @Test
    fun `isAuthenticated delegates to tokenManager`() {
        // Given
        every { tokenManager.hasValidToken() } returns true

        // When
        val result = authRepository.isAuthenticated()

        // Then
        assertTrue(result)
        verify { tokenManager.hasValidToken() }
    }

    @Test
    fun `validateSession delegates to sessionManager`() {
        // Given
        every { sessionManager.validateSession() } returns true

        // When
        val result = authRepository.validateSession()

        // Then
        assertTrue(result)
        verify { sessionManager.validateSession() }
    }

    @Test
    fun `getAccessToken delegates to tokenManager`() {
        // Given
        val expectedToken = "test_token"
        every { tokenManager.getAccessToken() } returns expectedToken

        // When
        val result = authRepository.getAccessToken()

        // Then
        assertEquals(expectedToken, result)
    }

    @Test
    fun `needsTokenRefresh delegates to tokenManager`() {
        // Given
        every { tokenManager.needsRefresh() } returns true

        // When
        val result = authRepository.needsTokenRefresh()

        // Then
        assertTrue(result)
    }

    @Test
    fun `verifyOtp success returns success`() = runTest {
        // Given
        val otpResponse = OtpResponse(
            success = true,
            message = "OTP verified successfully"
        )
        coEvery { authApiService.verifyOtp(any()) } returns Response.success(otpResponse)

        // When & Then
        authRepository.verifyOtp("+250788123456", "123456").test {
            assertEquals(AuthRepository.AuthResult.Loading, awaitItem())
            
            val result = awaitItem()
            assertTrue(result is AuthRepository.AuthResult.Success)
            assertEquals(otpResponse, (result as AuthRepository.AuthResult.Success).data)
            
            awaitComplete()
        }
    }

    @Test
    fun `requestOtp success returns success`() = runTest {
        // Given
        coEvery { supabaseAuthService.sendWhatsAppOtp(any()) } returns SupabaseAuthResult.Success(Unit)

        // When & Then
        authRepository.requestOtp("+250788123456").test {
            assertEquals(AuthRepository.AuthResult.Loading, awaitItem())
            
            val result = awaitItem()
            assertTrue(result is AuthRepository.AuthResult.Success)
            
            awaitComplete()
        }
    }

    @Test
    fun `refreshToken success updates access token`() = runTest {
        // Given
        val refreshToken = "test_refresh_token"
        val authResponse = AuthResponse(
            accessToken = "new_access_token",
            refreshToken = refreshToken,
            expiresIn = 3600L,
            user = User(
                id = "user123",
                phoneNumber = "+250788123456"
            )
        )
        every { tokenManager.getRefreshToken() } returns refreshToken
        coEvery { authApiService.refreshToken(any()) } returns Response.success(authResponse)

        // When
        val result = authRepository.refreshToken()

        // Then
        assertTrue(result)
        verify { tokenManager.updateAccessToken("new_access_token", 3600L) }
    }

    @Test
    fun `refreshToken failure logs out user`() = runTest {
        // Given
        every { tokenManager.getRefreshToken() } returns "test_refresh_token"
        coEvery { authApiService.refreshToken(any()) } returns Response.error(
            401,
            "Invalid refresh token".toResponseBody()
        )

        // When
        val result = authRepository.refreshToken()

        // Then
        assertFalse(result)
        verify { tokenManager.clearTokens() }
        verify { sessionManager.endSession() }
    }

    @Test
    fun `refreshToken returns false when no refresh token`() = runTest {
        // Given
        every { tokenManager.getRefreshToken() } returns null

        // When
        val result = authRepository.refreshToken()

        // Then
        assertFalse(result)
    }
}
