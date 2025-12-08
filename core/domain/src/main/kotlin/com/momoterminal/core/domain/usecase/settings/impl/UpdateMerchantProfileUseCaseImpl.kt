package com.momoterminal.core.domain.usecase.settings.impl

import com.momoterminal.core.domain.model.settings.MerchantStatus
import com.momoterminal.core.domain.repository.SettingsRepository
import com.momoterminal.core.domain.usecase.settings.UpdateMerchantProfileUseCase
import javax.inject.Inject

class UpdateMerchantProfileUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : UpdateMerchantProfileUseCase {
    
    override suspend fun invoke(
        userId: String,
        businessName: String?,
        status: MerchantStatus?
    ): Result<Unit> {
        require(businessName != null || status != null) {
            "At least one field must be provided for update"
        }
        
        businessName?.let {
            require(it.isNotBlank()) { "Business name cannot be blank" }
            require(it.length <= 255) { "Business name too long (max 255 characters)" }
        }
        
        return repository.updateProfile(userId, businessName, status)
    }
}
