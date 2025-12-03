package com.momoterminal.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nfc_tags")
data class NfcTagEntity(
    @PrimaryKey val tagId: String,
    val entityType: String, // "merchant", "user", "token_pack"
    val entityId: String,
    val metadata: String?, // JSON
    val lastScanned: Long = System.currentTimeMillis()
)
