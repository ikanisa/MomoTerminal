package com.momoterminal.core.network.api

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
     * Push a new payment transaction to the PWA database.
     * Called when a payment SMS is intercepted.
     */
    @POST("api/transactions")
    suspend fun syncTransaction(
        @Body transaction: PaymentTransaction
    ): Response<SyncResponse>

    /**
     * Get pending transactions that haven't been synced.
     */
    @GET("api/transactions/pending")
    suspend fun getPendingTransactions(
        @Query("merchant_code") merchantCode: String
    ): Response<List<PaymentTransaction>>

    /**
     * Confirm a transaction has been processed.
     */
    @POST("api/transactions/confirm")
    suspend fun confirmTransaction(
        @Query("transaction_id") transactionId: String
    ): Response<SyncResponse>

    /**
     * Register or update merchant information.
     */
    @POST("api/merchants/register")
    suspend fun registerMerchant(
        @Body merchantInfo: MerchantInfo
    ): Response<SyncResponse>
}

/**
 * Merchant registration data.
 */
data class MerchantInfo(
    val merchantCode: String,
    val businessName: String,
    val phoneNumber: String,
    val deviceId: String
)
