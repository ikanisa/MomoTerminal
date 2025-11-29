package com.momoterminal.auth

import app.cash.turbine.test
import com.momoterminal.api.AuthResponse
import com.momoterminal.api.User
import com.momoterminal.security.BiometricHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AuthViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private lateinit var authRepository: AuthRepository
    private lateinit var biometricHelper: BiometricHelper
    private lateinit var sessionManager: SessionManager

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        authRepository = mockk(relaxed = true)
        biometricHelper = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        
        every { authRepository.isAuthenticated() } returns false
        every { sessionManager.validateSession() } returns false
        every { biometricHelper.isBiometricAvailable() } returns false
        
        viewModel = AuthViewModel(authRepository, biometricHelper, sessionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty fields`() = runTest {
        // Then
        val state = viewModel.uiState.value
        assertEquals("", state.phoneNumber)
        assertEquals("", state.pin)
        assertFalse(state.isLoading)
        assertFalse(state.isAuthenticated)
        assertNull(state.error)
    }

    @Test
    fun `updatePhoneNumber updates state`() = runTest {
        // When
        viewModel.updatePhoneNumber("0201234567")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("0201234567", viewModel.uiState.value.phoneNumber)
    }

    @Test
    fun `updatePin accepts only digits up to 6 characters`() = runTest {
        // When - valid PIN
        viewModel.updatePin("123456")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("123456", viewModel.uiState.value.pin)

        // When - try to exceed 6 digits
        viewModel.updatePin("1234567")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should still be 6 digits
        assertEquals("123456", viewModel.uiState.value.pin)

        // When - try non-digits
        viewModel.updatePin("12345a")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should still be previous value
        assertEquals("123456", viewModel.uiState.value.pin)
    }

    @Test
    fun `login fails with empty phone number`() = runTest {
        // Given
        viewModel.updatePin("123456")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("Please enter your phone number", viewModel.uiState.value.error)
    }

    @Test
    fun `login fails with invalid PIN length`() = runTest {
        // Given
        viewModel.updatePhoneNumber("0201234567")
        viewModel.updatePin("123") // Only 3 digits
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("PIN must be 6 digits", viewModel.uiState.value.error)
    }

    @Test
    fun `login success navigates to home`() = runTest {
        // Given
        val authResponse = AuthResponse(
            accessToken = "token",
            refreshToken = "refresh",
            expiresIn = 3600,
            user = User(id = "123", phoneNumber = "0201234567")
        )
        coEvery { authRepository.login(any(), any()) } returns flowOf(
            AuthRepository.AuthResult.Loading,
            AuthRepository.AuthResult.Success(authResponse)
        )

        viewModel.updatePhoneNumber("0201234567")
        viewModel.updatePin("123456")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.events.test {
            val event = awaitItem()
            assertTrue(event is AuthViewModel.AuthEvent.NavigateToHome)
        }
        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertEquals(0, viewModel.uiState.value.pinAttempts)
    }

    @Test
    fun `login failure increments attempts and shows error`() = runTest {
        // Given
        coEvery { authRepository.login(any(), any()) } returns flowOf(
            AuthRepository.AuthResult.Loading,
            AuthRepository.AuthResult.Error("Invalid credentials")
        )

        viewModel.updatePhoneNumber("0201234567")
        viewModel.updatePin("123456")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.uiState.value.pinAttempts)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun `three failed login attempts locks out user`() = runTest {
        // Given
        coEvery { authRepository.login(any(), any()) } returns flowOf(
            AuthRepository.AuthResult.Loading,
            AuthRepository.AuthResult.Error("Invalid credentials")
        )

        viewModel.updatePhoneNumber("0201234567")
        viewModel.updatePin("123456")
        testDispatcher.scheduler.advanceUntilIdle()

        // When - fail 3 times
        repeat(3) {
            viewModel.login()
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // Then
        assertTrue(viewModel.uiState.value.isLockedOut)
        assertEquals(3, viewModel.uiState.value.pinAttempts)
    }

    @Test
    fun `logout clears state and navigates to login`() = runTest {
        // Given - set up authenticated state
        viewModel.updatePhoneNumber("0201234567")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { authRepository.logout() }
        viewModel.events.test {
            val event = awaitItem()
            assertTrue(event is AuthViewModel.AuthEvent.NavigateToLogin)
        }
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        // Given - set an error
        viewModel.login() // This will set an error (empty phone number)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `updateTermsAcceptance updates state`() = runTest {
        // When
        viewModel.updateTermsAcceptance(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.acceptedTerms)
    }

    @Test
    fun `nextRegistrationStep advances through steps`() = runTest {
        // Initial step
        assertEquals(
            AuthViewModel.RegistrationStep.PHONE_ENTRY,
            viewModel.uiState.value.registrationStep
        )

        // When
        viewModel.nextRegistrationStep()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(
            AuthViewModel.RegistrationStep.OTP_VERIFICATION,
            viewModel.uiState.value.registrationStep
        )
    }

    @Test
    fun `previousRegistrationStep goes back through steps`() = runTest {
        // Given - advance to OTP step
        viewModel.nextRegistrationStep()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            AuthViewModel.RegistrationStep.OTP_VERIFICATION,
            viewModel.uiState.value.registrationStep
        )

        // When
        viewModel.previousRegistrationStep()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(
            AuthViewModel.RegistrationStep.PHONE_ENTRY,
            viewModel.uiState.value.registrationStep
        )
    }

    @Test
    fun `biometric availability is checked on init`() = runTest {
        // Given
        every { biometricHelper.isBiometricAvailable() } returns true

        // When - create new instance
        val newViewModel = AuthViewModel(authRepository, biometricHelper, sessionManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(newViewModel.uiState.value.isBiometricAvailable)
    }

    @Test
    fun `triggerBiometricAuth emits event when available`() = runTest {
        // Given
        every { biometricHelper.isBiometricAvailable() } returns true
        val newViewModel = AuthViewModel(authRepository, biometricHelper, sessionManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        newViewModel.triggerBiometricAuth()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        newViewModel.events.test {
            val event = awaitItem()
            assertTrue(event is AuthViewModel.AuthEvent.ShowBiometricPrompt)
        }
    }

    @Test
    fun `onBiometricSuccess navigates to home when authenticated`() = runTest {
        // Given
        every { authRepository.isAuthenticated() } returns true

        // When
        viewModel.onBiometricSuccess()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { sessionManager.startSession() }
        assertTrue(viewModel.uiState.value.isAuthenticated)
    }
}
