package com.momoterminal.auth

import com.momoterminal.BuildConfig
import com.momoterminal.api.AuthApiService
import com.momoterminal.api.AuthResponse
import com.momoterminal.api.LoginRequest
import com.momoterminal.api.OtpRequest
import com.momoterminal.api.OtpResponse
import com.momoterminal.api.RefreshRequest
import com.momoterminal.api.RegisterRequest
import com.momoterminal.api.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository handling all authentication-related operations.
 * Provides login, registration, token refresh, and logout functionality.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager
) {
    /**
     * Result wrapper for authentication operations.
     */
    sealed class AuthResult<out T> {
        data class Success<T>(val data: T) : AuthResult<T>()
        data class Error(val message: String, val code: Int? = null) : AuthResult<Nothing>()
        data object Loading : AuthResult<Nothing>()
    }

    /**
     * Login with phone number and PIN.
     */
    fun login(phoneNumber: String, pin: String): Flow<AuthResult<AuthResponse>> = flow {
        emit(AuthResult.Loading)
        
        try {
            // DEV ONLY: Hardcoded login bypass for development
            if (BuildConfig.DEBUG && phoneNumber == "0788767816" && pin == "123456") {
                Timber.d("Using hardcoded dev login")
                
                // Create mock auth response
                val mockAuthResponse = AuthResponse(
                    accessToken = "dev_access_token_${System.currentTimeMillis()}",
                    refreshToken = "dev_refresh_token_${System.currentTimeMillis()}",
                    expiresIn = 3600,
                    user = User(
                        id = "dev_user_001",
                        phoneNumber = phoneNumber,
                        merchantName = "Dev Merchant",
                        isVerified = true
                    )
                )
                
                // Save tokens
                tokenManager.saveTokens(
                    accessToken = mockAuthResponse.accessToken,
                    refreshToken = mockAuthResponse.refreshToken,
                    expiresInSeconds = mockAuthResponse.expiresIn
                )
                
                // Save user info
                tokenManager.saveUserInfo(
                    userId = mockAuthResponse.user.id,
                    phoneNumber = mockAuthResponse.user.phoneNumber
                )
                
                // Start session
                sessionManager.startSession()
                
                Timber.d("Dev login successful for user: ${mockAuthResponse.user.id}")
                emit(AuthResult.Success(mockAuthResponse))
                return@flow
            }
            
            val request = LoginRequest(
                phoneNumber = phoneNumber,
                pin = pin
            )
            
            val response = authApiService.login(request)
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                
                // Save tokens
                tokenManager.saveTokens(
                    accessToken = authResponse.accessToken,
                    refreshToken = authResponse.refreshToken,
                    expiresInSeconds = authResponse.expiresIn
                )
                
                // Save user info
                tokenManager.saveUserInfo(
                    userId = authResponse.user.id,
                    phoneNumber = authResponse.user.phoneNumber
                )
                
                // Start session
                sessionManager.startSession()
                
                Timber.d("Login successful for user: ${authResponse.user.id}")
                emit(AuthResult.Success(authResponse))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Login failed"
                Timber.e("Login failed: $errorMessage")
                emit(AuthResult.Error(errorMessage, response.code()))
            }
        } catch (e: Exception) {
            Timber.e(e, "Login error")
            emit(AuthResult.Error(e.message ?: "Network error occurred"))
        }
    }

    /**
     * Register a new user.
     */
    fun register(
        phoneNumber: String,
        pin: String,
        merchantName: String,
        acceptedTerms: Boolean
    ): Flow<AuthResult<AuthResponse>> = flow {
        emit(AuthResult.Loading)
        
        try {
            val request = RegisterRequest(
                phoneNumber = phoneNumber,
                pin = pin,
                merchantName = merchantName,
                acceptedTerms = acceptedTerms
            )
            
            val response = authApiService.register(request)
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                
                // Save tokens
                tokenManager.saveTokens(
                    accessToken = authResponse.accessToken,
                    refreshToken = authResponse.refreshToken,
                    expiresInSeconds = authResponse.expiresIn
                )
                
                // Save user info
                tokenManager.saveUserInfo(
                    userId = authResponse.user.id,
                    phoneNumber = authResponse.user.phoneNumber
                )
                
                // Start session
                sessionManager.startSession()
                
                Timber.d("Registration successful for user: ${authResponse.user.id}")
                emit(AuthResult.Success(authResponse))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Registration failed"
                Timber.e("Registration failed: $errorMessage")
                emit(AuthResult.Error(errorMessage, response.code()))
            }
        } catch (e: Exception) {
            Timber.e(e, "Registration error")
            emit(AuthResult.Error(e.message ?: "Network error occurred"))
        }
    }

    /**
     * Verify OTP code.
     */
    fun verifyOtp(phoneNumber: String, otpCode: String): Flow<AuthResult<OtpResponse>> = flow {
        emit(AuthResult.Loading)
        
        try {
            val request = OtpRequest(
                phoneNumber = phoneNumber,
                otpCode = otpCode
            )
            
            val response = authApiService.verifyOtp(request)
            
            if (response.isSuccessful && response.body() != null) {
                val otpResponse = response.body()!!
                Timber.d("OTP verification successful")
                emit(AuthResult.Success(otpResponse))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "OTP verification failed"
                Timber.e("OTP verification failed: $errorMessage")
                emit(AuthResult.Error(errorMessage, response.code()))
            }
        } catch (e: Exception) {
            Timber.e(e, "OTP verification error")
            emit(AuthResult.Error(e.message ?: "Network error occurred"))
        }
    }

    /**
     * Request OTP for phone number verification.
     */
    fun requestOtp(phoneNumber: String): Flow<AuthResult<OtpResponse>> = flow {
        emit(AuthResult.Loading)
        
        try {
            val request = OtpRequest(
                phoneNumber = phoneNumber,
                otpCode = "" // Empty for request
            )
            
            val response = authApiService.requestOtp(request)
            
            if (response.isSuccessful && response.body() != null) {
                val otpResponse = response.body()!!
                Timber.d("OTP request successful")
                emit(AuthResult.Success(otpResponse))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to send OTP"
                Timber.e("OTP request failed: $errorMessage")
                emit(AuthResult.Error(errorMessage, response.code()))
            }
        } catch (e: Exception) {
            Timber.e(e, "OTP request error")
            emit(AuthResult.Error(e.message ?: "Network error occurred"))
        }
    }

    /**
     * Refresh the access token using refresh token.
     */
    suspend fun refreshToken(): Boolean {
        val refreshToken = tokenManager.getRefreshToken() ?: return false
        
        return try {
            val request = RefreshRequest(refreshToken = refreshToken)
            val response = authApiService.refreshToken(request)
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.updateAccessToken(
                    newToken = authResponse.accessToken,
                    expiresInSeconds = authResponse.expiresIn
                )
                Timber.d("Token refresh successful")
                true
            } else {
                Timber.e("Token refresh failed: ${response.code()}")
                // If refresh fails, clear tokens and end session
                logout()
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Token refresh error")
            false
        }
    }

    /**
     * Logout the user and clear all tokens.
     */
    fun logout() {
        tokenManager.clearTokens()
        sessionManager.endSession()
        Timber.d("User logged out")
    }

    /**
     * Check if user is currently authenticated.
     */
    fun isAuthenticated(): Boolean {
        return tokenManager.hasValidToken()
    }

    /**
     * Validate current session.
     */
    fun validateSession(): Boolean {
        return sessionManager.validateSession()
    }

    /**
     * Get current access token for API calls.
     */
    fun getAccessToken(): String? {
        return tokenManager.getAccessToken()
    }

    /**
     * Check if token needs refresh.
     */
    fun needsTokenRefresh(): Boolean {
        return tokenManager.needsRefresh()
    }
}
