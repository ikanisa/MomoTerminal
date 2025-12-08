package com.momoterminal.core.domain.usecase.settings.impl

import com.momoterminal.core.domain.model.settings.TransactionLimits
import com.momoterminal.core.domain.repository.SettingsRepository
import com.momoterminal.core.domain.usecase.settings.UpdateTransactionLimitsUseCase
import java.math.BigDecimal
import javax.inject.Inject

class UpdateTransactionLimitsUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : UpdateTransactionLimitsUseCase {
    
    override suspend fun invoke(
        userId: String,
        limits: TransactionLimits
    ): Result<Unit> {
        limits.minimumAmount.let {
            require(it >= BigDecimal.ZERO) { "Minimum amount must be positive" }
        }
        
        limits.dailyLimit?.let {
            require(it > BigDecimal.ZERO) { "Daily limit must be positive" }
        }
        
        limits.singleTransactionLimit?.let {
            require(it > BigDecimal.ZERO) { "Single transaction limit must be positive" }
            limits.minimumAmount.let { min ->
                require(it >= min) {
                    "Single transaction limit must be >= minimum amount"
                }
            }
        }
        
        limits.monthlyLimit?.let {
            require(it > BigDecimal.ZERO) { "Monthly limit must be positive" }
        }
        
        if (limits.dailyLimit != null && limits.monthlyLimit != null) {
            require(limits.monthlyLimit >= limits.dailyLimit) {
                "Monthly limit must be >= daily limit"
            }
        }
        
        return repository.updateTransactionLimits(userId, limits)
    }
}
