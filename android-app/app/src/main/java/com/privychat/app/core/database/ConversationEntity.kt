package com.privychat.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: UUID,
    val participantUserId: UUID,
    val participantName: String,
    val participantPhone: String,
    val participantAvatarUrl: String?,
    val lastMessagePreview: String?,
    val lastMessageTimestamp: Instant,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false
)

