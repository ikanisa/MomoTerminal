package com.momoterminal.core.ui

import com.momoterminal.core.error.MomoError

/**
 * Generic UI state wrapper for offline-first architecture.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: MomoError) : UiState<Nothing>
}

/**
 * Actions for error recovery in UI.
 */
sealed interface ErrorAction {
    data object Retry : ErrorAction
    data class OpenSettings(val settingsType: SettingsType) : ErrorAction
    data class ViewRawData(val data: String) : ErrorAction
}

enum class SettingsType { APP_PERMISSIONS, NFC_SETTINGS, NETWORK_SETTINGS }
