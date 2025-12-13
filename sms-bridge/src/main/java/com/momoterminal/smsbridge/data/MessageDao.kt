package com.momoterminal.smsbridge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages ORDER BY createdAt DESC LIMIT 200")
    fun getRecentMessages(): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET status = :status, attempts = :attempts, lastError = :error WHERE messageId = :messageId")
    suspend fun updateStatus(messageId: String, status: MessageStatus, attempts: Int, error: String?)
    
    @Query("SELECT * FROM messages WHERE status = 'PENDING' OR status = 'FAILED' AND attempts < 10")
    suspend fun getPendingMessages(): List<MessageEntity>
    
    @Query("SELECT * FROM messages WHERE messageId = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?
}
