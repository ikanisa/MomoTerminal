package com.momoterminal.feature.wallet.domain.usecase

import com.momoterminal.feature.wallet.domain.model.WalletBalance
import com.momoterminal.feature.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get the current wallet balance.
 */
class GetWalletBalanceUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) {
    operator fun invoke(): Flow<WalletBalance> {
        return walletRepository.getBalance()
    }
}
