package com.superapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.superapp.core.database.entity.EntityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntityDao {

    @Query("SELECT * FROM entities ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    fun getEntities(limit: Int, offset: Int): Flow<List<EntityEntity>>

    @Query("SELECT * FROM entities WHERE id = :id")
    fun getEntityById(id: String): Flow<EntityEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntities(entities: List<EntityEntity>)

    @Query("DELETE FROM entities")
    suspend fun clearAll()
}
