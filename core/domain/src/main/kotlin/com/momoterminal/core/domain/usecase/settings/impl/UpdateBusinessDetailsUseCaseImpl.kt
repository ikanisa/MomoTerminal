package com.momoterminal.core.domain.usecase.settings.impl

import com.momoterminal.core.domain.model.settings.BusinessDetails
import com.momoterminal.core.domain.repository.SettingsRepository
import com.momoterminal.core.domain.usecase.settings.UpdateBusinessDetailsUseCase
import javax.inject.Inject

class UpdateBusinessDetailsUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : UpdateBusinessDetailsUseCase {
    
    override suspend fun invoke(
        userId: String,
        businessDetails: BusinessDetails
    ): Result<Unit> {
        businessDetails.taxId?.let {
            require(it.isNotBlank()) { "Tax ID cannot be blank" }
        }
        
        businessDetails.website?.let {
            require(it.startsWith("http://") || it.startsWith("https://")) {
                "Website must be a valid URL"
            }
        }
        
        return repository.updateBusinessDetails(userId, businessDetails)
    }
}
