package com.momoterminal.domain.model

/**
 * Enum representing the synchronization status of a transaction.
 */
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    SENT,  // Legacy - same as SYNCED
    FAILED;
    
    companion object {
        fun fromValue(value: String): SyncStatus {
            return entries.find { it.name == value } ?: PENDING
        }
    }
}
