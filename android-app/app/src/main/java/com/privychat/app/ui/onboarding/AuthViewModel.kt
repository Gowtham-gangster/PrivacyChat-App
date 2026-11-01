package com.privychat.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privychat.app.core.preferences.UserPreferences
import com.privychat.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    sealed class AuthState {
        data object Idle : AuthState()
        data object Loading : AuthState()
        data class Error(val message: String) : AuthState()
        data class RegisterSuccess(val email: String, val phone: String) : AuthState()
        data object LoginSuccess : AuthState()
        data object VerificationSuccess : AuthState()
        data class PasswordResetRequested(val message: String) : AuthState()
        data class PasswordResetSuccess(val message: String) : AuthState()
        data class ResendSuccess(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun register(name: String, email: String, phone: String, pass: String, terms: Boolean) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()
        val normalizedPhone = com.privychat.app.core.util.PhoneNumberHelper.formatToE164(phone)
        val trimmedPass = pass

        if (trimmedName.length < 2) {
            _authState.value = AuthState.Error("Display name must be at least 2 characters")
            return
        }
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            _authState.value = AuthState.Error("Please enter a valid email address")
            return
        }
        if (normalizedPhone.isBlank()) {
            _authState.value = AuthState.Error("Phone number is required")
            return
        }
        if (!com.privychat.app.core.util.PhoneNumberHelper.isValidE164(normalizedPhone)) {
            _authState.value = AuthState.Error("Phone number must be valid E.164 format (e.g. +919705067505 or +12025550143)")
            return
        }
        if (trimmedPass.length < 8) {
            _authState.value = AuthState.Error("Password must be at least 8 characters")
            return
        }
        if (!terms) {
            _authState.value = AuthState.Error("You must accept the zero-knowledge terms & privacy policy")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.register(trimmedName, trimmedEmail, normalizedPhone, trimmedPass)
            result.onSuccess { user ->
                _authState.value = AuthState.RegisterSuccess(user.email, user.phoneNumber)
            }.onFailure { err ->
                _authState.value = AuthState.Error(err.message ?: "Registration failed")
            }
        }
    }

    fun login(identifier: String, pass: String) {
        if (identifier.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Please enter your credentials")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.login(identifier, pass)
            result.onSuccess {
                _authState.value = AuthState.LoginSuccess
            }.onFailure { err ->
                _authState.value = AuthState.Error(err.message ?: "Login failed")
            }
        }
    }

    fun verifyEmail(email: String, otp: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.verifyEmailOtp(email, otp)
            result.onSuccess {
                _authState.value = AuthState.VerificationSuccess
            }.onFailure { err ->
                _authState.value = AuthState.Error(err.message ?: "Verification failed")
            }
        }
    }

    fun resendVerification(email: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.resendVerification(email)
            result.onSuccess { msg ->
                _authState.value = AuthState.ResendSuccess(msg)
            }.onFailure { err ->
                _authState.value = AuthState.Error(err.message ?: "Failed to resend code")
            }
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank() || !email.contains("@")) {
            _authState.value = AuthState.Error("Please enter a valid email address")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.forgotPassword(email)
            result.onSuccess { msg ->
                _authState.value = AuthState.PasswordResetRequested(msg)
            }.onFailure { err ->
                _authState.value = AuthState.Error(err.message ?: "Failed to request password reset")
            }
        }
    }

    fun resetPassword(email: String, token: String, newPass: String) {
        if (token.isBlank() || newPass.length < 8) {
            _authState.value = AuthState.Error("Token is required and password must be at least 8 characters")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.resetPassword(email, token, newPass)
            result.onSuccess { msg ->
                _authState.value = AuthState.PasswordResetSuccess(msg)
            }.onFailure { err ->
                _authState.value = AuthState.Error(err.message ?: "Failed to reset password")
            }
        }
    }

    suspend fun checkSessionActive(): Boolean {
        return preferences.getAccessToken() != null
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
