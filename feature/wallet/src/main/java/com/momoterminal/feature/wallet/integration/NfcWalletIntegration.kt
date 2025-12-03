package com.momoterminal.feature.wallet.integration

import com.momoterminal.feature.wallet.domain.model.Token
import com.momoterminal.feature.wallet.domain.model.TokenSourceType
import com.momoterminal.feature.wallet.domain.repository.WalletRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to integrate NFC payments with the wallet system.
 * Automatically credits wallet when NFC payment is received.
 */
@Singleton
class NfcWalletIntegration @Inject constructor(
    private val walletRepository: WalletRepository
) {
    /**
     * Process an NFC payment and credit the wallet.
     */
    suspend fun processNfcPayment(
        amount: Long,
        currency: String,
        reference: String
    ): Result<Token> {
        val token = Token(
            amount = amount,
            currency = currency,
            sourceReference = reference,
            sourceType = TokenSourceType.NFC_RECEIVED
        )
        
        return walletRepository.addToken(token)
    }
}
