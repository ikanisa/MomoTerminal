package com.momoterminal.feature.vending.domain.usecase
import com.momoterminal.feature.vending.domain.repository.VendingRepository
import com.momoterminal.feature.wallet.domain.usecase.GetWalletBalanceUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CreateVendingOrderUseCase @Inject constructor(
    private val vendingRepository: VendingRepository,
    private val getWalletBalanceUseCase: GetWalletBalanceUseCase
) {
    suspend operator fun invoke(machineId: String, amount: Long): Result<com.momoterminal.feature.vending.domain.model.VendingOrder> {
        val balance = getWalletBalanceUseCase().first()
        if (balance == null) return Result.failure(Exception("Unable to fetch wallet balance"))
        if (balance.totalTokens < amount) return Result.failure(InsufficientBalanceException(balance.totalTokens, amount))
        return vendingRepository.createOrder(machineId, amount)
    }
}

class InsufficientBalanceException(val currentBalance: Long, val requiredAmount: Long) : 
    Exception("Insufficient balance. Current: $currentBalance, Required: $requiredAmount")
