package com.momoterminal.core.domain.usecase.settings

import com.momoterminal.core.domain.model.settings.MerchantStatus

interface UpdateMerchantProfileUseCase {
    suspend operator fun invoke(
        userId: String,
        businessName: String? = null,
        status: MerchantStatus? = null
    ): Result<Unit>
}
