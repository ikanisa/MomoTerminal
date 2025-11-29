package com.momoterminal.auth

import app.cash.turbine.test
import com.momoterminal.api.AuthApiService
import com.momoterminal.api.AuthResponse
import com.momoterminal.api.OtpResponse
import com.momoterminal.api.User
import com.momoterminal.supabase.AuthResult as SupabaseAuthResult
import com.momoterminal.supabase.SessionData
import com.momoterminal.supabase.SupabaseAuthService
import com.momoterminal.supabase.SupabaseUser
import io.mockk.coEvery
import io.mockk.coVerify
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
    fun `login success with Supabase saves tokens and starts session`() = runTest {
        // Given
        val sessionData = SessionData(
            accessToken = "test_access_token",
            refreshToken = "test_refresh_token",
            expiresIn = 3600L,
            expiresAt = System.currentTimeMillis() / 1000 + 3600,
            user = SupabaseUser(
                id = "user123",
<<<<<<< HEAD
                phone = "+250788123456"
                phone = "+250788767816"
=======
                phone = "0201234567",
                email = null,
                createdAt = "2023-01-01",
                updatedAt = "2023-01-01"
>>>>>>> 0787b13 (Add OTP testing documentation and update test configurations)
            )
        )
        coEvery { supabaseAuthService.verifyOtp(any(), any()) } returns SupabaseAuthResult.Success(sessionData)

        // When & Then
        authRepository.login("+250788123456", "123456").test {
        authRepository.login("+250788767816", "123456").test {
            // First emission should be Loading
            assertEquals(AuthRepository.AuthResult.Loading, awaitItem())
            
            // Second emission should be Success
            val result = awaitItem()
            assertTrue(result is AuthRepository.AuthResult.Success)
<<<<<<< HEAD
            val authResponse = (result as AuthRepository.AuthResult.Success).data
            assertEquals("test_access_token", authResponse.accessToken)
            assertEquals("user123", authResponse.user.id)
=======
            val data = (result as AuthRepository.AuthResult.Success).data
            assertEquals("test_access_token", data.accessToken)
            assertEquals("user123", data.user.id)
>>>>>>> 0787b13 (Add OTP testing documentation and update test configurations)
            
            awaitComplete()
        }

        // Verify tokens were saved
        verify { tokenManager.saveTokens("test_access_token", "test_refresh_token", 3600L) }
        verify { tokenManager.saveUserInfo("user123", "+250788123456") }
        verify { tokenManager.saveUserInfo("user123", "+250788767816") }
        verify { sessionManager.startSession() }
    }

    @Test
    fun `login failure with Supabase returns error`() = runTest {
        // Given
        coEvery { supabaseAuthService.verifyOtp(any(), any()) } returns SupabaseAuthResult.Error("Invalid OTP")
<<<<<<< HEAD

        // When & Then
        authRepository.login("+250788123456", "wrong_otp").test {
        coEvery { supabaseAuthService.verifyOtp(any(), any()) } returns SupabaseAuthResult.Error(
            message = "Invalid OTP code",
            code = "OTP_VERIFICATION_FAILED"
        )

        // When & Then
        authRepository.login("+250788767816", "wrong_otp").test {
=======

        // When & Then
        authRepository.login("0201234567", "wrong_otp").test {
>>>>>>> 0787b13 (Add OTP testing documentation and update test configurations)
            // First emission should be Loading
            assertEquals(AuthRepository.AuthResult.Loading, awaitItem())
            
            // Second emission should be Error
            val result = awaitItem()
            assertTrue(result is AuthRepository.AuthResult.Error)
<<<<<<< HEAD
            assertEquals("Invalid OTP code", (result as AuthRepository.AuthResult.Error).message)
            
            awaitComplete()
        }
    }

    @Test
    fun `login network error returns error`() = runTest {
        // Given
        coEvery { supabaseAuthService.verifyOtp(any(), any()) } throws Exception("Network error")

        // When & Then
        authRepository.login("+250788123456", "123456").test {
        authRepository.login("+250788767816", "123456").test {
            // First emission should be Loading
            assertEquals(AuthRepository.AuthResult.Loading, awaitItem())
            
            // Second emission should be Error
            val result = awaitItem()
            assertTrue(result is AuthRepository.AuthResult.Error)
            assertTrue((result as AuthRepository.AuthResult.Error).message.contains("Network error"))
=======
            assertEquals("Invalid OTP", (result as AuthRepository.AuthResult.Error).message)
>>>>>>> 0787b13 (Add OTP testing documentation and update test configurations)
            
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
    fun `requestOtp success with Supabase returns success`() = runTest {
        // Given
<<<<<<< HEAD
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
        authRepository.verifyOtp("+250788767816", "123456").test {
            assertEquals(AuthRepository.AuthResult.Loading, awaitItem())
            
            val result = awaitItem()
            assertTrue(result is AuthRepository.AuthResult.Success)
            assertEquals(otpResponse, (result as AuthRepository.AuthResult.Success).data)
            
            awaitComplete()
        }
    }

    @Test
    fun `requestOtp success via Supabase returns success`() = runTest {
        // Given
=======
>>>>>>> 0787b13 (Add OTP testing documentation and update test configurations)
        coEvery { supabaseAuthService.sendWhatsAppOtp(any()) } returns SupabaseAuthResult.Success(Unit)

        // When & Then
        authRepository.requestOtp("+250788123456").test {
        authRepository.requestOtp("+250788767816").test {
            assertEquals(AuthRepository.AuthResult.Loading, awaitItem())
            
            val result = awaitItem()
            assertTrue(result is AuthRepository.AuthResult.Success)
            val response = (result as AuthRepository.AuthResult.Success).data
            assertTrue(response.success)
            
            awaitComplete()
        }
    }
    
    @Test
    fun `requestOtp failure via Supabase returns error`() = runTest {
        // Given
        coEvery { supabaseAuthService.sendWhatsAppOtp(any()) } returns SupabaseAuthResult.Error(
            message = "Rate limit exceeded",
            code = "rate_limit_exceeded"
        )

        // When & Then
        authRepository.requestOtp("+250788767816").test {
            assertEquals(AuthRepository.AuthResult.Loading, awaitItem())
            
            val result = awaitItem()
            assertTrue(result is AuthRepository.AuthResult.Error)
            assertEquals("Rate limit exceeded", (result as AuthRepository.AuthResult.Error).message)
            
            awaitComplete()
        }
        
        coVerify { supabaseAuthService.sendWhatsAppOtp("0201234567") }
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
                phoneNumber = "+250788767816"
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
<<<<<<< HEAD

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
    
    @Test
    fun `no hardcoded dev bypass exists`() = runTest {
        // This test verifies that there is no hardcoded bypass in the login flow
        // The dev bypass was a security vulnerability that has been removed
        
        // Given - Mock Supabase to fail (simulating invalid OTP)
        coEvery { supabaseAuthService.verifyOtp(any(), any()) } returns SupabaseAuthResult.Error(
            message = "Invalid OTP",
            code = "OTP_VERIFICATION_FAILED"
        )

        // When - Try the previously hardcoded credentials
        authRepository.login("0788767816", "123456").test {
            assertEquals(AuthRepository.AuthResult.Loading, awaitItem())
            
            // Should get an error, not success (no bypass)
            val result = awaitItem()
            assertTrue(result is AuthRepository.AuthResult.Error)
            
            awaitComplete()
        }
        
        // Verify no tokens were saved (bypass didn't work)
        verify(exactly = 0) { tokenManager.saveTokens(any(), any(), any()) }
    }
=======
>>>>>>> 0787b13 (Add OTP testing documentation and update test configurations)
}
