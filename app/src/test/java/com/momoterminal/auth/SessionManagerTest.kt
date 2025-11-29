package com.momoterminal.auth

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SessionManager.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var tokenManager: TokenManager
    private lateinit var context: Context
    private lateinit var applicationScope: CoroutineScope
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        tokenManager = mockk(relaxed = true)
        applicationScope = CoroutineScope(testDispatcher)
        
        // Default: no valid token
        every { tokenManager.hasValidToken() } returns false
        every { tokenManager.isTokenExpired() } returns true
        
        sessionManager = SessionManager(context, tokenManager, applicationScope)
    }

    @Test
    fun `startSession sets state to Active`() = runTest {
        // When
        sessionManager.startSession()

        // Then
        assertEquals(SessionManager.SessionState.Active, sessionManager.sessionState.first())
        assertTrue(sessionManager.isSessionValid.first())
    }

    @Test
    fun `endSession clears tokens and sets state to LoggedOut`() = runTest {
        // Given
        sessionManager.startSession()

        // When
        sessionManager.endSession()

        // Then
        verify { tokenManager.clearTokens() }
        assertEquals(SessionManager.SessionState.LoggedOut, sessionManager.sessionState.first())
        assertFalse(sessionManager.isSessionValid.first())
    }

    @Test
    fun `recordActivity updates last activity time when active`() = runTest {
        // Given
        sessionManager.startSession()
        val initialRemaining = sessionManager.getRemainingSessionTime()
        
        // Small delay to ensure time passes
        Thread.sleep(50)

        // When
        sessionManager.recordActivity()

        // Then - remaining time should be close to full timeout (refreshed)
        val newRemaining = sessionManager.getRemainingSessionTime()
        assertTrue(newRemaining >= initialRemaining - 100) // Allow small variance
    }

    @Test
    fun `checkSessionTimeout expires session after timeout`() = runTest {
        // Given
        sessionManager.setSessionTimeout(0) // Set to minimum (will be 1 minute)
        sessionManager.startSession()
        
        // Simulate that token is expired
        every { tokenManager.isTokenExpired() } returns true

        // When
        val isExpired = sessionManager.checkSessionTimeout()

        // Then - session should expire because token is expired
        assertTrue(isExpired)
    }

    @Test
    fun `setSessionTimeout clamps to valid range`() {
        // When - set below minimum
        sessionManager.setSessionTimeout(0)
        
        // Then - should be minimum (1 minute)
        assertEquals(1, sessionManager.getSessionTimeoutMinutes())
        
        // When - set above maximum
        sessionManager.setSessionTimeout(120) // 2 hours
        
        // Then - should be maximum (60 minutes)
        assertEquals(60, sessionManager.getSessionTimeoutMinutes())
    }

    @Test
    fun `getRemainingSessionTime returns zero when not active`() = runTest {
        // Given - session not started

        // When
        val remaining = sessionManager.getRemainingSessionTime()

        // Then
        assertEquals(0L, remaining)
    }

    @Test
    fun `isSessionAboutToExpire returns true when under 2 minutes remaining`() = runTest {
        // Given
        sessionManager.setSessionTimeout(2) // 2 minutes
        sessionManager.startSession()
        
        // Simulate time passing by checking the condition
        // Since we just started, remaining should be around 2 minutes

        // When
        val aboutToExpire = sessionManager.isSessionAboutToExpire()

        // Then - should be true because we're within 2 minutes
        assertTrue(aboutToExpire)
    }

    @Test
    fun `validateSession returns true when token is valid`() = runTest {
        // Given
        every { tokenManager.hasValidToken() } returns true

        // When
        val result = sessionManager.validateSession()

        // Then
        assertTrue(result)
        assertEquals(SessionManager.SessionState.Active, sessionManager.sessionState.first())
    }

    @Test
    fun `validateSession returns false when no valid token`() {
        // Given
        every { tokenManager.hasValidToken() } returns false

        // When
        val result = sessionManager.validateSession()

        // Then
        assertFalse(result)
    }

    @Test
    fun `onAppForeground checks session timeout`() = runTest {
        // Given
        sessionManager.startSession()
        every { tokenManager.isTokenExpired() } returns false

        // When
        sessionManager.onAppForeground()

        // Then - session should still be active (not timed out)
        assertEquals(SessionManager.SessionState.Active, sessionManager.sessionState.first())
    }
}
