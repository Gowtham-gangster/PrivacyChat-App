package com.privychat.app.ui.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privychat.app.data.repository.ProfileRepository
import com.privychat.app.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isUploadingAvatar: Boolean = false,
    val uploadProgress: Float = 0f,
    val emailChangeMessage: String? = null,
    val phoneChangeMessage: String? = null,
    val isPendingEmailStep: Boolean = false,
    val isPendingPhoneStep: Boolean = false,
    val isAccountDeleted: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = profileRepository.getProfile()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = result.getOrNull(),
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to load profile"
                )
            }
        }
    }

    fun updateDisplayName(name: String, onDone: (() -> Unit)? = null) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Display name cannot be blank")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = profileRepository.updateDisplayName(name)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = result.getOrNull(),
                    error = null
                )
                onDone?.invoke()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to update display name"
                )
            }
        }
    }

    fun requestEmailChange(newEmail: String) {
        if (newEmail.isBlank() || !newEmail.contains("@")) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid email address")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = profileRepository.requestEmailChange(newEmail)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    emailChangeMessage = result.getOrNull() ?: "Verification code dispatched",
                    isPendingEmailStep = true,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to request email change"
                )
            }
        }
    }

    fun verifyEmailChange(token: String, onDone: (() -> Unit)? = null) {
        if (token.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter the verification code")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = profileRepository.verifyEmailChange(token)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = result.getOrNull(),
                    isPendingEmailStep = false,
                    emailChangeMessage = null,
                    error = null
                )
                onDone?.invoke()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Verification failed"
                )
            }
        }
    }

    fun requestPhoneChange(newPhone: String) {
        if (newPhone.isBlank() || !newPhone.startsWith("+")) {
            _uiState.value = _uiState.value.copy(error = "Enter a valid phone number with country code (e.g. +14155552671)")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = profileRepository.requestPhoneChange(newPhone)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    phoneChangeMessage = result.getOrNull() ?: "A verification code has been sent to your registered email address.",
                    isPendingPhoneStep = true,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to request phone change"
                )
            }
        }
    }

    fun verifyPhoneChange(otp: String, onDone: (() -> Unit)? = null) {
        if (otp.isBlank() || otp.length != 6) {
            _uiState.value = _uiState.value.copy(error = "Enter the 6-digit verification code received in your email")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = profileRepository.verifyPhoneChange(otp)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = result.getOrNull(),
                    isPendingPhoneStep = false,
                    phoneChangeMessage = null,
                    error = null
                )
                onDone?.invoke()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Invalid verification code"
                )
            }
        }
    }

    fun uploadAvatarBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingAvatar = true, uploadProgress = 0.2f, error = null)

            // Safe downscaling to max 1024x1024 to preserve memory & bandwidth
            val maxDim = 1024
            val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
            } else {
                bitmap
            }

            // Strips EXIF metadata including GPS coordinates by freshly re-compressing into PNG bytes
            _uiState.value = _uiState.value.copy(uploadProgress = 0.5f)
            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 95, stream)
            val bytes = stream.toByteArray()

            _uiState.value = _uiState.value.copy(uploadProgress = 0.8f)
            val result = profileRepository.uploadAvatar(bytes, "image/png")
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isUploadingAvatar = false,
                    uploadProgress = 1.0f,
                    user = result.getOrNull(),
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isUploadingAvatar = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to upload avatar"
                )
            }
        }
    }

    fun removeAvatar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = profileRepository.deleteAvatar()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = result.getOrNull(),
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to remove avatar"
                )
            }
        }
    }

    fun deleteAccount(password: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = profileRepository.deleteAccount(password)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAccountDeleted = true
                )
                onDeleted()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to delete account"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun cancelPendingChanges() {
        _uiState.value = _uiState.value.copy(
            isPendingEmailStep = false,
            isPendingPhoneStep = false,
            emailChangeMessage = null,
            phoneChangeMessage = null
        )
    }
}

