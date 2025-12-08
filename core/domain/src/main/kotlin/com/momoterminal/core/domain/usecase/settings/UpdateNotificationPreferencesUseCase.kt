package com.momoterminal.core.domain.usecase.settings

import com.momoterminal.core.domain.model.settings.NotificationPreferences

interface UpdateNotificationPreferencesUseCase {
    suspend operator fun invoke(
        userId: String,
        preferences: NotificationPreferences
    ): Result<Unit>
}
