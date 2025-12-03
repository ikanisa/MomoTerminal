package com.momoterminal.core.domain.model

import java.util.Date

/**
 * Data class for filtering transactions in the transaction history.
 */
data class TransactionFilter(
    /**
     * Filter by mobile money provider.
     */
    val provider: Provider? = null,
    
    /**
     * Filter by sync status.
     */
    val status: SyncStatus? = null,
    
    /**
     * Start date for date range filter.
     */
    val startDate: Date? = null,
    
    /**
     * End date for date range filter.
     */
    val endDate: Date? = null,
    
    /**
     * Search query for full-text search.
     */
    val searchQuery: String? = null,
    
    /**
     * Minimum amount filter.
     */
    val minAmount: Double? = null,
    
    /**
     * Maximum amount filter.
     */
    val maxAmount: Double? = null
) {
    /**
     * Check if any filter is active.
     */
    val hasActiveFilters: Boolean
        get() = provider != null || 
                status != null || 
                startDate != null || 
                endDate != null || 
                !searchQuery.isNullOrBlank() ||
                minAmount != null ||
                maxAmount != null
    
    /**
     * Get the count of active filters.
     */
    val activeFilterCount: Int
        get() {
            var count = 0
            if (provider != null) count++
            if (status != null) count++
            if (startDate != null || endDate != null) count++
            if (!searchQuery.isNullOrBlank()) count++
            if (minAmount != null || maxAmount != null) count++
            return count
        }
    
    /**
     * Clear all filters.
     */
    fun clear(): TransactionFilter = TransactionFilter()
    
    /**
     * Create a copy with updated provider filter.
     */
    fun withProvider(provider: Provider?): TransactionFilter = copy(provider = provider)
    
    /**
     * Create a copy with updated status filter.
     */
    fun withStatus(status: SyncStatus?): TransactionFilter = copy(status = status)
    
    /**
     * Create a copy with updated date range.
     */
    fun withDateRange(start: Date?, end: Date?): TransactionFilter = 
        copy(startDate = start, endDate = end)
    
    /**
     * Create a copy with updated search query.
     */
    fun withSearchQuery(query: String?): TransactionFilter = copy(searchQuery = query)
    
    /**
     * Create a copy with updated amount range.
     */
    fun withAmountRange(min: Double?, max: Double?): TransactionFilter =
        copy(minAmount = min, maxAmount = max)
    
    companion object {
        /**
         * Default filter with no active filters.
         */
        val EMPTY = TransactionFilter()
        
        /**
         * Filter for pending transactions only.
         */
        val PENDING_ONLY = TransactionFilter(status = SyncStatus.PENDING)
        
        /**
         * Filter for failed transactions only.
         */
        val FAILED_ONLY = TransactionFilter(status = SyncStatus.FAILED)
    }
}
