package com.momoterminal.core.domain.usecase

import com.momoterminal.core.data.repository.WalletRepository
import com.momoterminal.core.domain.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTokenBalanceUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) {
    fun observeWallet(userId: String): Flow<TokenWallet?> = walletRepository.observeWallet(userId)
    
    suspend fun getBalance(userId: String): Long = walletRepository.getBalance(userId)
}

class GetTokenHistoryUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) {
    fun observeTransactions(walletId: String): Flow<List<TokenTransaction>> = 
        walletRepository.observeTransactions(walletId)
}

class ApplyTokenTransactionUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(
        walletId: String,
        amount: Long,
        type: TokenTransactionType,
        reference: String? = null,
        referenceType: ReferenceType? = null,
        description: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): Result<TokenWallet> {
        return walletRepository.applyTransaction(
            walletId = walletId,
            amount = amount,
            type = type,
            reference = reference,
            referenceType = referenceType,
            description = description,
            metadata = metadata
        )
    }
}

class CreditFromSmsUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(
        userId: String,
        smsTransactionId: String,
        momoAmount: Long,
        currency: String,
        conversionRate: Double = 0.01 // Default: 1 token = 100 currency units
    ): Result<TokenWallet> {
        val wallet = walletRepository.getOrCreateWallet(userId)
        val tokenAmount = (momoAmount * conversionRate).toLong()
        
        return walletRepository.applyTransaction(
            walletId = wallet.id,
            amount = tokenAmount,
            type = TokenTransactionType.SMS_CREDIT,
            reference = smsTransactionId,
            referenceType = ReferenceType.SMS_TRANSACTION,
            description = "Credit from MoMo receive",
            metadata = mapOf(
                "originalAmount" to momoAmount.toString(),
                "originalCurrency" to currency,
                "conversionRate" to conversionRate.toString()
            )
        )
    }
}
