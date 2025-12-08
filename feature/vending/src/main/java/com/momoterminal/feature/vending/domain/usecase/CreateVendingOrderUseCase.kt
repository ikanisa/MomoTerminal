package com.momoterminal.feature.vending.domain.usecase
import com.momoterminal.feature.vending.domain.repository.VendingRepository
import com.momoterminal.feature.wallet.domain.usecase.GetWalletBalanceUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CreateVendingOrderUseCase @Inject constructor(
    private val vendingRepository: VendingRepository,
    private val getWalletBalanceUseCase: GetWalletBalanceUseCase
) {
    suspend operator fun invoke(machineId: String, quantity: Int): Result<com.momoterminal.feature.vending.domain.model.VendingOrder> {
        if (quantity < 1 || quantity > 10) {
            return Result.failure(IllegalArgumentException("Quantity must be between 1 and 10 cups"))
        }
        
        val balance = getWalletBalanceUseCase().first()
        if (balance == null) return Result.failure(Exception("Unable to fetch wallet balance"))
        
        val machine = vendingRepository.getMachineById(machineId).getOrElse {
            return Result.failure(it)
        }
        
        val totalAmount = machine.pricePerServing * quantity
        
        if (balance.totalTokens < totalAmount) {
            return Result.failure(InsufficientBalanceException(balance.totalTokens, totalAmount))
        }
        
        return vendingRepository.createOrder(machineId, quantity)
    }
}

class InsufficientBalanceException(val currentBalance: Long, val requiredAmount: Long) : 
    Exception("Insufficient balance. Current: $currentBalance, Required: $requiredAmount")
