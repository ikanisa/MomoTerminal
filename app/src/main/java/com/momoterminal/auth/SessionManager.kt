package com.momoterminal.auth

import android.content.Context
import com.momoterminal.core.common.auth.TokenManager
import com.momoterminal.core.common.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user session state including timeout and activity tracking.
 * Handles automatic session expiration after inactivity.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    /**
     * Session state representing the current user's session status.
     */
    sealed class SessionState {
        data object Active : SessionState()
        data object Inactive : SessionState()
        data object Expired : SessionState()
        data object LoggedOut : SessionState()
    }

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Inactive)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _isSessionValid = MutableStateFlow(false)
    val isSessionValid: StateFlow<Boolean> = _isSessionValid.asStateFlow()

    private var lastActivityTime: Long = 0L
    private var sessionTimeoutJob: Job? = null
    
    // Use SupervisorJob to prevent child failures from cancelling the scope
    // This scope is tied to the singleton lifecycle
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        // Default session timeout: 15 minutes
        private const val DEFAULT_SESSION_TIMEOUT_MS = 15 * 60 * 1000L
        
        // Minimum timeout: 1 minute
        private const val MIN_SESSION_TIMEOUT_MS = 60 * 1000L
        
        // Maximum timeout: 1 hour
        private const val MAX_SESSION_TIMEOUT_MS = 60 * 60 * 1000L
        
        // Check interval for session timeout
        private const val SESSION_CHECK_INTERVAL_MS = 30 * 1000L
    }

    private var sessionTimeoutMs: Long = DEFAULT_SESSION_TIMEOUT_MS

    init {
        // Check if there's an existing valid session
        checkExistingSession()
    }

    private fun checkExistingSession() {
        if (tokenManager.hasValidToken()) {
            _sessionState.value = SessionState.Active
            _isSessionValid.value = true
            lastActivityTime = System.currentTimeMillis()
            startSessionMonitor()
        } else {
            _sessionState.value = SessionState.Inactive
            _isSessionValid.value = false
        }
    }

    /**
     * Start a new session after successful login.
     */
    fun startSession() {
        lastActivityTime = System.currentTimeMillis()
        _sessionState.value = SessionState.Active
        _isSessionValid.value = true
        startSessionMonitor()
    }

    /**
     * Record user activity to extend the session.
     */
    fun recordActivity() {
        if (_sessionState.value == SessionState.Active) {
            lastActivityTime = System.currentTimeMillis()
        }
    }

    /**
     * End the current session (logout).
     */
    fun endSession() {
        sessionTimeoutJob?.cancel()
        tokenManager.clearTokens()
        _sessionState.value = SessionState.LoggedOut
        _isSessionValid.value = false
        lastActivityTime = 0L
    }

    /**
     * Set the session timeout duration in minutes.
     */
    fun setSessionTimeout(minutes: Int) {
        val timeoutMs = minutes * 60 * 1000L
        sessionTimeoutMs = timeoutMs.coerceIn(MIN_SESSION_TIMEOUT_MS, MAX_SESSION_TIMEOUT_MS)
    }

    /**
     * Get the current session timeout in minutes.
     */
    fun getSessionTimeoutMinutes(): Int {
        return (sessionTimeoutMs / 60000).toInt()
    }

    /**
     * Handle app going to background.
     */
    fun onAppBackground() {
        // Session continues but timer keeps running
    }

    /**
     * Handle app coming to foreground.
     */
    fun onAppForeground() {
        if (_sessionState.value == SessionState.Active) {
            checkSessionTimeout()
        }
    }

    /**
     * Check if the session has timed out due to inactivity.
     */
    fun checkSessionTimeout(): Boolean {
        if (_sessionState.value != SessionState.Active) {
            return true
        }

        val currentTime = System.currentTimeMillis()
        val timeSinceLastActivity = currentTime - lastActivityTime

        if (timeSinceLastActivity > sessionTimeoutMs) {
            expireSession()
            return true
        }

        // Also check if the token has expired
        if (tokenManager.isTokenExpired()) {
            expireSession()
            return true
        }

        return false
    }

    /**
     * Get the remaining session time in milliseconds.
     */
    fun getRemainingSessionTime(): Long {
        if (_sessionState.value != SessionState.Active) {
            return 0L
        }

        val currentTime = System.currentTimeMillis()
        val timeSinceLastActivity = currentTime - lastActivityTime
        val remaining = sessionTimeoutMs - timeSinceLastActivity

        return maxOf(0L, remaining)
    }

    /**
     * Check if the session is about to expire (within 2 minutes).
     */
    fun isSessionAboutToExpire(): Boolean {
        val remaining = getRemainingSessionTime()
        return remaining in 1..(2 * 60 * 1000)
    }

    private fun startSessionMonitor() {
        sessionTimeoutJob?.cancel()
        sessionTimeoutJob = applicationScope.launch {
            while (_sessionState.value == SessionState.Active) {
                delay(SESSION_CHECK_INTERVAL_MS)
                checkSessionTimeout()
            }
        }
    }

    private fun expireSession() {
        sessionTimeoutJob?.cancel()
        _sessionState.value = SessionState.Expired
        _isSessionValid.value = false
    }

    /**
     * Validate and restore session if possible.
     * Returns true if session is valid or was restored.
     */
    fun validateSession(): Boolean {
        // If already active, check if still valid
        if (_sessionState.value == SessionState.Active) {
            return !checkSessionTimeout()
        }

        // If we have a valid token, restore the session
        if (tokenManager.hasValidToken()) {
            startSession()
            return true
        }

        return false
    }

    /**
     * Get the current user ID from the token.
     */
    val currentUserId: String?
        get() = tokenManager.getUserId()
}
