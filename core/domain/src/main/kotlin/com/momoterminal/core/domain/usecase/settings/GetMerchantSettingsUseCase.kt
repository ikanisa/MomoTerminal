package com.momoterminal.core.domain.usecase.settings

import com.momoterminal.core.domain.model.settings.MerchantSettings

interface GetMerchantSettingsUseCase {
    suspend operator fun invoke(userId: String): Result<MerchantSettings>
}
