package com.momoterminal.core.domain.usecase.settings.impl

import com.momoterminal.core.domain.model.settings.MerchantSettings
import com.momoterminal.core.domain.repository.SettingsRepository
import com.momoterminal.core.domain.usecase.settings.GetMerchantSettingsUseCase
import javax.inject.Inject

class GetMerchantSettingsUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : GetMerchantSettingsUseCase {
    
    override suspend fun invoke(userId: String): Result<MerchantSettings> {
        return repository.getMerchantSettings(userId)
    }
}
