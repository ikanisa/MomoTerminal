package com.momoterminal.data.remote.api

import com.momoterminal.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for PWA database synchronization.
 * This service handles pushing payment SMS data to the PWA backend.
 */
interface MomoApiService {

    /**
     * Push a new transaction to the backend.
     */
    @POST("api/transactions")
    suspend fun syncTransaction(
        @Body transaction: SyncRequestDto
    ): Response<SyncResponseDto>

    /**
     * Get pending transactions from the server.
     */
    @GET("api/transactions/pending")
    suspend fun getPendingTransactions(
        @Query("merchant_code") merchantCode: String
    ): Response<List<TransactionDto>>

    /**
     * Confirm a transaction has been processed.
     */
    @POST("api/transactions/confirm")
    suspend fun confirmTransaction(
        @Query("transaction_id") transactionId: String
    ): Response<SyncResponseDto>

    /**
     * Register or update merchant information.
     */
    @POST("api/merchants/register")
    suspend fun registerMerchant(
        @Body merchantInfo: MerchantRegistrationDto
    ): Response<SyncResponseDto>
    
    // TODO: Uncomment when backend endpoints are implemented
    /*
    /**
     * Register a new device.
     */
    @POST("api/devices/register")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): RegisterDeviceResponse
    
    /**
     * Update device FCM token.
     */
    @PUT("api/devices/{device_id}/token")
    suspend fun updateDeviceToken(
        @Path("device_id") deviceId: String,
        @Body request: UpdateFcmTokenRequest
    ): Response<Unit>
    
    /**
     * Get merchant settings.
     */
    @GET("api/merchant-settings")
    suspend fun getMerchantSettings(): MerchantSettingsDto
    
    /**
     * Update merchant settings.
     */
    @PUT("api/merchant-settings")
    suspend fun updateMerchantSettings(
        @Body settings: MerchantSettingsDto
    ): MerchantSettingsDto
    
    /**
     * Batch upload analytics events.
     */
    @POST("api/analytics/events")
    suspend fun uploadAnalytics(
        @Body request: BatchAnalyticsRequest
    ): Response<Unit>
    
    /**
     * Batch upload error logs.
     */
    @POST("api/error-logs")
    suspend fun uploadErrorLogs(
        @Body request: BatchErrorLogsRequest
    ): Response<Unit>
    */
    
    /**
     * Health check endpoint.
     */
    @GET("api/health")
    suspend fun healthCheck(): Response<Unit>
}

/**
 * Merchant registration data.
 */
data class MerchantRegistrationDto(
    val merchantCode: String,
    val businessName: String?,
    val phoneNumber: String,
    val deviceId: String
)
