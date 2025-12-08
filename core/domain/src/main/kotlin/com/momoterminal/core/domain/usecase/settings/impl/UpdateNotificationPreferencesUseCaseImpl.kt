package com.momoterminal.core.domain.usecase.settings.impl

import com.momoterminal.core.domain.model.settings.NotificationPreferences
import com.momoterminal.core.domain.repository.SettingsRepository
import com.momoterminal.core.domain.usecase.settings.UpdateNotificationPreferencesUseCase
import javax.inject.Inject

class UpdateNotificationPreferencesUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : UpdateNotificationPreferencesUseCase {
    
    override suspend fun invoke(
        userId: String,
        preferences: NotificationPreferences
    ): Result<Unit> {
        preferences.quietHours?.let { quietHours ->
            val timePattern = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
            require(quietHours.startTime.matches(timePattern)) {
                "Start time must be in HH:mm format"
            }
            require(quietHours.endTime.matches(timePattern)) {
                "End time must be in HH:mm format"
            }
        }
        
        return repository.updateNotificationPreferences(userId, preferences)
    }
}
