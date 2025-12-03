package com.momoterminal.core.domain.usecase

import com.momoterminal.core.common.Result

class ProcessPaymentUseCase {
    suspend fun execute(amount: Double, recipient: String): Result<String> {
        // Placeholder - implement payment processing
        return Result.Error(NotImplementedError("Payment processing not implemented"))
    }
}
