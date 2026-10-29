package com.privychat.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(tableName = "crypto_sessions")
data class SessionEntity(
    @PrimaryKey
    val recipientUserId: UUID,
    val rootKeyBase64: String,
    val dhSendingPrivKeyBase64: String,
    val dhSendingPubKeyBase64: String,
    val dhReceivingPubKeyBase64: String?,
    val sendingChainKeyBase64: String?,
    val receivingChainKeyBase64: String?,
    val sendingMessageNumber: Long,
    val receivingMessageNumber: Long,
    val previousSendingChainLength: Long,
    val lastUpdated: Instant
)

