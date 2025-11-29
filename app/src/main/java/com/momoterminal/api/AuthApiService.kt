package com.momoterminal.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API interface for authentication endpoints.
 */
interface AuthApiService {

    /**
     * Login with phone number and PIN.
     */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    /**
     * Register a new merchant account.
     */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    /**
     * Refresh the access token using refresh token.
     */
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<AuthResponse>

    /**
     * Verify OTP code for phone number verification.
     */
    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: OtpRequest): Response<OtpResponse>

    /**
     * Request OTP code to be sent to phone number.
     */
    @POST("auth/request-otp")
    suspend fun requestOtp(@Body request: OtpRequest): Response<OtpResponse>

    /**
     * Logout and invalidate tokens.
     */
    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}
