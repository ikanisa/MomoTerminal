package com.superapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.superapp.core.database.dao.EntityDao
import com.superapp.core.database.entity.EntityEntity

@Database(
    entities = [EntityEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SuperAppDatabase : RoomDatabase() {
    abstract fun entityDao(): EntityDao
}
