package com.privychat.app.core.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

class PrivyFirebaseMessagingService : FirebaseMessagingService() {

    private val tag = "PrivyFCMService"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(tag, "Refreshed FCM push token")
        // Token is uploaded via device registration or profile update
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val type = data["type"] ?: return

        when (type) {
            "INCOMING_CALL" -> {
                val callId = UUID.fromString(data["callId"])
                val callerId = UUID.fromString(data["callerId"])
                val callerName = data["callerName"] ?: "PrivyChat Contact"
                val callType = data["callType"] ?: "Audio"

                NotificationHelper.showIncomingCallNotification(
                    this,
                    callId,
                    callerId,
                    callerName,
                    callType
                )
            }
            "NEW_MESSAGE" -> {
                val conversationId = UUID.fromString(data["conversationId"])
                val senderName = data["senderName"] ?: "New Encrypted Message"
                val preview = data["preview"] ?: "Tap to open chat"

                NotificationHelper.showMessageNotification(
                    this,
                    conversationId,
                    senderName,
                    preview
                )
            }
        }
    }
}

