package com.momoterminal.data.sync

import com.momoterminal.core.common.config.AppConfig
import com.momoterminal.data.local.MomoDatabase
import com.momoterminal.data.preferences.UserPreferences
import com.momoterminal.supabase.SupabasePaymentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to sync local transactions to Supabase.
 */
@Singleton
class TransactionSyncService @Inject constructor(
    private val database: MomoDatabase,
    private val supabaseRepo: SupabasePaymentRepository,
    private val appConfig: AppConfig,
    private val userPreferences: UserPreferences
) {
    private var merchantId: String? = null

    /**
     * Sync all pending transactions to Supabase.
     */
    suspend fun syncPendingTransactions(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val merchantPhone = appConfig.getMerchantPhone()
            if (merchantPhone.isBlank()) {
                return@withContext SyncResult.Error("Merchant not configured")
            }

            // Get or create merchant
            if (merchantId == null) {
                val result = supabaseRepo.getOrCreateMerchant(
                    phone = merchantPhone,
                    countryCode = appConfig.getCountryCode(),
                    currency = appConfig.getCurrency()
                )
                merchantId = result.getOrNull()
                if (merchantId == null) {
                    return@withContext SyncResult.Error("Failed to get merchant ID")
                }
            }

            // Get unsynced transactions
            val pending = database.transactionDao().getUnsyncedTransactions()
            if (pending.isEmpty()) {
                return@withContext SyncResult.Success(0)
            }

            val deviceId = userPreferences.getDeviceUuid()
            var synced = 0

            for (transaction in pending) {
                val result = supabaseRepo.syncTransaction(
                    transaction = transaction,
                    merchantId = merchantId!!,
                    deviceId = deviceId
                )

                if (result.isSuccess) {
                    // Mark as synced
                    database.transactionDao().markAsSynced(transaction.id)
                    synced++
                }
            }

            Timber.d("Synced $synced/${pending.size} transactions")
            SyncResult.Success(synced)
        } catch (e: Exception) {
            Timber.e(e, "Sync failed")
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class SyncResult {
    data class Success(val count: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
