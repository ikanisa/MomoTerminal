package com.momoterminal.feature.settings.viewmodel

import com.momoterminal.core.common.UiEffect
import com.momoterminal.core.common.UiEvent
import com.momoterminal.core.common.UiState
import com.momoterminal.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : BaseViewModel<SettingsState, SettingsEvent, SettingsEffect>(SettingsState()) {

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleNotifications -> updateState { copy(notificationsEnabled = !notificationsEnabled) }
            is SettingsEvent.ChangeLanguage -> updateState { copy(selectedLanguage = event.language) }
        }
    }
}

data class SettingsState(
    val notificationsEnabled: Boolean = true,
    val selectedLanguage: String = "en"
) : UiState

sealed class SettingsEvent : UiEvent {
    data object ToggleNotifications : SettingsEvent()
    data class ChangeLanguage(val language: String) : SettingsEvent()
}

sealed class SettingsEffect : UiEffect
