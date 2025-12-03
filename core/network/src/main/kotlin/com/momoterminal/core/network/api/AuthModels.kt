package com.momoterminal.core.network.api

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
 * Request body for OTP operations (legacy - kept for backwards compatibility).
 * Request body for OTP operations (legacy - kept for backward compatibility).
 */
data class OtpRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("otp_code")
    val otpCode: String
)

/**
 * Request body for sending OTP via WhatsApp.
 * Request body for sending WhatsApp OTP.
 */
data class SendOtpRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("channel")
    val channel: String = "whatsapp",
    @SerializedName("template_name")
    val templateName: String = "momo_terminal"
)

/**
 * Request body for verifying OTP.
 * Request body for verifying OTP code.
 */
data class VerifyOtpRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("otp_code")
    val otpCode: String
)

/**
 * Response for OTP send operations.
 */
data class OtpSendResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message_id")
    val messageId: String? = null,
    @SerializedName("expires_in_seconds")
    val expiresInSeconds: Int = 300,
    @SerializedName("retry_after_seconds")
    val retryAfterSeconds: Int? = null
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
    @SerializedName("user_id")
    val userId: String? = null,
    @SerializedName("expires_at")
    val expiresAt: Long? = null,
    @SerializedName("expires_in_seconds")
    val expiresInSeconds: Int = 300,
    @SerializedName("retry_after_seconds")
    val retryAfterSeconds: Int? = null
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
