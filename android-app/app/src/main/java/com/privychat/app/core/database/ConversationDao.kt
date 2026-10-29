package com.privychat.app.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    fun getAllConversationsFlow(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: UUID): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE participantUserId = :userId")
    suspend fun getConversationByParticipantId(userId: UUID): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markConversationRead(conversationId: UUID)

    @Delete
    suspend fun delete(conversation: ConversationEntity)
}

