package com.momoterminal.domain.model

/**
 * Enum representing the synchronization status of a transaction.
 */
enum class SyncStatus(val value: String) {
    /**
     * Transaction is pending upload to server.
     */
    PENDING("PENDING"),
    
    /**
     * Transaction has been successfully synced to server.
     */
    SENT("SENT"),
    
    /**
     * Transaction sync failed.
     */
    FAILED("FAILED");
    
    companion object {
        /**
         * Convert string value to SyncStatus.
         */
        fun fromValue(value: String): SyncStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}
