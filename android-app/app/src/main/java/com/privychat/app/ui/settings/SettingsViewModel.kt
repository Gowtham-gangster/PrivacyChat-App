package com.privychat.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privychat.app.core.network.PrivacySettingsDto
import com.privychat.app.core.network.PrivyApiService
import com.privychat.app.core.network.UpdateProfileDto
import com.privychat.app.core.preferences.UserPreferences
import com.privychat.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiService: PrivyApiService,
    private val authRepository: AuthRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _readReceiptsEnabled = MutableStateFlow(true)
    val readReceiptsEnabled: StateFlow<Boolean> = _readReceiptsEnabled

    private val _lastSeenVisibility = MutableStateFlow("EVERYONE")
    val lastSeenVisibility: StateFlow<String> = _lastSeenVisibility

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _displayName.value = preferences.getDisplayName() ?: "PrivyChat User"
            _phoneNumber.value = preferences.getPhoneNumber() ?: ""
            _email.value = preferences.getEmail() ?: ""
            _readReceiptsEnabled.value = preferences.isReadReceiptsEnabled()
        }
    }

    fun updateProfile(name: String) {
        viewModelScope.launch {
            apiService.updateProfile(UpdateProfileDto(displayName = name, avatarUrl = null))
            _displayName.value = name
        }
    }

    fun setReadReceipts(enabled: Boolean) {
        _readReceiptsEnabled.value = enabled
        viewModelScope.launch {
            preferences.setReadReceiptsEnabled(enabled)
            apiService.updatePrivacySettings(
                PrivacySettingsDto(
                    readReceiptsEnabled = enabled,
                    lastSeenVisibility = _lastSeenVisibility.value,
                    profilePhotoVisibility = "EVERYONE"
                )
            )
        }
    }

    fun setLastSeenVisibility(visibility: String) {
        _lastSeenVisibility.value = visibility
        viewModelScope.launch {
            apiService.updatePrivacySettings(
                PrivacySettingsDto(
                    readReceiptsEnabled = _readReceiptsEnabled.value,
                    lastSeenVisibility = visibility,
                    profilePhotoVisibility = "EVERYONE"
                )
            )
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    fun deleteAccount(password: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            apiService.deleteAccount(password)
            authRepository.logout()
            onDeleted()
        }
    }
}

