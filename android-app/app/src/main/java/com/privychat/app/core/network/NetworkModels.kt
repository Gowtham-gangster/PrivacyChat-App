package com.privychat.app.core.network

import com.google.gson.annotations.SerializedName
import java.util.UUID

// =============================================================================
// Authentication & Profiles
// =============================================================================

data class RegisterRequestDto(
    @SerializedName("displayName")
    val displayName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("termsAccepted")
    val termsAccepted: Boolean
)

data class LoginRequestDto(
    @SerializedName("identifier")
    val identifier: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("deviceLabel")
    val deviceLabel: String
)

data class AuthResponseDto(
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String,
    @SerializedName("tokenType")
    val tokenType: String,
    @SerializedName("expiresIn")
    val expiresIn: Long,
    @SerializedName("user")
    val user: UserProfileDto,
    @SerializedName("deviceId")
    val deviceId: UUID
)

data class UserProfileDto(
    @SerializedName("id")
    val id: UUID,
    @SerializedName("displayName")
    val displayName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    @SerializedName("avatarUrl")
    val avatarUrl: String?,
    @SerializedName("emailVerified")
    val emailVerified: Boolean,
    @SerializedName("phoneVerified")
    val phoneVerified: Boolean,
    @SerializedName("pendingEmail")
    val pendingEmail: String? = null,
    @SerializedName("pendingPhoneNumber")
    val pendingPhoneNumber: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null
)

data class EmailChangeRequestDto(
    @SerializedName("newEmail")
    val newEmail: String
)

data class VerifyEmailChangeRequestDto(
    @SerializedName("token")
    val token: String
)

data class PhoneChangeRequestDto(
    @SerializedName("newPhoneNumber")
    val newPhoneNumber: String
)

data class VerifyPhoneChangeRequestDto(
    @SerializedName("code")
    val code: String
)

data class OtpVerificationDto(
    @SerializedName("identifier")
    val identifier: String,
    @SerializedName("otpCode")
    val otpCode: String
)

data class ForgotPasswordRequestDto(
    @SerializedName("email")
    val email: String
)

data class ResetPasswordRequestDto(
    @SerializedName("email")
    val email: String,
    @SerializedName("token")
    val token: String,
    @SerializedName("newPassword")
    val newPassword: String
)

data class ResendVerificationRequestDto(
    @SerializedName("email")
    val email: String
)

data class RefreshTokenDto(
    @SerializedName("refreshToken")
    val refreshToken: String
)

data class UpdateProfileDto(
    @SerializedName("displayName")
    val displayName: String?,
    @SerializedName("avatarUrl")
    val avatarUrl: String?
)

data class PrivacySettingsDto(
    @SerializedName("readReceiptsEnabled")
    val readReceiptsEnabled: Boolean,
    @SerializedName("lastSeenVisibility")
    val lastSeenVisibility: String,
    @SerializedName("profilePhotoVisibility")
    val profilePhotoVisibility: String
)

// =============================================================================
// Contacts & Invitations
// =============================================================================

data class ContactLookupRequestDto(
    @SerializedName("phoneNumbers")
    val phoneNumbers: List<String>
)

data class ContactResponseDto(
    @SerializedName("userId")
    val userId: UUID?,
    @SerializedName("displayName")
    val displayName: String?,
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    @SerializedName("avatarUrl")
    val avatarUrl: String?,
    @SerializedName("registered")
    val registered: Boolean
)

data class SendInvitationDto(
    @SerializedName("recipientEmail")
    val recipientEmail: String
)

data class InvitationResponseDto(
    @SerializedName("id")
    val id: UUID,
    @SerializedName("recipientEmail")
    val recipientEmail: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("expiresAt")
    val expiresAt: String
)

// =============================================================================
// Cryptographic Keys (E2EE)
// =============================================================================

data class SignedPreKeyDto(
    @SerializedName("keyId")
    val keyId: Int,
    @SerializedName("publicKey")
    val publicKey: String,
    @SerializedName("signature")
    val signature: String
)

data class OneTimePreKeyDto(
    @SerializedName("keyId")
    val keyId: Int,
    @SerializedName("publicKey")
    val publicKey: String
)

data class PublishKeysDto(
    @SerializedName("deviceId")
    val deviceId: UUID,
    @SerializedName("identityKey")
    val identityKey: String,
    @SerializedName("registrationId")
    val registrationId: Int,
    @SerializedName("signedPreKey")
    val signedPreKey: SignedPreKeyDto,
    @SerializedName("oneTimePreKeys")
    val oneTimePreKeys: List<OneTimePreKeyDto>
)

data class PreKeyBundleDto(
    @SerializedName("userId")
    val userId: UUID,
    @SerializedName("deviceId")
    val deviceId: UUID,
    @SerializedName("registrationId")
    val registrationId: Int,
    @SerializedName("identityKey")
    val identityKey: String,
    @SerializedName("signedPreKey")
    val signedPreKey: SignedPreKeyDto,
    @SerializedName("oneTimePreKey")
    val oneTimePreKey: OneTimePreKeyDto?
)

// =============================================================================
// Conversations & Messages
// =============================================================================

data class ConversationResponseDto(
    @SerializedName("id")
    val id: UUID,
    @SerializedName("participant")
    val participant: UserProfileDto,
    @SerializedName("lastMessageTimestamp")
    val lastMessageTimestamp: String,
    @SerializedName("unreadCount")
    val unreadCount: Long,
    @SerializedName("createdAt")
    val createdAt: String
)

data class DirectConversationRequestDto(
    @SerializedName("recipientUserId")
    val recipientUserId: UUID
)

data class SendMessageDto(
    @SerializedName("messageId")
    val messageId: UUID,
    @SerializedName("conversationId")
    val conversationId: UUID,
    @SerializedName("recipientId")
    val recipientId: UUID,
    @SerializedName("recipientDeviceId")
    val recipientDeviceId: UUID?,
    @SerializedName("ciphertext")
    val ciphertext: String,
    @SerializedName("iv")
    val iv: String,
    @SerializedName("ratchetPublicKey")
    val ratchetPublicKey: String?,
    @SerializedName("sequenceNumber")
    val sequenceNumber: Long,
    @SerializedName("previousCounter")
    val previousCounter: Int,
    @SerializedName("oneTimePreKeyId")
    val oneTimePreKeyId: Int?
)

data class MessageResponseDto(
    @SerializedName("id")
    val id: UUID,
    @SerializedName("conversationId")
    val conversationId: UUID,
    @SerializedName("senderId")
    val senderId: UUID,
    @SerializedName("recipientId")
    val recipientId: UUID,
    @SerializedName("ciphertext")
    val ciphertext: String,
    @SerializedName("iv")
    val iv: String,
    @SerializedName("ratchetPublicKey")
    val ratchetPublicKey: String?,
    @SerializedName("sequenceNumber")
    val sequenceNumber: Long,
    @SerializedName("previousCounter")
    val previousCounter: Int,
    @SerializedName("oneTimePreKeyId")
    val oneTimePreKeyId: Int?,
    @SerializedName("status")
    val status: String,
    @SerializedName("delivered")
    val delivered: Boolean,
    @SerializedName("read")
    val read: Boolean,
    @SerializedName("createdAt")
    val createdAt: String
)

data class MessageReceiptDto(
    @SerializedName("messageIds")
    val messageIds: List<UUID>,
    @SerializedName("type")
    val type: String,
    @SerializedName("conversationId")
    val conversationId: UUID
)

// =============================================================================
// Attachments
// =============================================================================

data class AttachmentUploadRequestDto(
    @SerializedName("messageId")
    val messageId: UUID?,
    @SerializedName("conversationId")
    val conversationId: UUID? = null,
    @SerializedName("fileSizeBytes")
    val fileSizeBytes: Long,
    @SerializedName("sha256Checksum")
    val sha256Checksum: String,
    @SerializedName("mimeType")
    val mimeType: String
)

data class AttachmentUploadResponseDto(
    @SerializedName("attachmentId")
    val attachmentId: UUID,
    @SerializedName("uploadUrl")
    val uploadUrl: String,
    @SerializedName("maxSizeBytes")
    val maxSizeBytes: Long
)

data class AttachmentDownloadResponseDto(
    @SerializedName("attachmentId")
    val attachmentId: UUID,
    @SerializedName("downloadUrl")
    val downloadUrl: String,
    @SerializedName("signedUrl")
    val signedUrl: String? = null,
    @SerializedName("expiresInSeconds")
    val expiresInSeconds: Int? = null,
    @SerializedName("fileSizeBytes")
    val fileSizeBytes: Long,
    @SerializedName("sha256Checksum")
    val sha256Checksum: String,
    @SerializedName("mimeType")
    val mimeType: String
)

// =============================================================================
// Calling & Signaling
// =============================================================================

data class CallSignalDto(
    @SerializedName("callId")
    val callId: UUID?,
    @SerializedName("callerId")
    val callerId: UUID?,
    @SerializedName("recipientId")
    val recipientId: UUID,
    @SerializedName("signalType")
    val signalType: String,
    @SerializedName("callType")
    val callType: String,
    @SerializedName("sdp")
    val sdp: String?,
    @SerializedName("candidate")
    val candidate: IceCandidateDto?
)

data class IceCandidateDto(
    @SerializedName("sdpMid")
    val sdpMid: String?,
    @SerializedName("sdpMLineIndex")
    val sdpMLineIndex: Int,
    @SerializedName("candidate")
    val candidate: String
)

data class IceServerDto(
    @SerializedName("urls")
    val urls: List<String>,
    @SerializedName("username")
    val username: String? = null,
    @SerializedName("credential")
    val credential: String? = null
)

data class IceConfigResponseDto(
    @SerializedName("iceServers")
    val iceServers: List<IceServerDto>,
    @SerializedName("ttl")
    val ttl: Long,
    @SerializedName("turnConfigured")
    val turnConfigured: Boolean
)

data class CallSessionResponseDto(
    @SerializedName("id")
    val id: UUID,
    @SerializedName("otherParty")
    val otherParty: UserProfileDto,
    @SerializedName("isOutgoing")
    val isOutgoing: Boolean,
    @SerializedName("callType")
    val callType: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("startedAt")
    val startedAt: String,
    @SerializedName("durationSeconds")
    val durationSeconds: Int?
)
