package com.momoterminal.smsbridge.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MessageStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED
}

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val messageId: String = UUID.randomUUID().toString(),
    val form: String,
    val body: String,
    val receivedAt: String,
    val deviceId: String,
    val deviceName: String,
    val simSlot: Int,
    val status: MessageStatus = MessageStatus.PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
