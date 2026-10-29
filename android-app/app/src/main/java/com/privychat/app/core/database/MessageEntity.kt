package com.privychat.app.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId", "timestamp"]),
        Index(value = ["status"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: UUID,
    val conversationId: UUID,
    val senderId: UUID,
    val recipientId: UUID,
    val isOutgoing: Boolean,
    val plaintext: String, // Stored encrypted by SQLCipher in local SQLite
    val ciphertext: String?,
    val iv: String?,
    val sequenceNumber: Long,
    val status: String, // SENDING, SENT, DELIVERED, READ, FAILED
    val timestamp: Instant,
    val isAttachment: Boolean = false,
    val attachmentPath: String? = null,
    val attachmentType: String? = null, // IMAGE, VIDEO, AUDIO, DOCUMENT, LOCATION
    val attachmentSize: Long = 0,
    val isDisappearing: Boolean = false,
    val expireAt: Instant? = null
)

