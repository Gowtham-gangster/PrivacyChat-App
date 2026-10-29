package com.privychat.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.privychat.app.ui.navigation.PrivyNavGraph
import com.privychat.app.ui.navigation.Screen
import com.privychat.app.ui.theme.PrivyChatTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PrivyChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(intent) {
                        handleNotificationIntent(intent, navController)
                    }

                    PrivyNavGraph(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?, navController: androidx.navigation.NavHostController) {
        if (intent == null) return
        val conversationIdStr = intent.getStringExtra("conversationId")
        val senderName = intent.getStringExtra("senderName") ?: "Contact"
        val senderIdStr = intent.getStringExtra("senderId")

        if (!conversationIdStr.isNullOrBlank() && !senderIdStr.isNullOrBlank()) {
            try {
                val convId = UUID.fromString(conversationIdStr)
                val senderId = UUID.fromString(senderIdStr)
                navController.navigate(Screen.Conversation.createRoute(convId, senderName, senderId))
            } catch (_: Exception) {
                // Ignore malformed extra UUIDs
            }
        }
    }
}

