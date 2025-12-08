package com.momoterminal.core.domain.usecase.settings.impl

import com.momoterminal.core.domain.model.settings.FeatureFlags
import com.momoterminal.core.domain.repository.SettingsRepository
import com.momoterminal.core.domain.usecase.settings.UpdateFeatureFlagsUseCase
import javax.inject.Inject

class UpdateFeatureFlagsUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : UpdateFeatureFlagsUseCase {
    
    override suspend fun invoke(
        userId: String,
        flags: FeatureFlags
    ): Result<Unit> {
        return repository.updateFeatureFlags(userId, flags)
    }
}
