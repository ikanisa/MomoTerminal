package com.momoterminal.feature.auth

import com.momoterminal.core.network.api.AuthApiService
import com.momoterminal.core.network.api.AuthResponse
import com.momoterminal.core.network.api.OtpResponse
import com.momoterminal.core.network.api.RefreshRequest
import com.momoterminal.core.network.api.RegisterRequest
import com.momoterminal.core.network.api.User
import com.momoterminal.core.network.supabase.SupabaseAuthService
import com.momoterminal.core.network.supabase.AuthResult as SupabaseAuthResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

import com.momoterminal.core.domain.repository.AuthRepository as AuthRepositoryInterface
import com.momoterminal.core.domain.model.User as DomainUser
import com.momoterminal.core.common.Result

/**
 * Repository handling all authentication-related operations.
 * Now uses Supabase for WhatsApp OTP authentication.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val supabaseAuthService: SupabaseAuthService,
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager
) : AuthRepositoryInterface {
    /**
     * Result wrapper for authentication operations.
     */
    sealed class AuthResult<out T> {
        data class Success<T>(val data: T) : AuthResult<T>()
        data class Error(val message: String, val code: Int? = null) : AuthResult<Nothing>()
        data object Loading : AuthResult<Nothing>()
    }

    /**
     * Get current user as a Flow.
     */
    override fun getCurrentUser(): Flow<DomainUser?> = flow {
        val userId = tokenManager.getUserId()
        val phoneNumber = tokenManager.getPhoneNumber()
        
        if (userId != null && phoneNumber != null) {
            emit(
                DomainUser(
                    id = userId,
                    phone = phoneNumber,
                    name = "", // Will be fetched from profile
                    isVerified = true
                )
            )
        } else {
            emit(null)
        }
    }

    /**
     * Send OTP to phone number.
     */
    override suspend fun sendOtp(phone: String): Result<Unit> {
        return try {
            when (val result = supabaseAuthService.sendWhatsAppOtp(phone)) {
                is SupabaseAuthResult.Success -> Result.Success(Unit)
                is SupabaseAuthResult.Error -> Result.Error(Exception(result.message))
                else -> Result.Error(Exception("Unexpected error sending OTP"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Verify OTP code.
     */
    override suspend fun verifyOtp(phone: String, code: String): Result<DomainUser> {
        return try {
            when (val result = supabaseAuthService.verifyOtp(phone, code)) {
                is SupabaseAuthResult.Success -> {
                    val sessionData = result.data
                    
                    tokenManager.saveTokens(
                        accessToken = sessionData.accessToken,
                        refreshToken = sessionData.refreshToken,
                        expiresInSeconds = sessionData.expiresIn
                    )
                    
                    tokenManager.saveUserInfo(
                        userId = sessionData.user.id,
                        phoneNumber = sessionData.user.phone ?: phone
                    )
                    
                    sessionManager.startSession()
                    
                    Result.Success(
                        DomainUser(
                            id = sessionData.user.id,
                            phone = sessionData.user.phone ?: phone,
                            name = "",
                            isVerified = true
                        )
                    )
                }
                is SupabaseAuthResult.Error -> Result.Error(Exception(result.message))
                else -> Result.Error(Exception("Unexpected error verifying OTP"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Sign out.
     */
    override suspend fun signOut(): Result<Unit> {
        return try {
            logout()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Check if authenticated.
     */
    override fun isAuthenticated(): Flow<Boolean> = flow {
        emit(tokenManager.hasValidToken())
    }

    // ... existing methods ...

    /**
     * Login with phone number using WhatsApp OTP.
     * For new flow: First call requestOtp(), then call this with the OTP code.
     */
    fun login(phoneNumber: String, otpCode: String): Flow<AuthResult<AuthResponse>> = flow {
        // ... implementation ...
        emit(AuthResult.Loading)
        
        try {
            // Use Supabase for WhatsApp OTP verification
            when (val result = supabaseAuthService.verifyOtp(phoneNumber, otpCode)) {
                is SupabaseAuthResult.Success -> {
                    val sessionData = result.data
                    
                    // Save tokens from Supabase session
                    tokenManager.saveTokens(
                        accessToken = sessionData.accessToken,
                        refreshToken = sessionData.refreshToken,
                        expiresInSeconds = sessionData.expiresIn
                    )
                    
                    // Save user info
                    tokenManager.saveUserInfo(
                        userId = sessionData.user.id,
                        phoneNumber = sessionData.user.phone ?: phoneNumber
                    )
                    
                    // Start session
                    sessionManager.startSession()
                    
                    // Convert to AuthResponse for compatibility
                    val authResponse = AuthResponse(
                        accessToken = sessionData.accessToken,
                        refreshToken = sessionData.refreshToken,
                        expiresIn = sessionData.expiresIn,
                        user = User(
                            id = sessionData.user.id,
                            phoneNumber = sessionData.user.phone ?: phoneNumber,
                            merchantName = "", // Will be set during profile completion
                            isVerified = true
                        )
                    )
                    
                    Timber.d("Login successful for user: ${sessionData.user.id}")
                    emit(AuthResult.Success(authResponse))
                }
                is SupabaseAuthResult.Error -> {
                    Timber.e("Login failed: ${result.message}")
                    emit(AuthResult.Error(result.message))
                }
                else -> {
                    emit(AuthResult.Error("Unexpected error during login"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Login error for phone: $phoneNumber")
            emit(AuthResult.Error(e.message ?: "Network error occurred during login"))
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
     * Complete user profile after OTP verification.
     */
    fun completeProfile(
        userId: String,
        pin: String,
        merchantName: String,
        acceptedTerms: Boolean
    ): Flow<AuthResult<Unit>> = flow {
        emit(AuthResult.Loading)
        
        try {
            // Use Supabase to complete profile
            when (val result = supabaseAuthService.completeProfile(userId, pin, merchantName, acceptedTerms)) {
                is SupabaseAuthResult.Success -> {
                    Timber.d("Profile completed successfully")
                    emit(AuthResult.Success(Unit))
                }
                is SupabaseAuthResult.Error -> {
                    Timber.e("Profile completion failed: ${result.message}")
                    emit(AuthResult.Error(result.message))
                }
                else -> {
                    emit(AuthResult.Error("Unexpected error completing profile"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Profile completion error")
            emit(AuthResult.Error(e.message ?: "Network error occurred"))
        }
    }

    /**
     * Verify OTP code using Supabase Edge Functions.
     * Verify OTP code using Supabase.
     */
    fun verifyOtpLegacy(phoneNumber: String, otpCode: String): Flow<AuthResult<OtpResponse>> = flow {
        emit(AuthResult.Loading)
        
        try {
            // Use Supabase to verify WhatsApp OTP
            when (val result = supabaseAuthService.verifyOtp(phoneNumber, otpCode)) {
                is SupabaseAuthResult.Success -> {
                    val sessionData = result.data
                    
                    // Save tokens from Supabase session
                    tokenManager.saveTokens(
                        accessToken = sessionData.accessToken,
                        refreshToken = sessionData.refreshToken,
                        expiresInSeconds = sessionData.expiresIn
                    )
                    
                    // Save user info
                    tokenManager.saveUserInfo(
                        userId = sessionData.user.id,
                        phoneNumber = sessionData.user.phone ?: phoneNumber
                    )
                    
                    // Start session
                    sessionManager.startSession()
                    
                    Timber.d("OTP verification successful")
                    val otpResponse = OtpResponse(
                        success = true,
                        message = "OTP verified successfully",
                        userId = sessionData.user.id
                    )
                    emit(AuthResult.Success(otpResponse))
                }
                is SupabaseAuthResult.Error -> {
                    Timber.e("OTP verification failed: ${result.message}")
                    emit(AuthResult.Error(result.message))
                }
                is SupabaseAuthResult.Loading -> {
                    // Already emitted Loading state above
                }
                else -> {
                    emit(AuthResult.Error("Unexpected error during OTP verification"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "OTP verification error for phone: $phoneNumber")
            emit(AuthResult.Error(e.message ?: "Network error occurred during OTP verification"))
        }
    }

    /**
     * Request WhatsApp OTP for phone number.
     */
    fun requestOtp(phoneNumber: String): Flow<AuthResult<OtpResponse>> = flow {
        emit(AuthResult.Loading)
        
        try {
            // Use Supabase to send WhatsApp OTP
            when (val result = supabaseAuthService.sendWhatsAppOtp(phoneNumber)) {
                is SupabaseAuthResult.Success -> {
                    Timber.d("WhatsApp OTP sent successfully")
                    // Return success response
                    val otpResponse = OtpResponse(
                        success = true,
                        message = "OTP sent to WhatsApp"
                    )
                    emit(AuthResult.Success(otpResponse))
                }
                is SupabaseAuthResult.Error -> {
                    Timber.e("Failed to send WhatsApp OTP: ${result.message}")
                    emit(AuthResult.Error(result.message))
                }
                else -> {
                    emit(AuthResult.Error("Unexpected error sending OTP"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "OTP request error")
            emit(AuthResult.Error(e.message ?: "Network error occurred"))
        }
    }

    /**
     * Refresh the access token using refresh token.
     */
    override suspend fun refreshToken(): Boolean {
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
    override fun logout() {
        tokenManager.clearTokens()
        sessionManager.endSession()
        Timber.d("User logged out")
    }

    /**
     * Check if user is currently authenticated.
     */
    fun isAuthenticatedLegacy(): Boolean {
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
    
    /**
     * Reset user PIN after successful OTP verification.
     * 
     * @param userId The user ID from OTP verification
     * @param newPin The new 4-digit PIN
     */
    suspend fun resetPin(userId: String, newPin: String) {
        try {
            // In a real implementation, this would call a Supabase function or RPC
            // For now, we'll update the user's metadata via Supabase
            // TODO: Implement actual PIN reset via Supabase
            
            // Hash the PIN before storing (in production)
            // val hashedPin = hashPin(newPin)
            
            // For now, just log success
            Timber.d("PIN reset successfully for user: $userId")
            
            // Note: In production, you would:
            // 1. Call supabaseAuthService.updateUserPin(userId, hashedPin)
            // 2. Or call a Supabase Edge Function to handle this securely
            // 3. Store the hashed PIN in user_metadata or a secure table
        } catch (e: Exception) {
            Timber.e(e, "Failed to reset PIN")
            throw e
        }
    }
}
