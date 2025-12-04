package com.momoterminal.sync

import com.momoterminal.core.database.entity.TransactionEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles conflict resolution between local and server data.
 * Uses "last write wins" strategy based on timestamps.
 */
@Singleton
class ConflictResolver @Inject constructor() {
    
    /**
     * Result of conflict resolution.
     */
    sealed class Resolution {
        /**
         * Keep the local version.
         */
        data class KeepLocal(val transaction: TransactionEntity) : Resolution()
        
        /**
         * Use the server version.
         */
        data class UseServer(val transaction: TransactionEntity) : Resolution()
        
        /**
         * Merge both versions (for non-conflicting fields).
         */
        data class Merge(val transaction: TransactionEntity) : Resolution()
    }
    
    /**
     * Conflict type detected during sync.
     */
    enum class ConflictType {
        /**
         * Transaction exists on both local and server with different data.
         */
        DATA_MISMATCH,
        
        /**
         * Transaction was deleted on server but exists locally.
         */
        DELETED_ON_SERVER,
        
        /**
         * Transaction was modified on both local and server.
         */
        CONCURRENT_MODIFICATION
    }
    
    /**
     * Resolve a conflict between local and server versions of a transaction.
     * Uses "last write wins" strategy based on timestamps.
     * 
     * @param local The local transaction
     * @param server The server transaction
     * @param conflictType The type of conflict detected
     * @return Resolution indicating which version to keep
     */
    fun resolve(
        local: TransactionEntity,
        server: TransactionEntity?,
        conflictType: ConflictType
    ): Resolution {
        // If server version is null (deleted), keep local
        if (server == null) {
            return when (conflictType) {
                ConflictType.DELETED_ON_SERVER -> {
                    // Server deleted it, but we have pending changes - re-upload local
                    Resolution.KeepLocal(local)
                }
                else -> Resolution.KeepLocal(local)
            }
        }
        
        return when (conflictType) {
            ConflictType.DATA_MISMATCH,
            ConflictType.CONCURRENT_MODIFICATION -> {
                // Last write wins based on timestamp
                if (local.timestamp >= server.timestamp) {
                    Resolution.KeepLocal(local)
                } else {
                    Resolution.UseServer(server)
                }
            }
            ConflictType.DELETED_ON_SERVER -> {
                // Re-upload local if it has pending status
                if (local.status == "PENDING") {
                    Resolution.KeepLocal(local)
                } else {
                    Resolution.UseServer(server)
                }
            }
        }
    }
    
    /**
     * Merge local and server transactions, preferring newer values for each field.
     * This is useful for partial updates where only some fields changed.
     * 
     * @param local The local transaction
     * @param server The server transaction
     * @return Merged transaction
     */
    fun merge(
        local: TransactionEntity,
        server: TransactionEntity
    ): TransactionEntity {
        // Use newer timestamp's data, but keep local ID
        return if (local.timestamp >= server.timestamp) {
            local.copy(
                // Keep local values, but update status from server if synced
                status = if (server.status == "SENT") server.status else local.status
            )
        } else {
            server.copy(
                id = local.id // Keep local ID for database consistency
            )
        }
    }
    
    /**
     * Detect if there's a conflict between local and server versions.
     * 
     * @param local The local transaction
     * @param server The server transaction (null if not found)
     * @return The type of conflict, or null if no conflict
     */
    fun detectConflict(
        local: TransactionEntity,
        server: TransactionEntity?
    ): ConflictType? {
        if (server == null) {
            // Server doesn't have this transaction - might be deleted or never synced
            return if (local.status == "SENT") {
                ConflictType.DELETED_ON_SERVER
            } else {
                null // Not a conflict, just needs to be uploaded
            }
        }
        
        // Check for data mismatch (using amountInPesewas for comparison)
        if (local.body != server.body || 
            local.amountInPesewas != server.amountInPesewas ||
            local.sender != server.sender) {
            
            // Check if both were modified (concurrent modification)
            if (local.status != "PENDING" && server.status != local.status) {
                return ConflictType.CONCURRENT_MODIFICATION
            }
            
            return ConflictType.DATA_MISMATCH
        }
        
        return null // No conflict
    }
}
