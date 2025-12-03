package com.momoterminal.feature.wallet.integration

import com.momoterminal.core.database.entity.SmsTransactionEntity
import com.momoterminal.feature.wallet.domain.model.Token
import com.momoterminal.feature.wallet.domain.model.TokenSourceType
import com.momoterminal.feature.wallet.domain.repository.WalletRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to integrate SMS transactions with the wallet system.
 * Automatically credits wallet when SMS payment is received.
 */
@Singleton
class SmsWalletIntegration @Inject constructor(
    private val walletRepository: WalletRepository
) {
    /**
     * Process an SMS transaction and credit the wallet if it's a received payment.
     */
    suspend fun processSmsTransaction(
        smsTransaction: SmsTransactionEntity
    ): Result<Token?> {
        // Only credit wallet for received payments
        if (smsTransaction.type.name != "RECEIVED") {
            return Result.success(null)
        }
        
        val token = Token(
            amount = (smsTransaction.amount * 100).toLong(), // Convert to smallest unit
            currency = smsTransaction.currency,
            sourceReference = smsTransaction.reference,
            sourceType = TokenSourceType.SMS_RECEIVED
        )
        
        return walletRepository.addToken(token).map { it }
    }
}
