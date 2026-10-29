package com.privychat.app.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversationFlow(conversationId: UUID): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE status = 'SENDING' ORDER BY timestamp ASC")
    suspend fun getPendingOutboxMessages(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: UUID): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: UUID, status: String)

    @Query("UPDATE messages SET status = :status WHERE id IN (:messageIds)")
    suspend fun updateMessagesStatus(messageIds: List<UUID>, status: String)

    @Query("DELETE FROM messages WHERE isDisappearing = 1 AND expireAt <= :now")
    suspend fun purgeExpiredDisappearingMessages(now: Instant)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: UUID)
}

