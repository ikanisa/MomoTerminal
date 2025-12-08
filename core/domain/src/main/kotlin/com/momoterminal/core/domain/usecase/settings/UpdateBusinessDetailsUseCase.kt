package com.momoterminal.core.domain.usecase.settings

import com.momoterminal.core.domain.model.settings.BusinessDetails

interface UpdateBusinessDetailsUseCase {
    suspend operator fun invoke(
        userId: String,
        businessDetails: BusinessDetails
    ): Result<Unit>
}
