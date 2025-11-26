package com.momoterminal.sync

/**
 * Sealed class representing the synchronization state of the application.
 * Used to track the current status of data synchronization with the server.
 */
sealed class SyncState {
    /**
     * No sync operation is in progress.
     */
    data object Idle : SyncState()
    
    /**
     * Sync is currently in progress.
     * @param progress Optional progress percentage (0-100)
     */
    data class Syncing(val progress: Int = 0) : SyncState()
    
    /**
     * Sync completed successfully.
     * @param syncedCount Number of transactions synced
     */
    data class Success(val syncedCount: Int) : SyncState()
    
    /**
     * Sync failed with an error.
     * @param message Error message describing what went wrong
     * @param isRetryable Whether the sync can be retried
     */
    data class Error(
        val message: String,
        val isRetryable: Boolean = true
    ) : SyncState()
    
    /**
     * Device is offline, sync is not possible.
     */
    data object Offline : SyncState()
    
    /**
     * Whether sync is currently in progress.
     */
    val isSyncing: Boolean
        get() = this is Syncing
    
    /**
     * Whether the device is online and sync is possible.
     */
    val isOnline: Boolean
        get() = this !is Offline
    
    /**
     * Whether sync has completed (success or failure).
     */
    val isComplete: Boolean
        get() = this is Success || this is Error
    
    /**
     * Get a user-friendly status message.
     */
    fun getStatusMessage(): String = when (this) {
        is Idle -> "Up to date"
        is Syncing -> "Syncing..."
        is Success -> "Synced $syncedCount transactions"
        is Error -> message
        is Offline -> "Offline"
    }
}
