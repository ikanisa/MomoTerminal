package com.momoterminal.auth

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.momoterminal.api.AuthApiService
import com.momoterminal.api.AuthResponse
import com.momoterminal.api.OtpResponse
import com.momoterminal.api.User
import com.momoterminal.supabase.AuthResult as SupabaseAuthResult
import com.momoterminal.supabase.SessionData
import com.momoterminal.supabase.SupabaseAuthService
import com.momoterminal.supabase.SupabaseUser
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Comprehensive tests for WhatsApp OTP integration.
 * Tests the complete flow from OTP request to verification and session creation.
 */
class OtpWhatsAppIntegrationTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var supabaseAuthService: SupabaseAuthService
    private lateinit var authApiService: AuthApiService
    private lateinit var tokenManager: TokenManager
    private lateinit var sessionManager: SessionManager

    private val testPhoneNumber = "+250788767816"
    private val testOtpCode = "123456"
    private val testAccessToken = "test_access_token"
    private val testRefreshToken = "test_refresh_token"
    private val testUserId = "test_user_123"

    @Before
    fun setup() {
        supabaseAuthService = mockk()
        authApiService = mockk()
        tokenManager = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)

        authRepository = AuthRepository(
            authApiService = authApiService,
            supabaseAuthService = supabaseAuthService,
            tokenManager = tokenManager,
            sessionManager = sessionManager
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ============== OTP Request Tests ==============

    @Test
    fun `requestOtp sends WhatsApp OTP successfully`() = runTest {
        // Given
        coEvery { 
            supabaseAuthService.sendWhatsAppOtp(testPhoneNumber) 
        } returns SupabaseAuthResult.Success(Unit)

        // When & Then
        authRepository.requestOtp(testPhoneNumber).test {
            // Should emit Loading first
            val loading = awaitItem()
            assertThat(loading).isInstanceOf(AuthRepository.AuthResult.Loading::class.java)

            // Then emit Success
            val success = awaitItem()
            assertThat(success).isInstanceOf(AuthRepository.AuthResult.Success::class.java)
            
            val response = (success as AuthRepository.AuthResult.Success).data
            assertThat(response.success).isTrue()
            assertThat(response.message).contains("WhatsApp")

            awaitComplete()
        }

        // Verify Supabase was called
        coVerify(exactly = 1) { supabaseAuthService.sendWhatsAppOtp(testPhoneNumber) }
    }

    @Test
    fun `requestOtp handles network failure`() = runTest {
        // Given
        val errorMessage = "Network connection failed"
        coEvery { 
            supabaseAuthService.sendWhatsAppOtp(testPhoneNumber) 
        } returns SupabaseAuthResult.Error(errorMessage, "NETWORK_ERROR")

        // When & Then
        authRepository.requestOtp(testPhoneNumber).test {
            val loading = awaitItem()
            assertThat(loading).isInstanceOf(AuthRepository.AuthResult.Loading::class.java)

            val error = awaitItem()
            assertThat(error).isInstanceOf(AuthRepository.AuthResult.Error::class.java)
            assertThat((error as AuthRepository.AuthResult.Error).message).isEqualTo(errorMessage)

            awaitComplete()
        }
    }

    @Test
    fun `requestOtp handles Supabase exception`() = runTest {
        // Given
        coEvery { 
            supabaseAuthService.sendWhatsAppOtp(testPhoneNumber) 
        } throws IOException("Network timeout")

        // When & Then
        authRepository.requestOtp(testPhoneNumber).test {
            val loading = awaitItem()
            assertThat(loading).isInstanceOf(AuthRepository.AuthResult.Loading::class.java)

            val error = awaitItem()
            assertThat(error).isInstanceOf(AuthRepository.AuthResult.Error::class.java)
            assertThat((error as AuthRepository.AuthResult.Error).message).contains("Network timeout")

            awaitComplete()
        }
    }

    @Test
    fun `requestOtp handles invalid phone number format`() = runTest {
        // Given
        val invalidPhone = "invalid_phone"
        val errorMessage = "Invalid phone number format"
        coEvery { 
            supabaseAuthService.sendWhatsAppOtp(invalidPhone) 
        } returns SupabaseAuthResult.Error(errorMessage, "INVALID_PHONE")

        // When & Then
        authRepository.requestOtp(invalidPhone).test {
            awaitItem() // Loading
            
            val error = awaitItem()
            assertThat(error).isInstanceOf(AuthRepository.AuthResult.Error::class.java)

            awaitComplete()
        }
    }

    // ============== OTP Verification Tests ==============

    @Test
    fun `login verifies OTP and creates session successfully`() = runTest {
        // Given
        val sessionData = SessionData(
            accessToken = testAccessToken,
            refreshToken = testRefreshToken,
            expiresIn = 3600,
            expiresAt = System.currentTimeMillis() / 1000 + 3600,
            user = SupabaseUser(
                id = testUserId,
                phone = testPhoneNumber,
                email = null,
                createdAt = "2024-01-01",
                updatedAt = "2024-01-01"
            )
        )

        coEvery { 
            supabaseAuthService.verifyOtp(testPhoneNumber, testOtpCode) 
        } returns SupabaseAuthResult.Success(sessionData)

        // When & Then
        authRepository.login(testPhoneNumber, testOtpCode).test {
            val loading = awaitItem()
            assertThat(loading).isInstanceOf(AuthRepository.AuthResult.Loading::class.java)

            val success = awaitItem()
            assertThat(success).isInstanceOf(AuthRepository.AuthResult.Success::class.java)
            
            val authResponse = (success as AuthRepository.AuthResult.Success).data
            assertThat(authResponse.accessToken).isEqualTo(testAccessToken)
            assertThat(authResponse.refreshToken).isEqualTo(testRefreshToken)
            assertThat(authResponse.user.id).isEqualTo(testUserId)
            assertThat(authResponse.user.phoneNumber).isEqualTo(testPhoneNumber)

            awaitComplete()
        }

        // Verify tokens were saved
        verify { 
            tokenManager.saveTokens(
                accessToken = testAccessToken,
                refreshToken = testRefreshToken,
                expiresInSeconds = 3600
            )
        }

        // Verify user info was saved
        verify { 
            tokenManager.saveUserInfo(
                userId = testUserId,
                phoneNumber = testPhoneNumber
            )
        }

        // Verify session was started
        verify(exactly = 1) { sessionManager.startSession() }
    }

    @Test
    fun `login handles invalid OTP code`() = runTest {
        // Given
        val errorMessage = "Invalid OTP code"
        coEvery { 
            supabaseAuthService.verifyOtp(testPhoneNumber, "999999") 
        } returns SupabaseAuthResult.Error(errorMessage, "INVALID_OTP")

        // When & Then
        authRepository.login(testPhoneNumber, "999999").test {
            awaitItem() // Loading

            val error = awaitItem()
            assertThat(error).isInstanceOf(AuthRepository.AuthResult.Error::class.java)
            assertThat((error as AuthRepository.AuthResult.Error).message).isEqualTo(errorMessage)

            awaitComplete()
        }

        // Verify no tokens were saved
        verify(exactly = 0) { tokenManager.saveTokens(any(), any(), any()) }
        verify(exactly = 0) { sessionManager.startSession() }
    }

    @Test
    fun `login handles expired OTP code`() = runTest {
        // Given
        val errorMessage = "OTP code has expired"
        coEvery { 
            supabaseAuthService.verifyOtp(testPhoneNumber, testOtpCode) 
        } returns SupabaseAuthResult.Error(errorMessage, "OTP_EXPIRED")

        // When & Then
        authRepository.login(testPhoneNumber, testOtpCode).test {
            awaitItem() // Loading

            val error = awaitItem()
            assertThat(error).isInstanceOf(AuthRepository.AuthResult.Error::class.java)
            assertThat((error as AuthRepository.AuthResult.Error).message).contains("expired")

            awaitComplete()
        }
    }

    @Test
    fun `login handles network error during verification`() = runTest {
        // Given
        coEvery { 
            supabaseAuthService.verifyOtp(testPhoneNumber, testOtpCode) 
        } throws IOException("Connection timeout")

        // When & Then
        authRepository.login(testPhoneNumber, testOtpCode).test {
            awaitItem() // Loading

            val error = awaitItem()
            assertThat(error).isInstanceOf(AuthRepository.AuthResult.Error::class.java)

            awaitComplete()
        }
    }

    // ============== Session Persistence Tests ==============

    @Test
    fun `session is properly persisted after successful login`() = runTest {
        // Given
        val sessionData = createTestSessionData()
        coEvery { 
            supabaseAuthService.verifyOtp(testPhoneNumber, testOtpCode) 
        } returns SupabaseAuthResult.Success(sessionData)

        // When
        authRepository.login(testPhoneNumber, testOtpCode).test {
            awaitItem() // Loading
            awaitItem() // Success
            awaitComplete()
        }

        // Then - Verify session persistence
        verify(exactly = 1) { sessionManager.startSession() }
        verify { 
            tokenManager.saveTokens(
                accessToken = any(),
                refreshToken = any(),
                expiresInSeconds = any()
            )
        }
        verify { 
            tokenManager.saveUserInfo(
                userId = any(),
                phoneNumber = any()
            )
        }
    }

    @Test
    fun `session is not created on verification failure`() = runTest {
        // Given
        coEvery { 
            supabaseAuthService.verifyOtp(testPhoneNumber, testOtpCode) 
        } returns SupabaseAuthResult.Error("Verification failed", "ERROR")

        // When
        authRepository.login(testPhoneNumber, testOtpCode).test {
            awaitItem() // Loading
            awaitItem() // Error
            awaitComplete()
        }

        // Then - Verify no session was created
        verify(exactly = 0) { sessionManager.startSession() }
        verify(exactly = 0) { tokenManager.saveTokens(any(), any(), any()) }
    }

    // ============== Error Handling & Edge Cases ==============

    @Test
    fun `handles multiple OTP requests for same number`() = runTest {
        // Given
        coEvery { 
            supabaseAuthService.sendWhatsAppOtp(testPhoneNumber) 
        } returns SupabaseAuthResult.Success(Unit)

        // When - Request OTP twice
        authRepository.requestOtp(testPhoneNumber).test {
            awaitItem() // Loading
            awaitItem() // Success
            awaitComplete()
        }

        authRepository.requestOtp(testPhoneNumber).test {
            awaitItem() // Loading
            awaitItem() // Success
            awaitComplete()
        }

        // Then - Both requests should succeed
        coVerify(exactly = 2) { supabaseAuthService.sendWhatsAppOtp(testPhoneNumber) }
    }

    @Test
    fun `handles verification without prior OTP request`() = runTest {
        // Given - User tries to verify without requesting OTP first
        val errorMessage = "No pending OTP for this number"
        coEvery { 
            supabaseAuthService.verifyOtp(testPhoneNumber, testOtpCode) 
        } returns SupabaseAuthResult.Error(errorMessage, "NO_PENDING_OTP")

        // When & Then
        authRepository.login(testPhoneNumber, testOtpCode).test {
            awaitItem() // Loading
            
            val error = awaitItem()
            assertThat(error).isInstanceOf(AuthRepository.AuthResult.Error::class.java)

            awaitComplete()
        }
    }

    @Test
    fun `handles empty OTP code`() = runTest {
        // Given
        val errorMessage = "OTP code cannot be empty"
        coEvery { 
            supabaseAuthService.verifyOtp(testPhoneNumber, "") 
        } returns SupabaseAuthResult.Error(errorMessage, "EMPTY_OTP")

        // When & Then
        authRepository.login(testPhoneNumber, "").test {
            awaitItem() // Loading
            
            val error = awaitItem()
            assertThat(error).isInstanceOf(AuthRepository.AuthResult.Error::class.java)

            awaitComplete()
        }
    }

    @Test
    fun `handles OTP code with wrong length`() = runTest {
        // Given
        val shortOtp = "123"
        val errorMessage = "OTP must be 6 digits"
        coEvery { 
            supabaseAuthService.verifyOtp(testPhoneNumber, shortOtp) 
        } returns SupabaseAuthResult.Error(errorMessage, "INVALID_OTP_LENGTH")

        // When & Then
        authRepository.login(testPhoneNumber, shortOtp).test {
            awaitItem() // Loading
            
            val error = awaitItem()
            assertThat(error).isInstanceOf(AuthRepository.AuthResult.Error::class.java)

            awaitComplete()
        }
    }

    // ============== Integration with Existing Features ==============

    @Test
    fun `authenticated user can access existing features`() = runTest {
        // Given - Successful login
        val sessionData = createTestSessionData()
        coEvery { 
            supabaseAuthService.verifyOtp(testPhoneNumber, testOtpCode) 
        } returns SupabaseAuthResult.Success(sessionData)

        every { tokenManager.hasValidToken() } returns true
        every { sessionManager.validateSession() } returns true

        // When
        authRepository.login(testPhoneNumber, testOtpCode).test {
            awaitItem() // Loading
            awaitItem() // Success
            awaitComplete()
        }

        // Then - User should be authenticated
        assertThat(authRepository.isAuthenticated()).isTrue()
        assertThat(authRepository.validateSession()).isTrue()
    }

    @Test
    fun `logout clears WhatsApp OTP session`() = runTest {
        // Given - User is logged in
        val sessionData = createTestSessionData()
        coEvery { 
            supabaseAuthService.verifyOtp(testPhoneNumber, testOtpCode) 
        } returns SupabaseAuthResult.Success(sessionData)

        authRepository.login(testPhoneNumber, testOtpCode).test {
            awaitItem() // Loading
            awaitItem() // Success
            awaitComplete()
        }

        // When - User logs out
        authRepository.logout()

        // Then - Session should be cleared
        verify(exactly = 1) { tokenManager.clearTokens() }
        verify(exactly = 1) { sessionManager.endSession() }
    }

    // ============== Helper Methods ==============

    private fun createTestSessionData(): SessionData {
        return SessionData(
            accessToken = testAccessToken,
            refreshToken = testRefreshToken,
            expiresIn = 3600,
            expiresAt = System.currentTimeMillis() / 1000 + 3600,
            user = SupabaseUser(
                id = testUserId,
                phone = testPhoneNumber,
                email = null,
                createdAt = "2024-01-01",
                updatedAt = "2024-01-01"
            )
        )
    }
}
