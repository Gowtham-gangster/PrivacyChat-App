package com.privychat.app.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privychat.app.core.database.ContactEntity
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    onNavigateBack: () -> Unit,
    onStartConversation: (convId: UUID, name: String, userId: UUID) -> Unit
) {
    val contacts by viewModel.allContacts.collectAsState()
    val inviteStatus by viewModel.inviteStatus.collectAsState()

    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteEmail by remember { mutableStateOf("") }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var manualPhone by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(inviteStatus) {
        inviteStatus?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInviteStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Contacts & Invitations", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showManualAddDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Invite Friend Card Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { showInviteDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Mail,
                        contentDescription = "Invite",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Invite a Friend to PrivyChat",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Send a secure invitation email with verified download instructions",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("No Contacts Discovered", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap the invite button above to invite your contacts via email or add a phone number manually.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(contacts, key = { it.phoneNumber }) { contact ->
                        ContactRow(
                            contact = contact,
                            onChat = {
                                if (contact.userId != null) {
                                    viewModel.startConversation(contact.userId) { convId ->
                                        onStartConversation(convId, contact.displayName, contact.userId)
                                    }
                                }
                            },
                            onInvite = {
                                showInviteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Friend to PrivyChat") },
            text = {
                Column {
                    Text("Enter your friend's email address. We'll send an invitation with a private download link:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Friend's Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (inviteEmail.isNotBlank()) {
                        viewModel.sendEmailInvitation(inviteEmail)
                        inviteEmail = ""
                        showInviteDialog = false
                    }
                }) {
                    Text("Send Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showManualAddDialog) {
        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text("Look Up Phone Number") },
            text = {
                Column {
                    Text("Enter mobile number with country code to check if registered on PrivyChat:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualPhone,
                        onValueChange = { manualPhone = it },
                        label = { Text("Phone Number (+E.164)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (manualPhone.isNotBlank()) {
                        viewModel.discoverContacts(listOf(manualPhone.trim()))
                        manualPhone = ""
                        showManualAddDialog = false
                    }
                }) {
                    Text("Look Up")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ContactRow(
    contact: ContactEntity,
    onChat: () -> Unit,
    onInvite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (contact.isRegistered) onChat() else onInvite() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (contact.isRegistered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.displayName.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = if (contact.isRegistered) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = contact.phoneNumber,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (contact.isRegistered) {
            Button(
                onClick = onChat,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Chat", fontSize = 13.sp)
            }
        } else {
            OutlinedButton(
                onClick = onInvite,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Invite", fontSize = 13.sp)
            }
        }
    }
}

