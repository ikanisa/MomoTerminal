package com.superapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entities")
data class EntityEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val description: String?,
    val metadataJson: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)
