package com.momoterminal.supabase

import android.os.Build
import com.momoterminal.ai.AiParsedTransaction
import com.momoterminal.data.local.entity.TransactionEntity
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing payment records in Supabase.
 * Provides methods to insert, query, and sync payment transactions.
 */
@Singleton
class SupabasePaymentRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    
    companion object {
        private const val TAG = "SupabasePaymentRepo"
        private const val PAYMENTS_TABLE = "payments"
    }
    
    /**
     * Insert a payment record into Supabase.
     * 
     * @param payment The payment data to insert
     * @return PaymentResult indicating success or failure
     */
    suspend fun insertPayment(payment: SupabasePaymentInsert): PaymentResult {
        return try {
            val result = postgrest.from(PAYMENTS_TABLE)
                .insert(payment) {
                    select(Columns.ALL)
                }
                .decodeSingle<SupabasePayment>()
            
            Timber.d("Payment inserted successfully: ${result.id}")
            PaymentResult.Success(result)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to insert payment into Supabase")
            PaymentResult.Error("Failed to insert payment: ${e.message}", e)
        }
    }
    
    /**
     * Sync a local transaction to Supabase payments table.
     * 
     * @param transaction The local transaction entity
     * @param parsedData The parsed transaction data from AI or regex
     * @param deviceId The device identifier
     * @param merchantCode The merchant code associated with this device
     * @return PaymentResult indicating success or failure
     */
    suspend fun syncTransaction(
        transaction: TransactionEntity,
        parsedData: AiParsedTransaction?,
        deviceId: String?,
        merchantCode: String?
    ): PaymentResult {
        val payment = SupabasePaymentInsert(
            amountInPesewas = parsedData?.amountInPesewas ?: transaction.amountInPesewas ?: 0,
            currency = parsedData?.currency ?: transaction.currency ?: "GHS",
            senderPhone = parsedData?.senderPhone,
            recipientPhone = parsedData?.recipientPhone,
            transactionId = parsedData?.transactionId ?: transaction.transactionId,
            transactionType = parsedData?.transactionType ?: "UNKNOWN",
            provider = parsedData?.provider ?: "UNKNOWN",
            balanceInPesewas = parsedData?.balanceInPesewas,
            rawMessage = transaction.body,
            deviceId = deviceId ?: Build.MODEL,
            merchantCode = merchantCode ?: transaction.merchantCode,
            parsedBy = parsedData?.parsedBy ?: "regex",
            localId = transaction.id
        )
        
        return insertPayment(payment)
    }
    
    /**
     * Check if a transaction has already been synced by local ID.
     * 
     * @param localId The local transaction ID
     * @return true if transaction exists in Supabase, false otherwise
     */
    suspend fun isTransactionSynced(localId: Long): Boolean {
        return try {
            val result = postgrest.from(PAYMENTS_TABLE)
                .select(Columns.list("id")) {
                    filter {
                        eq("local_id", localId)
                    }
                    limit(1)
                }
                .decodeList<Map<String, String>>()
            
            result.isNotEmpty()
        } catch (e: Exception) {
            Timber.e(e, "Failed to check if transaction is synced")
            false
        }
    }
    
    /**
     * Get recent payments from Supabase.
     * 
     * @param limit Maximum number of payments to retrieve
     * @param merchantCode Optional filter by merchant code
     * @return List of payments or empty list on error
     */
    suspend fun getRecentPayments(limit: Int = 50, merchantCode: String? = null): List<SupabasePayment> {
        return try {
            val result = postgrest.from(PAYMENTS_TABLE)
                .select(Columns.ALL) {
                    if (merchantCode != null) {
                        filter {
                            eq("merchant_code", merchantCode)
                        }
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<SupabasePayment>()
            
            Timber.d("Retrieved ${result.size} recent payments")
            result
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to get recent payments from Supabase")
            emptyList()
        }
    }
}
