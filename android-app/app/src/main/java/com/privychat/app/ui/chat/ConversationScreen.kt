package com.privychat.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.privychat.app.domain.model.Message
import com.privychat.app.domain.model.MessageStatus
import com.privychat.app.ui.theme.TickDelivered
import com.privychat.app.ui.theme.TickRead
import com.privychat.app.ui.theme.TickSent
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    onNavigateBack: () -> Unit,
    onInitiateCall: (callId: UUID, name: String, type: String) -> Unit,
    onNavigateToSafetyNumber: (name: String, userId: UUID) -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = viewModel.participantName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = viewModel.participantName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "End-to-End Encrypted",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Safety Number Verification Button
                    IconButton(onClick = {
                        onNavigateToSafetyNumber(viewModel.participantName, viewModel.participantUserId)
                    }) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = "Safety Number")
                    }
                    // Voice Call Button
                    IconButton(onClick = {
                        onInitiateCall(UUID.randomUUID(), viewModel.participantName, "AUDIO")
                    }) {
                        Icon(Icons.Default.Phone, contentDescription = "Voice Call")
                    }
                    // Video Call Button
                    IconButton(onClick = {
                        onInitiateCall(UUID.randomUUID(), viewModel.participantName, "VIDEO")
                    }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                reverseLayout = true
            ) {
                items(messages.reversed(), key = { it.id }) { msg ->
                    MessageBubble(message = msg)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Bottom Compose Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showAttachmentSheet = true }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach File")
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Encrypted message…") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp)
                    )

                    if (inputText.isNotBlank()) {
                        IconButton(onClick = {
                            viewModel.sendTextMessage(inputText)
                            inputText = ""
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            if (isRecordingVoice) {
                                viewModel.stopVoiceRecordingAndSend(ByteArray(1024) { 0x01 })
                            } else {
                                viewModel.startVoiceRecording()
                            }
                        }) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Record Voice Note",
                                tint = if (isRecordingVoice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAttachmentSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachmentSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Encrypted Attachment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOption(icon = Icons.Default.Image, label = "Photo") {
                        showAttachmentSheet = false
                        viewModel.sendAttachment(ByteArray(2048) { 0x02 }, "photo.jpg", "image/jpeg", com.privychat.app.domain.model.AttachmentType.IMAGE)
                    }
                    AttachmentOption(icon = Icons.Default.Videocam, label = "Video") {
                        showAttachmentSheet = false
                        viewModel.sendAttachment(ByteArray(4096) { 0x03 }, "video.mp4", "video/mp4", com.privychat.app.domain.model.AttachmentType.VIDEO)
                    }
                    AttachmentOption(icon = Icons.Default.Description, label = "Document") {
                        showAttachmentSheet = false
                        viewModel.sendAttachment(ByteArray(1024) { 0x04 }, "document.pdf", "application/pdf", com.privychat.app.domain.model.AttachmentType.DOCUMENT)
                    }
                    AttachmentOption(icon = Icons.Default.Place, label = "Location") {
                        showAttachmentSheet = false
                        viewModel.sendTextMessage("LOCATION:37.7749,-122.4194")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isOut = message.isOutgoing
    val alignment = if (isOut) Alignment.End else Alignment.Start
    val bgColor = if (isOut) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isOut) 14.dp else 2.dp,
                bottomEnd = if (isOut) 2.dp else 14.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (message.isAttachment) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.attachmentType?.name ?: "ATTACHMENT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timestamp.toString().substring(11, 16),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isOut) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val (tickIcon, tickColor) = when (message.status) {
                            MessageStatus.SENDING -> Pair(Icons.Default.AccessTime, TickSent)
                            MessageStatus.SENT -> Pair(Icons.Default.Check, TickSent)
                            MessageStatus.DELIVERED -> Pair(Icons.Default.DoneAll, TickDelivered)
                            MessageStatus.READ -> Pair(Icons.Default.DoneAll, TickRead)
                            MessageStatus.FAILED -> Pair(Icons.Default.ErrorOutline, Color.Red)
                        }
                        Icon(
                            tickIcon,
                            contentDescription = message.status.name,
                            modifier = Modifier.size(13.dp),
                            tint = tickColor
                        )
                    }
                }
            }
        }
    }
}

