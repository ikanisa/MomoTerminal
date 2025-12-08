package com.momoterminal.core.domain.usecase.settings

import com.momoterminal.core.domain.model.settings.FeatureFlags

interface UpdateFeatureFlagsUseCase {
    suspend operator fun invoke(
        userId: String,
        flags: FeatureFlags
    ): Result<Unit>
}
