package com.privychat.app.domain.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val displayName: String,
    val email: String,
    val phoneNumber: String,
    val avatarUrl: String?,
    val isEmailVerified: Boolean,
    val isPhoneVerified: Boolean,
    val pendingEmail: String? = null,
    val pendingPhoneNumber: String? = null,
    val createdAt: String? = null
)

data class Conversation(
    val id: UUID,
    val participantUserId: UUID,
    val participantName: String,
    val participantPhone: String,
    val participantAvatarUrl: String?,
    val lastMessagePreview: String?,
    val lastMessageTimestamp: Instant,
    val unreadCount: Int,
    val isMuted: Boolean = false
)

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
}

enum class AttachmentType {
    IMAGE, VIDEO, AUDIO, DOCUMENT, LOCATION
}

data class Message(
    val id: UUID,
    val conversationId: UUID,
    val senderId: UUID,
    val recipientId: UUID,
    val isOutgoing: Boolean,
    val text: String,
    val status: MessageStatus,
    val timestamp: Instant,
    val isAttachment: Boolean = false,
    val attachmentPath: String? = null,
    val attachmentType: AttachmentType? = null,
    val attachmentSize: Long = 0,
    val isDisappearing: Boolean = false
)

data class CallSession(
    val id: UUID,
    val otherPartyName: String,
    val otherPartyAvatar: String?,
    val isOutgoing: Boolean,
    val callType: String,
    val status: String,
    val timestamp: Instant,
    val durationSeconds: Int?
)

