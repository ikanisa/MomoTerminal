package com.momoterminal.data.remote.api

import com.momoterminal.data.remote.dto.NfcTagDto
import com.momoterminal.data.remote.dto.SmsTransactionDto
import com.momoterminal.data.remote.dto.TokenTransactionDto
import com.momoterminal.data.remote.dto.WalletDto
import retrofit2.Response
import retrofit2.http.*

/**
 * REST API for wallet and transaction sync with backend.
 */
interface WalletSyncApi {
    
    // Wallet endpoints
    @GET("wallets/{userId}")
    suspend fun getWallet(@Path("userId") userId: String): Response<WalletDto>
    
    @POST("wallets")
    suspend fun createWallet(@Body wallet: WalletDto): Response<WalletDto>
    
    @PUT("wallets/{id}")
    suspend fun updateWallet(@Path("id") id: String, @Body wallet: WalletDto): Response<WalletDto>
    
    // Token transactions
    @GET("wallets/{walletId}/transactions")
    suspend fun getTransactions(
        @Path("walletId") walletId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<TokenTransactionDto>>
    
    @POST("wallets/{walletId}/transactions")
    suspend fun syncTransactions(
        @Path("walletId") walletId: String,
        @Body transactions: List<TokenTransactionDto>
    ): Response<SyncResponse>
    
    // SMS transactions
    @POST("sms-transactions")
    suspend fun syncSmsTransactions(@Body transactions: List<SmsTransactionDto>): Response<SyncResponse>
    
    // NFC tags
    @GET("nfc-tags")
    suspend fun getNfcTags(): Response<List<NfcTagDto>>
    
    @POST("nfc-tags")
    suspend fun syncNfcTags(@Body tags: List<NfcTagDto>): Response<SyncResponse>
}

data class SyncResponse(
    val success: Boolean,
    val syncedCount: Int,
    val failedIds: List<String> = emptyList(),
    val message: String? = null
)
