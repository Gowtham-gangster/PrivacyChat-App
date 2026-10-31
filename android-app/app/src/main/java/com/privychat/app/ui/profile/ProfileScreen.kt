package com.privychat.app.ui.profile

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.privychat.app.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Dialog state
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }

    // Dialog input state
    var newEmailInput by remember { mutableStateOf("") }
    var emailTokenInput by remember { mutableStateOf("") }
    var newPhoneInput by remember { mutableStateOf("") }
    var phoneOtpInput by remember { mutableStateOf("") }
    var deletePasswordInput by remember { mutableStateOf("") }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                viewModel.uploadAvatarBitmap(bitmap)
            } catch (e: Exception) {
                // Handled in VM
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.uploadAvatarBitmap(bitmap)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Identity", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEditProfile) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading && uiState.user == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val user = uiState.user
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Box with camera badge
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        val avatarUrl = user?.avatarUrl?.let {
                            if (it.startsWith("http")) it else BuildConfig.BASE_URL.removeSuffix("api/v1/") + it.removePrefix("/")
                        }

                        if (avatarUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable { showPhotoPickerSheet = true }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .clickable { showPhotoPickerSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (user?.displayName?.take(1) ?: "P").uppercase(),
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Edit badge
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { showPhotoPickerSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Change photo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (uiState.isUploadingAvatar) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uiState.uploadProgress },
                            modifier = Modifier.width(180.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sanitizing & Uploading...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display Name
                    Text(
                        text = user?.displayName ?: "PrivyChat User",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Account Status Badge
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Active End-to-End Account", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error banner if any
                    if (uiState.error != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                            }
                        }
                    }

                    // Card 1: Verified Identity Details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("VERIFIED CONTACT CHANNELS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Email Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Email Address", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(user?.email ?: "Not set", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    if (user?.pendingEmail != null) {
                                        Text("Pending change to: ${user.pendingEmail}", fontSize = 12.sp, color = Color(0xFFF59E0B))
                                    }
                                }
                                TextButton(onClick = {
                                    newEmailInput = ""
                                    emailTokenInput = ""
                                    showEmailDialog = true
                                }) {
                                    Text(if (user?.pendingEmail != null) "Verify" else "Change")
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Phone Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Mobile Number", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(user?.phoneNumber ?: "Not set", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    if (user?.pendingPhoneNumber != null) {
                                        Text("Pending change to: ${user.pendingPhoneNumber}", fontSize = 12.sp, color = Color(0xFFF59E0B))
                                    }
                                }
                                TextButton(onClick = {
                                    newPhoneInput = ""
                                    phoneOtpInput = ""
                                    showPhoneDialog = true
                                }) {
                                    Text(if (user?.pendingPhoneNumber != null) "Verify" else "Change")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Card 2: Security & Actions
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("SECURITY & ACCOUNT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Settings shortcut
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToSettings() }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Privacy & Security Settings", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text("Read receipts, avatar visibility, last seen", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Logout
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLogout() }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Log Out of Device", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Delete Account
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDeleteDialog = true }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Delete Account", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                                    Text("Irreversibly erase profile, cryptographic keys, and history", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Modal: Photo Choice
    if (showPhotoPickerSheet) {
        AlertDialog(
            onDismissRequest = { showPhotoPickerSheet = false },
            title = { Text("Profile Photo") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showPhotoPickerSheet = false
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Choose from Gallery / Photos")
                    }
                    TextButton(
                        onClick = {
                            showPhotoPickerSheet = false
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Take New Photo")
                    }
                    if (uiState.user?.avatarUrl != null) {
                        TextButton(
                            onClick = {
                                showPhotoPickerSheet = false
                                viewModel.removeAvatar()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoPickerSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Email Change Workflow
    if (showEmailDialog) {
        val isPending = uiState.isPendingEmailStep || uiState.user?.pendingEmail != null
        AlertDialog(
            onDismissRequest = {
                showEmailDialog = false
                viewModel.cancelPendingChanges()
            },
            title = { Text(if (isPending) "Verify New Email" else "Change Email Address") },
            text = {
                Column {
                    if (!isPending) {
                        Text("Enter the new email address you want to link. A single-use verification code will be sent.", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newEmailInput,
                            onValueChange = { newEmailInput = it },
                            label = { Text("New Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val pending = uiState.user?.pendingEmail ?: newEmailInput
                        Text("Enter the 6-digit verification code sent to $pending:", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = emailTokenInput,
                            onValueChange = { emailTokenInput = it },
                            label = { Text("Verification Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isPending) {
                            viewModel.requestEmailChange(newEmailInput)
                        } else {
                            viewModel.verifyEmailChange(emailTokenInput) {
                                showEmailDialog = false
                            }
                        }
                    }
                ) {
                    Text(if (!isPending) "Send Code" else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEmailDialog = false
                    viewModel.cancelPendingChanges()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Phone Change Workflow (Verified exclusively via email)
    if (showPhoneDialog) {
        val isPending = uiState.isPendingPhoneStep || uiState.user?.pendingPhoneNumber != null
        AlertDialog(
            onDismissRequest = {
                showPhoneDialog = false
                viewModel.cancelPendingChanges()
            },
            title = { Text(if (isPending) "Verify Mobile Number" else "Change Mobile Number") },
            text = {
                Column {
                    if (!isPending) {
                        Text(
                            "Enter your new mobile number in E.164 international format (e.g. +14155552671). Per PrivyChat policy, verification will be delivered to your registered email address:",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newPhoneInput,
                            onValueChange = { newPhoneInput = it },
                            label = { Text("New Mobile Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "A verification code has been sent to your registered email address.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Please check your inbox and enter the 6-digit verification code below:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = phoneOtpInput,
                            onValueChange = { phoneOtpInput = it },
                            label = { Text("Email Verification Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isPending) {
                            viewModel.requestPhoneChange(newPhoneInput)
                        } else {
                            viewModel.verifyPhoneChange(phoneOtpInput) {
                                showPhoneDialog = false
                            }
                        }
                    }
                ) {
                    Text(if (!isPending) "Send Code via Email" else "Confirm Change")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhoneDialog = false
                    viewModel.cancelPendingChanges()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Delete Account Confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete PrivyChat Account?", color = MaterialTheme.colorScheme.error) },
            text = {
                Column {
                    Text(
                        "This will permanently and irreversibly delete your account, your cryptographic identity keys, and all session records. Please enter your account password to confirm.",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = deletePasswordInput,
                        onValueChange = { deletePasswordInput = it },
                        label = { Text("Account Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(deletePasswordInput) {
                            showDeleteDialog = false
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

