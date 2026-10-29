package com.privychat.app.ui.call

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privychat.app.core.notification.NotificationHelper
import com.privychat.app.ui.MainActivity
import com.privychat.app.ui.theme.PrivyChatTheme
import java.util.UUID

class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and display over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val callId = intent.getStringExtra("callId")?.let(UUID::fromString) ?: UUID.randomUUID()
        val callerName = intent.getStringExtra("callerName") ?: "PrivyChat Contact"
        val callType = intent.getStringExtra("callType") ?: "Audio"

        setContent {
            PrivyChatTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 64.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = callerName.take(1).uppercase(),
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = callerName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Incoming $callType Call…",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 48.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Decline Call
                            IconButton(
                                onClick = {
                                    NotificationHelper.cancelCallNotification(this@IncomingCallActivity, callId)
                                    finish()
                                },
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White)
                            }

                            // Answer Call
                            IconButton(
                                onClick = {
                                    NotificationHelper.cancelCallNotification(this@IncomingCallActivity, callId)
                                    val mainIntent = Intent(this@IncomingCallActivity, MainActivity::class.java).apply {
                                        putExtra("callId", callId.toString())
                                        putExtra("answerCall", true)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    }
                                    startActivity(mainIntent)
                                    finish()
                                },
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Answer", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

