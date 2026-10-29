package com.privychat.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val displayName by viewModel.displayName.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val email by viewModel.email.collectAsState()
    val readReceipts by viewModel.readReceiptsEnabled.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Card Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(1).uppercase().ifEmpty { "U" },
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName.ifEmpty { "PrivyChat User" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (phoneNumber.isNotEmpty()) {
                            Text(
                                text = phoneNumber,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                        if (email.isNotEmpty()) {
                            Text(
                                text = email,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = {
                        editedName = displayName
                        showEditNameDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Privacy & Security Section
            SettingsSectionHeader(title = "Privacy & Security")

            SettingsSwitchItem(
                icon = Icons.Default.DoneAll,
                title = "Read Receipts",
                subtitle = "If turned off, you won't send or receive read receipts",
                checked = readReceipts,
                onCheckedChange = { viewModel.setReadReceipts(it) }
            )

            SettingsItem(
                icon = Icons.Default.Lock,
                title = "End-to-End Encryption",
                subtitle = "Messages and calls are secured with Double Ratchet & X3DH"
            )

            SettingsItem(
                icon = Icons.Default.Security,
                title = "Local Encrypted Database",
                subtitle = "Protected by SQLCipher and Android Keystore master keys"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account Section
            SettingsSectionHeader(title = "Account Management")

            SettingsItem(
                icon = Icons.Default.ExitToApp,
                title = "Log Out",
                subtitle = "Sign out from this device",
                onClick = { showLogoutDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = "Delete Account",
                subtitle = "Permanently remove your account, cryptographic keys, and data",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { showDeleteAccountDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Display Name") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editedName.isNotBlank()) {
                        viewModel.updateProfile(editedName.trim())
                    }
                    showEditNameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out? Your local cryptographic keys will remain secured on this device.") },
            confirmButton = {
                Button(onClick = {
                    showLogoutDialog = false
                    viewModel.logout(onLoggedOut)
                }) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Account", color = MaterialTheme.colorScheme.error) },
            text = {
                Column {
                    Text(
                        "This action is permanent and cannot be undone. All your identity keys, messages, and contact associations will be erased from PrivyChat servers.",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        label = { Text("Confirm your password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deletePassword.isNotBlank()) {
                            showDeleteAccountDialog = false
                            viewModel.deleteAccount(deletePassword, onLoggedOut)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = titleColor, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            if (subtitle != null) {
                Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            if (subtitle != null) {
                Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

