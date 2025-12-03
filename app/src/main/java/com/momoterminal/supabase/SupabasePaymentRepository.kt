package com.momoterminal.supabase

import com.momoterminal.data.local.entity.TransactionEntity
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabasePaymentRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    companion object {
        private const val TRANSACTIONS_TABLE = "transactions"
        private const val MERCHANTS_TABLE = "merchants"
    }

    /**
     * Sync a local transaction to Supabase.
     */
    suspend fun syncTransaction(
        transaction: TransactionEntity,
        merchantId: String,
        deviceId: String?
    ): Result<String> {
        return try {
            val insert = TransactionInsert(
                client_transaction_id = transaction.clientTransactionId,
                merchant_id = merchantId,
                device_id = deviceId,
                amount = transaction.amountInPesewas ?: 0,
                currency = transaction.currency ?: "RWF",
                type = transaction.type ?: "received",
                status = transaction.status ?: "completed",
                provider = transaction.provider,
                provider_ref = transaction.transactionId,
                sender_phone = transaction.senderPhone,
                sender_name = transaction.senderName,
                sms_sender = transaction.sender,
                sms_body = transaction.body,
                sms_timestamp = transaction.timestamp?.let { 
                    java.time.Instant.ofEpochMilli(it).toString() 
                }
            )

            val result = postgrest.from(TRANSACTIONS_TABLE)
                .insert(insert) { select(Columns.list("id")) }
                .decodeSingle<IdResponse>()

            Timber.d("Transaction synced: ${result.id}")
            Result.success(result.id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync transaction")
            Result.failure(e)
        }
    }

    /**
     * Get or create merchant by phone.
     */
    suspend fun getOrCreateMerchant(
        phone: String,
        countryCode: String,
        currency: String
    ): Result<String> {
        return try {
            // Try to find existing merchant
            val existing = postgrest.from(MERCHANTS_TABLE)
                .select(Columns.list("id")) {
                    filter { eq("phone", phone) }
                    limit(1)
                }
                .decodeList<IdResponse>()

            if (existing.isNotEmpty()) {
                return Result.success(existing.first().id)
            }

            // Create new merchant
            val insert = MerchantInsert(
                phone = phone,
                country_code = countryCode,
                currency = currency
            )

            val result = postgrest.from(MERCHANTS_TABLE)
                .insert(insert) { select(Columns.list("id")) }
                .decodeSingle<IdResponse>()

            Timber.d("Merchant created: ${result.id}")
            Result.success(result.id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get/create merchant")
            Result.failure(e)
        }
    }

    /**
     * Get transactions for a merchant.
     */
    suspend fun getTransactions(merchantId: String, limit: Int = 50): List<TransactionResponse> {
        return try {
            postgrest.from(TRANSACTIONS_TABLE)
                .select(Columns.ALL) {
                    filter { eq("merchant_id", merchantId) }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get transactions")
            emptyList()
        }
    }
}

@Serializable
data class TransactionInsert(
    val client_transaction_id: String,
    val merchant_id: String,
    val device_id: String? = null,
    val amount: Long,
    val currency: String,
    val type: String,
    val status: String,
    val provider: String? = null,
    val provider_ref: String? = null,
    val sender_phone: String? = null,
    val sender_name: String? = null,
    val sms_sender: String? = null,
    val sms_body: String? = null,
    val sms_timestamp: String? = null
)

@Serializable
data class MerchantInsert(
    val phone: String,
    val country_code: String,
    val currency: String
)

@Serializable
data class IdResponse(val id: String)

@Serializable
data class TransactionResponse(
    val id: String,
    val amount: Long,
    val currency: String,
    val type: String,
    val status: String,
    val provider: String? = null,
    val sender_phone: String? = null,
    val sender_name: String? = null,
    val created_at: String
)
