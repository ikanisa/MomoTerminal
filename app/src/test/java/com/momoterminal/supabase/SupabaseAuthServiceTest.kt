package com.momoterminal.supabase

import com.google.common.truth.Truth.assertThat
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.user.UserSession
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for SupabaseAuthService.
 * Tests WhatsApp OTP sending, verification, and session management.
 */
class SupabaseAuthServiceTest {

    private lateinit var supabaseAuthService: SupabaseAuthService
    private lateinit var auth: Auth
    private lateinit var edgeFunctionsApi: EdgeFunctionsApi

    private val testPhoneNumber = "+250788767816"
    private val testOtpCode = "123456"
    private val testAccessToken = "supabase_access_token"
    private val testRefreshToken = "supabase_refresh_token"
    private val testUserId = "supabase_user_123"

    @Before
    fun setup() {
        auth = mockk(relaxed = true)
        edgeFunctionsApi = mockk(relaxed = true)
        supabaseAuthService = SupabaseAuthService(auth, edgeFunctionsApi)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ============== WhatsApp OTP Sending Tests ==============

    @Test
    fun `sendWhatsAppOtp sends OTP successfully`() = runTest {
        // Given - mock EdgeFunctionsApi response
        coEvery { edgeFunctionsApi.sendWhatsAppOtp(any()) } returns Response.success(
            SendOtpResponse(success = true, message = "OTP sent")
        )

        // When
        val result = supabaseAuthService.sendWhatsAppOtp(testPhoneNumber)

        // Then
        assertThat(result).isInstanceOf(AuthResult.Success::class.java)
    }

    @Test
    fun `sendWhatsAppOtp handles invalid phone number`() = runTest {
        // Given
        coEvery { edgeFunctionsApi.sendWhatsAppOtp(any()) } returns Response.success(
            SendOtpResponse(success = false, message = "Failed", error = "Invalid phone number format")
        )

        // When
        val result = supabaseAuthService.sendWhatsAppOtp("invalid")

        // Then
        assertThat(result).isInstanceOf(AuthResult.Error::class.java)
        val error = result as AuthResult.Error
        assertThat(error.code).isEqualTo("OTP_SEND_FAILED")
    }

    @Test
    fun `verifyOtp verifies code successfully and returns session`() = runTest {
        // Given - mock EdgeFunctionsApi response
        coEvery { edgeFunctionsApi.verifyWhatsAppOtp(any()) } returns Response.success(
            VerifyOtpResponse(
                success = true,
                message = "Verified",
                userId = testUserId,
                accessToken = testAccessToken,
                refreshToken = testRefreshToken
            )
        )

        // When
        val result = supabaseAuthService.verifyOtp(testPhoneNumber, testOtpCode)

        // Then
        assertThat(result).isInstanceOf(AuthResult.Success::class.java)
        val sessionData = (result as AuthResult.Success).data
        assertThat(sessionData.user.id).isEqualTo(testUserId)
    }

    @Test
    fun `isAuthenticated returns true when session exists`() = runTest {
        // Given
        val userSession = createMockUserSession()
        coEvery { auth.currentSessionOrNull() } returns userSession

        // When
        val isAuth = supabaseAuthService.isAuthenticated()

        // Then
        assertThat(isAuth).isTrue()
    }

    // ============== Helper Methods ==============

    private fun createMockUserSession(): UserSession {
        val userInfo = createMockUserInfo()
        return mockk<UserSession>().apply {
            every { accessToken } returns testAccessToken
            every { refreshToken } returns testRefreshToken
            every { expiresIn } returns 3600
            every { expiresAt } returns Instant.fromEpochSeconds(System.currentTimeMillis() / 1000 + 3600)
            every { user } returns userInfo
        }
    }

    private fun createMockUserInfo(): UserInfo {
        return mockk<UserInfo>().apply {
            every { id } returns testUserId
            every { phone } returns testPhoneNumber
            every { email } returns null
            every { createdAt } returns Instant.fromEpochSeconds(System.currentTimeMillis() / 1000)
            every { updatedAt } returns Instant.fromEpochSeconds(System.currentTimeMillis() / 1000)
        }
    }
}
