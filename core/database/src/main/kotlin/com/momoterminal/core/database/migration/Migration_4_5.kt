package com.momoterminal.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 4 to 5 - adds wallet_tokens table.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create wallet_tokens table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS wallet_tokens (
                id TEXT PRIMARY KEY NOT NULL,
                amount INTEGER NOT NULL,
                currency TEXT NOT NULL,
                source_reference TEXT,
                source_type TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                status TEXT NOT NULL,
                expires_at INTEGER,
                user_id TEXT NOT NULL
            )
        """.trimIndent())
        
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_wallet_tokens_source_reference ON wallet_tokens(source_reference)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_wallet_tokens_timestamp ON wallet_tokens(timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_wallet_tokens_status ON wallet_tokens(status)")
    }
}
