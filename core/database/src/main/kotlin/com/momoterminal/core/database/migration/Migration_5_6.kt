package com.momoterminal.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 5 to 6 - adds sync tracking fields to sms_transactions table.
 * Adds: sync_status, parsed_by, ai_confidence, and supabase_id fields.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new columns to sms_transactions table
        db.execSQL("ALTER TABLE sms_transactions ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE sms_transactions ADD COLUMN parsed_by TEXT NOT NULL DEFAULT 'regex'")
        db.execSQL("ALTER TABLE sms_transactions ADD COLUMN ai_confidence REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE sms_transactions ADD COLUMN supabase_id TEXT DEFAULT NULL")
        
        // Create index for sync_status to optimize pending transaction queries
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sms_transactions_sync_status ON sms_transactions(sync_status)")
    }
}
