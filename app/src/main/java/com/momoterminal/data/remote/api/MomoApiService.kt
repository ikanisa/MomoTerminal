package com.momoterminal.data.remote.api

import com.momoterminal.data.remote.dto.SyncRequestDto
import com.momoterminal.data.remote.dto.SyncResponseDto
import com.momoterminal.data.remote.dto.TransactionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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
