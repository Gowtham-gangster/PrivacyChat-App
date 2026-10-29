package com.privychat.app.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privychat.app.core.webrtc.WebRtcClient

@Composable
fun CallScreen(
    viewModel: CallViewModel,
    onCallEnded: () -> Unit
) {
    val callState by viewModel.callState.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isCameraEnabled by viewModel.isCameraEnabled.collectAsState()

    LaunchedEffect(callState) {
        if (callState == WebRtcClient.CallState.TERMINATED) {
            onCallEnded()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Section: Contact Info & Call Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.otherPartyName.take(1).uppercase(),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = viewModel.otherPartyName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                val stateText = when (callState) {
                    WebRtcClient.CallState.DIALING -> "Dialing…"
                    WebRtcClient.CallState.RINGING -> "Ringing…"
                    WebRtcClient.CallState.CONNECTING -> "Connecting WebRTC SRTP…"
                    WebRtcClient.CallState.CONNECTED -> "End-to-End Encrypted Call"
                    else -> "Call Terminated"
                }

                Text(
                    text = stateText,
                    fontSize = 15.sp,
                    color = if (callState == WebRtcClient.CallState.CONNECTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bottom Section: Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Button
                    IconButton(
                        onClick = { viewModel.toggleMute() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (isMuted) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Video Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleCamera() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (!isCameraEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Camera Toggle",
                            tint = if (isCameraEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // End Call Button
                    IconButton(
                        onClick = { viewModel.endCall() },
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

