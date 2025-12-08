package com.momoterminal.core.domain.usecase.settings

import com.momoterminal.core.domain.model.settings.TransactionLimits

interface UpdateTransactionLimitsUseCase {
    suspend operator fun invoke(
        userId: String,
        limits: TransactionLimits
    ): Result<Unit>
}
