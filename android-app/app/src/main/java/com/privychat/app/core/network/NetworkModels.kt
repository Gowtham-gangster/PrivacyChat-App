package com.privychat.app.core.network

import com.google.gson.annotations.SerializedName
import java.util.UUID

// =============================================================================
// Authentication & Profiles
// =============================================================================

data class RegisterRequestDto(
    val displayName: String,
    val email: String,
    val phoneNumber: String,
    val password: String,
    val termsAccepted: Boolean
)

data class LoginRequestDto(
    val identifier: String,
    val password: String,
    val deviceLabel: String
)

data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserProfileDto,
    val deviceId: UUID
)

data class UserProfileDto(
    val id: UUID,
    val displayName: String,
    val email: String,
    val phoneNumber: String,
    val avatarUrl: String?,
    val emailVerified: Boolean,
    val phoneVerified: Boolean
)

data class OtpVerificationDto(
    val identifier: String,
    val otpCode: String
)

data class RefreshTokenDto(
    val refreshToken: String
)

data class UpdateProfileDto(
    val displayName: String?,
    val avatarUrl: String?
)

data class PrivacySettingsDto(
    val readReceiptsEnabled: Boolean,
    val lastSeenVisibility: String,
    val profilePhotoVisibility: String
)

// =============================================================================
// Contacts & Invitations
// =============================================================================

data class ContactLookupRequestDto(
    val phoneNumbers: List<String>
)

data class ContactResponseDto(
    val userId: UUID?,
    val displayName: String?,
    val phoneNumber: String,
    val avatarUrl: String?,
    val registered: Boolean
)

data class SendInvitationDto(
    val recipientEmail: String
)

data class InvitationResponseDto(
    val id: UUID,
    val recipientEmail: String,
    val status: String,
    val expiresAt: String
)

// =============================================================================
// Cryptographic Keys (E2EE)
// =============================================================================

data class SignedPreKeyDto(
    val keyId: Int,
    val publicKey: String,
    val signature: String
)

data class OneTimePreKeyDto(
    val keyId: Int,
    val publicKey: String
)

data class PublishKeysDto(
    val deviceId: UUID,
    val identityKey: String,
    val registrationId: Int,
    val signedPreKey: SignedPreKeyDto,
    val oneTimePreKeys: List<OneTimePreKeyDto>
)

data class PreKeyBundleDto(
    val userId: UUID,
    val deviceId: UUID,
    val registrationId: Int,
    val identityKey: String,
    val signedPreKey: SignedPreKeyDto,
    val oneTimePreKey: OneTimePreKeyDto?
)

// =============================================================================
// Conversations & Messages
// =============================================================================

data class ConversationResponseDto(
    val id: UUID,
    val participant: UserProfileDto,
    val lastMessageTimestamp: String,
    val unreadCount: Long,
    val createdAt: String
)

data class DirectConversationRequestDto(
    val recipientUserId: UUID
)

data class SendMessageDto(
    val messageId: UUID,
    val conversationId: UUID,
    val recipientId: UUID,
    val recipientDeviceId: UUID?,
    val ciphertext: String,
    val iv: String,
    val ratchetPublicKey: String?,
    val sequenceNumber: Long,
    val previousCounter: Int,
    val oneTimePreKeyId: Int?
)

data class MessageResponseDto(
    val id: UUID,
    val conversationId: UUID,
    val senderId: UUID,
    val recipientId: UUID,
    val ciphertext: String,
    val iv: String,
    val ratchetPublicKey: String?,
    val sequenceNumber: Long,
    val previousCounter: Int,
    val oneTimePreKeyId: Int?,
    val status: String,
    val delivered: Boolean,
    val read: Boolean,
    val createdAt: String
)

data class MessageReceiptDto(
    val messageIds: List<UUID>,
    val type: String,
    val conversationId: UUID
)

// =============================================================================
// Attachments
// =============================================================================

data class AttachmentUploadRequestDto(
    val messageId: UUID?,
    val conversationId: UUID? = null,
    val fileSizeBytes: Long,
    val sha256Checksum: String,
    val mimeType: String
)

data class AttachmentUploadResponseDto(
    val attachmentId: UUID,
    val uploadUrl: String,
    val maxSizeBytes: Long
)

data class AttachmentDownloadResponseDto(
    val attachmentId: UUID,
    val downloadUrl: String,
    val signedUrl: String? = null,
    val expiresInSeconds: Int? = null,
    val fileSizeBytes: Long,
    val sha256Checksum: String,
    val mimeType: String
)

// =============================================================================
// Calling & Signaling
// =============================================================================

data class CallSignalDto(
    val callId: UUID?,
    val callerId: UUID?,
    val recipientId: UUID,
    val signalType: String,
    val callType: String,
    val sdp: String?,
    val candidate: IceCandidateDto?
)

data class IceCandidateDto(
    val sdpMid: String?,
    val sdpMLineIndex: Int,
    val candidate: String
)

data class CallSessionResponseDto(
    val id: UUID,
    val otherParty: UserProfileDto,
    val isOutgoing: Boolean,
    val callType: String,
    val status: String,
    val startedAt: String,
    val durationSeconds: Int?
)

