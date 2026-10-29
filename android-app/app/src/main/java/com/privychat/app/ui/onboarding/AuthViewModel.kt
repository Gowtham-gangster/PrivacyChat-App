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
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun register(name: String, email: String, phone: String, pass: String, terms: Boolean) {
        if (!terms) {
            _authState.value = AuthState.Error("You must accept the privacy policy")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.register(name, email, phone, pass)
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

    fun verifyPhone(phone: String, otp: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.verifyPhoneOtp(phone, otp)
            result.onSuccess {
                _authState.value = AuthState.VerificationSuccess
            }.onFailure { err ->
                _authState.value = AuthState.Error(err.message ?: "Verification failed")
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

