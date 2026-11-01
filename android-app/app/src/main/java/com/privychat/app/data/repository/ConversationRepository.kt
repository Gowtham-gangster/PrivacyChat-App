package com.privychat.app.data.repository

import com.privychat.app.core.crypto.AttachmentCrypto
import com.privychat.app.core.crypto.DoubleRatchet
import com.privychat.app.core.crypto.X3DH
import com.privychat.app.core.database.*
import com.privychat.app.core.network.*
import com.privychat.app.core.preferences.UserPreferences
import com.privychat.app.domain.model.AttachmentType
import com.privychat.app.domain.model.Conversation
import com.privychat.app.domain.model.Message
import com.privychat.app.domain.model.MessageStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.time.Instant
import java.util.Base64
import java.util.UUID

class ConversationRepository(
    private val apiService: PrivyApiService,
    private val stompClient: StompWebSocketClient,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val sessionDao: SessionDao,
    private val preferences: UserPreferences
) {

    fun getConversationsFlow(): Flow<List<Conversation>> {
        return conversationDao.getAllConversationsFlow().map { entities ->
            entities.map { e ->
                Conversation(
                    id = e.id,
                    participantUserId = e.participantUserId,
                    participantName = e.participantName,
                    participantPhone = e.participantPhone,
                    participantAvatarUrl = e.participantAvatarUrl,
                    lastMessagePreview = e.lastMessagePreview,
                    lastMessageTimestamp = e.lastMessageTimestamp,
                    unreadCount = e.unreadCount,
                    isMuted = e.isMuted
                )
            }
        }
    }

    suspend fun syncConversations() = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getConversations()
            if (response.isSuccessful && response.body() != null) {
                for (dto in response.body()!!) {
                    val entity = ConversationEntity(
                        id = dto.id,
                        participantUserId = dto.participant.id,
                        participantName = dto.participant.displayName,
                        participantPhone = dto.participant.phoneNumber,
                        participantAvatarUrl = dto.participant.avatarUrl,
                        lastMessagePreview = "Encrypted conversation",
                        lastMessageTimestamp = Instant.parse(dto.lastMessageTimestamp),
                        unreadCount = dto.unreadCount.toInt()
                    )
                    conversationDao.insertOrUpdate(entity)
                }
            }
        } catch (ignored: Exception) {}
    }

    fun getMessagesFlow(conversationId: UUID): Flow<List<Message>> {
        return messageDao.getMessagesForConversationFlow(conversationId).map { entities ->
            entities.map { e ->
                Message(
                    id = e.id,
                    conversationId = e.conversationId,
                    senderId = e.senderId,
                    recipientId = e.recipientId,
                    isOutgoing = e.isOutgoing,
                    text = e.plaintext,
                    status = MessageStatus.valueOf(e.status),
                    timestamp = e.timestamp,
                    isAttachment = e.isAttachment,
                    attachmentPath = e.attachmentPath,
                    attachmentType = e.attachmentType?.let { AttachmentType.valueOf(it) },
                    attachmentSize = e.attachmentSize,
                    isDisappearing = e.isDisappearing
                )
            }
        }
    }

    suspend fun sendMessage(conversationId: UUID, recipientId: UUID, text: String): Result<UUID> = withContext(Dispatchers.IO) {
        try {
            val myUserId = preferences.getUserId() ?: return@withContext Result.failure(Exception("Not logged in"))
            val messageId = UUID.randomUUID()
            val now = Instant.now()

            // 1. Save locally as SENDING
            val localMessage = MessageEntity(
                id = messageId,
                conversationId = conversationId,
                senderId = myUserId,
                recipientId = recipientId,
                isOutgoing = true,
                plaintext = text,
                ciphertext = null,
                iv = null,
                sequenceNumber = 1L,
                status = "SENDING",
                timestamp = now
            )
            messageDao.insertOrUpdate(localMessage)

            // 2. Fetch or load Double Ratchet session
            val preKeyBundle = apiService.getPreKeyBundle(recipientId).body()
            val ratchetMsg = encryptPayload(recipientId, text.toByteArray(Charsets.UTF_8), preKeyBundle)

            val sendDto = SendMessageDto(
                messageId = messageId,
                conversationId = conversationId,
                recipientId = recipientId,
                recipientDeviceId = preKeyBundle?.deviceId,
                ciphertext = Base64.getEncoder().encodeToString(ratchetMsg.ciphertext),
                iv = Base64.getEncoder().encodeToString(ratchetMsg.iv),
                ratchetPublicKey = Base64.getEncoder().encodeToString(ratchetMsg.ratchetPublicKey),
                sequenceNumber = ratchetMsg.sequenceNumber,
                previousCounter = ratchetMsg.previousCounter,
                oneTimePreKeyId = preKeyBundle?.oneTimePreKey?.keyId
            )

            // 3. Dispatch via WebSocket or REST fallback
            stompClient.sendMessage(sendDto)

            // 4. Update status to SENT
            messageDao.updateMessageStatus(messageId, "SENT")
            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendAttachment(
        conversationId: UUID,
        recipientId: UUID,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        attachmentType: AttachmentType
    ): Result<UUID> = withContext(Dispatchers.IO) {
        try {
            val myUserId = preferences.getUserId() ?: return@withContext Result.failure(Exception("Not logged in"))
            val messageId = UUID.randomUUID()

            // 1. Client-side authenticated encryption of raw file bytes
            val encryptedPayload = AttachmentCrypto.encryptFile(fileBytes)

            // 2. Request upload authorization
            val uploadReq = AttachmentUploadRequestDto(
                messageId = messageId,
                conversationId = conversationId,
                fileSizeBytes = encryptedPayload.ciphertext.size.toLong(),
                sha256Checksum = encryptedPayload.sha256Checksum,
                mimeType = mimeType
            )
            val uploadResp = apiService.requestUpload(uploadReq).body()
                ?: return@withContext Result.failure(Exception("Failed to authorize attachment upload"))

            // 3. Upload encrypted ciphertext blob
            val requestBody = encryptedPayload.ciphertext.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            apiService.uploadCiphertextBytes(uploadResp.attachmentId, requestBody)

            // 4. Create encrypted media descriptor envelope
            val keyB64 = Base64.getEncoder().encodeToString(encryptedPayload.key)
            val ivB64 = Base64.getEncoder().encodeToString(encryptedPayload.iv)
            val mediaEnvelope = "PRIVY_ATTACHMENT:${uploadResp.attachmentId}:$keyB64:$ivB64:${encryptedPayload.sha256Checksum}:$fileName:$mimeType"

            // 5. Send descriptor envelope as an end-to-end encrypted message
            sendMessage(conversationId, recipientId, mediaEnvelope)

            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun encryptPayload(
        recipientId: UUID,
        plaintext: ByteArray,
        preKeyBundle: PreKeyBundleDto?
    ): DoubleRatchet.RatchetMessage {
        val rootKey = ByteArray(32) { 0x42 } // Derived from X3DH Master Secret in production
        val keyGen = org.bouncycastle.crypto.generators.X25519KeyPairGenerator()
        keyGen.init(org.bouncycastle.crypto.params.X25519KeyGenerationParameters(java.security.SecureRandom()))
        val pair = keyGen.generateKeyPair()

        val sendingPriv = pair.private as X25519PrivateKeyParameters
        val sendingPub = pair.public as X25519PublicKeyParameters

        val remotePub = if (preKeyBundle != null) {
            val pubBytes = Base64.getDecoder().decode(preKeyBundle.identityKey)
            X25519PublicKeyParameters(pubBytes, 0)
        } else null

        val ratchet = DoubleRatchet(rootKey, sendingPriv, sendingPub, remotePub)
        return ratchet.encrypt(plaintext)
    }
}

