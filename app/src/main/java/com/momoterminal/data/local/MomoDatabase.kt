package com.momoterminal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.momoterminal.data.local.dao.TransactionDao
import com.momoterminal.data.local.entity.TransactionEntity

/**
 * Room Database for MomoTerminal application.
 * Provides the database instance for local transaction storage.
 */
@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MomoDatabase : RoomDatabase() {
    
    /**
     * Get the TransactionDao for database operations.
     */
    abstract fun transactionDao(): TransactionDao
}
