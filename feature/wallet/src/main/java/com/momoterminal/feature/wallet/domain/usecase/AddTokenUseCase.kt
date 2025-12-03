package com.momoterminal.feature.wallet.domain.usecase

import com.momoterminal.feature.wallet.domain.model.Token
import com.momoterminal.feature.wallet.domain.repository.WalletRepository
import javax.inject.Inject

/**
 * Use case to add a token to the wallet.
 */
class AddTokenUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(token: Token): Result<Token> {
        return walletRepository.addToken(token)
    }
}
