package com.momoterminal.error

/**
 * Action that can be taken in response to an error.
 */
sealed class ErrorAction {
    /**
     * Retry the failed operation.
     */
    data class Retry(val action: () -> Unit) : ErrorAction()
    
    /**
     * Open system settings (e.g., to enable NFC).
     */
    data class OpenSettings(val settingsAction: String) : ErrorAction()
    
    /**
     * Navigate to a specific screen.
     */
    data class Navigate(val route: String) : ErrorAction()
    
    /**
     * Dismiss the error without action.
     */
    data object Dismiss : ErrorAction()
}

/**
 * Data class representing an error event that can be displayed to the user.
 */
data class ErrorEvent(
    /**
     * The error that occurred.
     */
    val error: AppError,
    
    /**
     * Optional action that can be taken.
     */
    val action: ErrorAction? = null,
    
    /**
     * Label for the action button (if action is provided).
     */
    val actionLabel: String? = null,
    
    /**
     * Unique ID for this error event (for deduplication).
     */
    val id: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Create an error event from an AppError with appropriate action.
         */
        fun from(
            error: AppError,
            retryAction: (() -> Unit)? = null
        ): ErrorEvent {
            val action = when {
                error.isRecoverable && retryAction != null -> ErrorAction.Retry(retryAction)
                error is AppError.Nfc.Disabled -> ErrorAction.OpenSettings("android.settings.NFC_SETTINGS")
                error is AppError.Sms.PermissionDenied -> ErrorAction.OpenSettings("android.settings.APPLICATION_DETAILS_SETTINGS")
                error is AppError.Biometric.NotEnrolled -> ErrorAction.OpenSettings("android.settings.BIOMETRIC_ENROLL")
                error is AppError.Network.NoConnection -> ErrorAction.OpenSettings("android.settings.WIFI_SETTINGS")
                else -> ErrorAction.Dismiss
            }
            
            val actionLabel = when (action) {
                is ErrorAction.Retry -> "Retry"
                is ErrorAction.OpenSettings -> "Open Settings"
                is ErrorAction.Navigate -> "Go"
                is ErrorAction.Dismiss -> null
            }
            
            return ErrorEvent(
                error = error,
                action = action,
                actionLabel = actionLabel
            )
        }
    }
}
