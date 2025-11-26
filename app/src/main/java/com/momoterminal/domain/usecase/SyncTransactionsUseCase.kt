package com.momoterminal.domain.usecase

import com.momoterminal.domain.repository.TransactionRepository
import com.momoterminal.util.Result
import javax.inject.Inject

/**
 * Use case for syncing pending transactions to the server.
 */
class SyncTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    
    /**
     * Sync all pending transactions.
     * 
     * @return Result containing the number of successfully synced transactions
     */
    suspend operator fun invoke(): Result<SyncResult> {
        return when (val result = transactionRepository.syncPendingTransactions()) {
            is Result.Success -> {
                Result.Success(
                    SyncResult(
                        syncedCount = result.data,
                        success = true
                    )
                )
            }
            is Result.Error -> {
                Result.Error(result.exception)
            }
            is Result.Loading -> Result.Loading
        }
    }
}

/**
 * Data class containing sync result information.
 */
data class SyncResult(
    val syncedCount: Int,
    val success: Boolean,
    val errorMessage: String? = null
)
