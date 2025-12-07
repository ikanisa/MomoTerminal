package com.momoterminal.supabase

import com.momoterminal.core.database.entity.SmsTransactionEntity
import com.momoterminal.core.database.entity.TransactionEntity
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
        private const val VENDOR_SMS_TRANSACTIONS_TABLE = "vendor_sms_transactions"
        private const val SMS_PARSING_VENDORS_TABLE = "sms_parsing_vendors"
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

    /**
     * Sync SMS transaction to Supabase.
     * Looks up vendor by MOMO number and inserts into vendor_sms_transactions table.
     */
    suspend fun syncSmsTransaction(transaction: SmsTransactionEntity): Result<String> {
        return try {
            // Extract phone number from transaction (sender or recipient based on type)
            val phoneNumber = extractPhoneNumber(transaction)
            
            // Find vendor by MOMO number
            val vendorId = if (phoneNumber != null) {
                findVendorByMomoNumber(phoneNumber)
            } else {
                null
            }
            
            // Prepare transaction data for sync
            val insert = VendorSmsTransactionInsert(
                vendor_id = vendorId,
                raw_message = transaction.rawMessage,
                sender = transaction.sender,
                amount_in_pesewas = (transaction.amount * 100).toLong(),
                currency = transaction.currency,
                transaction_type = transaction.type.name,
                balance_in_pesewas = transaction.balance?.let { (it * 100).toLong() },
                reference = transaction.reference,
                timestamp = java.time.Instant.ofEpochMilli(transaction.timestamp).toString(),
                parsed_by = transaction.parsedBy,
                ai_confidence = transaction.aiConfidence,
                payee_momo_number = phoneNumber
            )

            val result = postgrest.from(VENDOR_SMS_TRANSACTIONS_TABLE)
                .insert(insert) { select(Columns.list("id")) }
                .decodeSingle<IdResponse>()

            Timber.d("SMS transaction synced: ${result.id}")
            Result.success(result.id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync SMS transaction")
            Result.failure(e)
        }
    }

    /**
     * Find vendor by their registered MOMO number.
     */
    private suspend fun findVendorByMomoNumber(momoNumber: String): String? {
        return try {
            val result = postgrest.from(SMS_PARSING_VENDORS_TABLE)
                .select(Columns.list("vendor_id")) {
                    filter { eq("momo_number", momoNumber) }
                    limit(1)
                }
                .decodeList<VendorIdResponse>()
            
            result.firstOrNull()?.vendor_id
        } catch (e: Exception) {
            Timber.w(e, "Failed to find vendor by MOMO number: $momoNumber")
            null
        }
    }

    /**
     * Extract phone number from transaction based on type.
     */
    private fun extractPhoneNumber(transaction: SmsTransactionEntity): String? {
        // For received transactions, we might need to extract the sender's phone
        // from the raw message or other fields. For now, we'll return null
        // and let the sync work without vendor matching.
        // This can be enhanced based on actual data structure.
        return transaction.reference // Placeholder - adjust based on actual requirements
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
data class VendorSmsTransactionInsert(
    val vendor_id: String? = null,
    val raw_message: String,
    val sender: String,
    val amount_in_pesewas: Long,
    val currency: String,
    val transaction_type: String,
    val balance_in_pesewas: Long? = null,
    val reference: String? = null,
    val timestamp: String,
    val parsed_by: String,
    val ai_confidence: Float,
    val payee_momo_number: String? = null
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
data class VendorIdResponse(val vendor_id: String)

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
