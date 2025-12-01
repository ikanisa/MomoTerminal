package com.momoterminal.supabase

import kotlinx.serialization.Serializable

/**
 * Data models for Supabase operations.
 */

/**
 * Supabase user profile data.
 */
@Serializable
data class SupabaseUser(
    val id: String,
    val phone: String? = null,
    val email: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * WhatsApp OTP request.
 */
data class OtpRequest(
    val phoneNumber: String,
    val channel: String = "whatsapp" // whatsapp, sms, or email
)

/**
 * OTP verification request.
 */
data class OtpVerification(
    val phoneNumber: String,
    val token: String // The OTP code
)

/**
 * Session data from Supabase.
 */
data class SessionData(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val expiresAt: Long,
    val user: SupabaseUser
)

/**
 * Authentication result wrapper.
 */
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val code: String? = null) : AuthResult<Nothing>()
    data object Loading : AuthResult<Nothing>()
}

/**
 * Complete user profile request.
 */
data class CompleteProfileRequest(
    val userId: String,
    val pin: String,
    val merchantName: String,
    val acceptedTerms: Boolean
)

/**
 * Complete user profile response.
 */
data class CompleteProfileResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val code: String? = null
)
