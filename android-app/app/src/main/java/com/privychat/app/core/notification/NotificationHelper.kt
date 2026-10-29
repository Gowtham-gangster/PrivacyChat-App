package com.privychat.app.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.privychat.app.PrivyApplication
import com.privychat.app.ui.MainActivity
import com.privychat.app.ui.call.IncomingCallActivity
import java.util.UUID

object NotificationHelper {

    fun showIncomingCallNotification(
        context: Context,
        callId: UUID,
        callerId: UUID,
        callerName: String,
        callType: String
    ) {
        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
            putExtra("callId", callId.toString())
            putExtra("callerId", callerId.toString())
            putExtra("callerName", callerName)
            putExtra("callType", callType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PrivyApplication.CHANNEL_CALLS_ID)
            .setContentTitle("Incoming $callType Call")
            .setContentText("$callerName is calling you securely…")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                android.R.drawable.ic_menu_call,
                "Answer",
                fullScreenPendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(callId.hashCode(), notification)
    }

    fun showMessageNotification(
        context: Context,
        conversationId: UUID,
        senderName: String,
        messagePreview: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("conversationId", conversationId.toString())
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PrivyApplication.CHANNEL_MESSAGES_ID)
            .setContentTitle(senderName)
            .setContentText(messagePreview)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(conversationId.hashCode(), notification)
    }

    fun cancelCallNotification(context: Context, callId: UUID) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(callId.hashCode())
    }
}

