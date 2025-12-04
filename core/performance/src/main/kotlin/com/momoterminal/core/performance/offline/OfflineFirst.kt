package com.momoterminal.core.performance.offline

/**
 * Offline-first architecture models.
 * These are simplified stubs - implement as needed.
 */

/**
 * Represents a pending action to be synced.
 */
data class PendingAction(
    val id: Long,
    val type: String,
    val data: String,
    val timestamp: Long,
    val retryCount: Int = 0
)

/**
 * Sync status for offline items.
 */
enum class SyncStatus {
    PENDING, SYNCING, SYNCED, FAILED
}

/**
 * Queue for managing pending actions.
 */
interface PendingActionQueue {
    suspend fun enqueue(action: PendingAction)
    suspend fun dequeue(): PendingAction?
    suspend fun markSynced(actionId: Long)
    suspend fun markFailed(actionId: Long)
}
