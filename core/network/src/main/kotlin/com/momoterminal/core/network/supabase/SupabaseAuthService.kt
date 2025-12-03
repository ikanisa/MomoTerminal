package com.momoterminal.core.network.supabase

import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service handling Supabase authentication operations.
 * Provides WhatsApp OTP authentication via custom Edge Functions.
 */
@Singleton
class SupabaseAuthService @Inject constructor(
    private val auth: Auth,
    private val edgeFunctionsApi: EdgeFunctionsApi
) {
    
    /**
     * Send WhatsApp OTP to the specified phone number using custom Edge Function.
     * Uses the "momo_terminal" template configured in Meta WhatsApp Business.
     *
     * @param phoneNumber Phone number in E.164 format (e.g., +250788767816)
     * @return AuthResult indicating success or failure
     */
    suspend fun sendWhatsAppOtp(phoneNumber: String): AuthResult<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Sending WhatsApp OTP to: $phoneNumber via Edge Function")
            
            val response = edgeFunctionsApi.sendWhatsAppOtp(
                SendOtpRequest(phoneNumber = phoneNumber)
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                Timber.d("WhatsApp OTP sent successfully")
                AuthResult.Success(Unit)
            } else {
                val errorMessage = response.body()?.error ?: "Failed to send OTP"
                Timber.e("Failed to send WhatsApp OTP: $errorMessage")
                AuthResult.Error(
                    message = errorMessage,
                    code = "OTP_SEND_FAILED"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to send WhatsApp OTP")
            AuthResult.Error(
                message = e.message ?: "Failed to send OTP",
                code = "OTP_SEND_FAILED"
            )
        }
    }
    
    /**
     * Verify OTP code using custom Edge Function.
     *
     * @param phoneNumber Phone number in E.164 format
     * @param otpCode 6-digit OTP code
     * @return AuthResult with SessionData on success
     */
    suspend fun verifyOtp(phoneNumber: String, otpCode: String): AuthResult<SessionData> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Verifying OTP for: $phoneNumber via Edge Function")
            
            val response = edgeFunctionsApi.verifyWhatsAppOtp(
                VerifyOtpRequest(
                    phoneNumber = phoneNumber,
                    otpCode = otpCode
                )
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                Timber.d("OTP verified successfully, user: ${body.userId}")
                
                // Edge Function returns null tokens because Supabase phone provider is not enabled
                // Create a simple session with the user ID
                val sessionData = SessionData(
                    accessToken = "whatsapp_otp_${body.userId}_${System.currentTimeMillis()}",
                    refreshToken = "refresh_${body.userId}_${System.currentTimeMillis()}",
                    expiresIn = 604800L, // 7 days
                    expiresAt = System.currentTimeMillis() / 1000 + 604800L,
                    user = SupabaseUser(
                        id = body.userId ?: "",
                        phone = phoneNumber,
                        email = null,
                        createdAt = null,
                        updatedAt = null
                    )
                )
                
                Timber.d("Session created for verified user: ${body.userId}")
                AuthResult.Success(sessionData)
            } else {
                val errorMessage = response.body()?.error ?: "Invalid OTP code"
                Timber.e("OTP verification failed: $errorMessage")
                AuthResult.Error(
                    message = errorMessage,
                    code = response.body()?.code ?: "OTP_VERIFICATION_FAILED"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to verify OTP")
            AuthResult.Error(
                message = e.message ?: "Invalid OTP code",
                code = "OTP_VERIFICATION_FAILED"
            )
        }
    }
    
    /**
     * Complete user profile with PIN and merchant info.
     *
     * @param userId User ID from OTP verification
     * @param pin 6-digit PIN
     * @param merchantName Business/merchant name
     * @param acceptedTerms Whether terms were accepted
     * @return AuthResult indicating success or failure
     */
    suspend fun completeProfile(
        userId: String,
        pin: String,
        merchantName: String,
        acceptedTerms: Boolean
    ): AuthResult<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Completing profile for user: $userId")
            
            val response = edgeFunctionsApi.completeUserProfile(
                CompleteProfileRequest(
                    userId = userId,
                    pin = pin,
                    merchantName = merchantName,
                    acceptedTerms = acceptedTerms
                )
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                Timber.d("Profile completed successfully")
                AuthResult.Success(Unit)
            } else {
                val errorMessage = response.body()?.error ?: "Failed to complete profile"
                Timber.e("Profile completion failed: $errorMessage")
                AuthResult.Error(
                    message = errorMessage,
                    code = response.body()?.code ?: "PROFILE_COMPLETION_FAILED"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to complete profile")
            AuthResult.Error(
                message = e.message ?: "Failed to complete profile",
                code = "PROFILE_COMPLETION_FAILED"
            )
        }
    }
    
    /**
     * Sign out the current user and clear session.
     */
    suspend fun signOut(): AuthResult<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Signing out user")
            auth.signOut()
            Timber.d("User signed out successfully")
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sign out")
            AuthResult.Error(
                message = e.message ?: "Sign out failed",
                code = "SIGN_OUT_FAILED"
            )
        }
    }
    
    /**
     * Get the current session if available.
     *
     * @return AuthResult with SessionData if session exists
     */
    suspend fun getCurrentSession(): AuthResult<SessionData?> = withContext(Dispatchers.IO) {
        try {
            val session = auth.currentSessionOrNull()
            if (session != null) {
                AuthResult.Success(session.toSessionData())
            } else {
                AuthResult.Success(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get current session")
            AuthResult.Error(
                message = e.message ?: "Failed to retrieve session",
                code = "SESSION_RETRIEVAL_FAILED"
            )
        }
    }
    
    /**
     * Refresh the current session.
     *
     * @return AuthResult with new SessionData on success
     */
    suspend fun refreshSession(): AuthResult<SessionData> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Refreshing session")
            auth.refreshCurrentSession()
            val session = auth.currentSessionOrNull() ?: throw Exception("No session after refresh")
            val sessionData = session.toSessionData()
            Timber.d("Session refreshed successfully")
            AuthResult.Success(sessionData)
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh session")
            AuthResult.Error(
                message = e.message ?: "Session refresh failed",
                code = "SESSION_REFRESH_FAILED"
            )
        }
    }
    
    /**
     * Get the current authenticated user.
     *
     * @return AuthResult with SupabaseUser if authenticated
     */
    suspend fun getCurrentUser(): AuthResult<SupabaseUser?> = withContext(Dispatchers.IO) {
        try {
            val userInfo = auth.currentUserOrNull()
            if (userInfo != null) {
                AuthResult.Success(userInfo.toSupabaseUser())
            } else {
                AuthResult.Success(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get current user")
            AuthResult.Error(
                message = e.message ?: "Failed to retrieve user",
                code = "USER_RETRIEVAL_FAILED"
            )
        }
    }
    
    /**
     * Check if user is currently authenticated.
     */
    suspend fun isAuthenticated(): Boolean = withContext(Dispatchers.IO) {
        try {
            auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            Timber.e(e, "Error checking authentication status")
            false
        }
    }
}

/**
 * Extension function to convert UserSession to SessionData.
 */
private fun UserSession.toSessionData(): SessionData {
    return SessionData(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = expiresIn,
        expiresAt = expiresAt.epochSeconds,
        user = user?.toSupabaseUser() ?: SupabaseUser(id = "unknown")
    )
}

/**
 * Extension function to convert UserInfo to SupabaseUser.
 */
private fun UserInfo.toSupabaseUser(): SupabaseUser {
    return SupabaseUser(
        id = id,
        phone = phone,
        email = email,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString()
    )
}
