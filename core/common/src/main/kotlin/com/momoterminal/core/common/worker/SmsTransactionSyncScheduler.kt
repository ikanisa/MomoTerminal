package com.momoterminal.core.common.worker

import android.content.Context

/**
 * Interface for scheduling SMS transaction sync work.
 * This avoids circular dependencies between modules.
 */
interface SmsTransactionSyncScheduler {
    /**
     * Schedule a sync worker to upload pending SMS transactions to Supabase.
     */
    fun scheduleSync(context: Context)
}
