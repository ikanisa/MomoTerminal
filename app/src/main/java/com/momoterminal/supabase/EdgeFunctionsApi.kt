package com.momoterminal.supabase

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API interface for Supabase Edge Functions
 */
interface EdgeFunctionsApi {
    
    @POST("send-whatsapp-otp")
    suspend fun sendWhatsAppOtp(
        @Body request: SendOtpRequest
    ): Response<SendOtpResponse>
    
    @POST("verify-whatsapp-otp")
    suspend fun verifyWhatsAppOtp(
        @Body request: VerifyOtpRequest
    ): Response<VerifyOtpResponse>
}

data class SendOtpRequest(
    val phoneNumber: String
)

data class SendOtpResponse(
    val success: Boolean,
    val message: String,
    val expiresAt: String? = null,
    val expiresInSeconds: Int? = null,
    val error: String? = null,
    val retryAfter: Int? = null
)

data class VerifyOtpRequest(
    val phoneNumber: String,
    val otpCode: String
)

data class VerifyOtpResponse(
    val success: Boolean,
    val message: String,
    val userId: String? = null,
    val isNewUser: Boolean? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val error: String? = null,
    val code: String? = null
)
