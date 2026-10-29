package com.privychat.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.privychat.app.core.crypto.KeystoreHelper
import dagger.hilt.android.HiltAndroidApp
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

@HiltAndroidApp
class PrivyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Bouncy Castle security provider for Curve25519 & Ed25519
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())

        // 2. Initialize Android Keystore master encryption key
        KeystoreHelper.initMasterKey()

        // 4. Create Notification Channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // High-priority WebRTC Calls Channel
            val callSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val callChannel = NotificationChannel(
                CHANNEL_CALLS_ID,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice and video call notifications"
                setSound(callSound, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Standard Encrypted Messages Channel
            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                "Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "New encrypted message notifications"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(callChannel)
            notificationManager.createNotificationChannel(messageChannel)
        }
    }

    companion object {
        const val CHANNEL_CALLS_ID = "privy_calls_channel"
        const val CHANNEL_MESSAGES_ID = "privy_messages_channel"
    }
}

