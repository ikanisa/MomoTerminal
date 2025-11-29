package com.momoterminal.api

import com.google.gson.annotations.SerializedName

/**
 * Request body for login.
 */
data class LoginRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("pin")
    val pin: String
)

/**
 * Request body for registration.
 */
data class RegisterRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("pin")
    val pin: String,
    @SerializedName("merchant_name")
    val merchantName: String,
    @SerializedName("accepted_terms")
    val acceptedTerms: Boolean
)

/**
 * Request body for token refresh.
 */
data class RefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

/**
 * Request body for OTP operations.
 */
data class OtpRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("otp_code")
    val otpCode: String
)

/**
 * Response for authentication operations (login, register, refresh).
 */
data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
    @SerializedName("token_type")
    val tokenType: String = "Bearer",
    @SerializedName("user")
    val user: User
)

/**
 * Response for OTP operations.
 */
data class OtpResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("expires_at")
    val expiresAt: Long? = null
)

/**
 * User information returned from authentication.
 */
data class User(
    @SerializedName("id")
    val id: String,
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("merchant_name")
    val merchantName: String? = null,
    @SerializedName("created_at")
    val createdAt: Long? = null,
    @SerializedName("is_verified")
    val isVerified: Boolean = false
)
