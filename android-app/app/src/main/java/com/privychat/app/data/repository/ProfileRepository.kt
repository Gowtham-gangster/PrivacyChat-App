package com.privychat.app.data.repository

import com.privychat.app.core.network.*
import com.privychat.app.core.preferences.UserPreferences
import com.privychat.app.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class ProfileRepository(
    private val apiService: PrivyApiService,
    private val preferences: UserPreferences
) {

    suspend fun getProfile(): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                preferences.updateProfileLocally(
                    displayName = dto.displayName,
                    email = dto.email,
                    phone = dto.phoneNumber,
                    avatarUrl = dto.avatarUrl,
                    emailVerified = dto.emailVerified,
                    phoneVerified = dto.phoneVerified,
                    pendingEmail = dto.pendingEmail,
                    pendingPhone = dto.pendingPhoneNumber,
                    createdAt = dto.createdAt
                )
                Result.success(
                    User(
                        id = dto.id,
                        displayName = dto.displayName,
                        email = dto.email,
                        phoneNumber = dto.phoneNumber,
                        avatarUrl = dto.avatarUrl,
                        isEmailVerified = dto.emailVerified,
                        isPhoneVerified = dto.phoneVerified,
                        pendingEmail = dto.pendingEmail,
                        pendingPhoneNumber = dto.pendingPhoneNumber,
                        createdAt = dto.createdAt
                    )
                )
            } else {
                // Offline fallback from local preferences
                loadFromPreferences()
            }
        } catch (e: Exception) {
            // Network failure fallback
            loadFromPreferences()
        }
    }

    private suspend fun loadFromPreferences(): Result<User> {
        val userId = preferences.getUserId()
        return if (userId != null) {
            Result.success(
                User(
                    id = userId,
                    displayName = preferences.getDisplayName() ?: "Privy User",
                    email = preferences.getEmail() ?: "",
                    phoneNumber = preferences.getPhoneNumber() ?: "",
                    avatarUrl = preferences.getAvatarUrl(),
                    isEmailVerified = preferences.isEmailVerified(),
                    isPhoneVerified = preferences.isPhoneVerified(),
                    pendingEmail = preferences.getPendingEmail(),
                    pendingPhoneNumber = preferences.getPendingPhone(),
                    createdAt = preferences.getCreatedAt()
                )
            )
        } else {
            Result.failure(Exception("Offline and no local profile found"))
        }
    }

    suspend fun updateDisplayName(displayName: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.patchProfile(UpdateProfileDto(displayName = displayName.trim(), avatarUrl = null))
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                preferences.updateProfileLocally(
                    displayName = dto.displayName,
                    email = dto.email,
                    phone = dto.phoneNumber,
                    avatarUrl = dto.avatarUrl,
                    emailVerified = dto.emailVerified,
                    phoneVerified = dto.phoneVerified,
                    pendingEmail = dto.pendingEmail,
                    pendingPhone = dto.pendingPhoneNumber,
                    createdAt = dto.createdAt
                )
                Result.success(
                    User(
                        id = dto.id,
                        displayName = dto.displayName,
                        email = dto.email,
                        phoneNumber = dto.phoneNumber,
                        avatarUrl = dto.avatarUrl,
                        isEmailVerified = dto.emailVerified,
                        isPhoneVerified = dto.phoneVerified,
                        pendingEmail = dto.pendingEmail,
                        pendingPhoneNumber = dto.pendingPhoneNumber,
                        createdAt = dto.createdAt
                    )
                )
            } else {
                Result.failure(Exception("Failed to update profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestEmailChange(newEmail: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.requestEmailChange(EmailChangeRequestDto(newEmail.trim().lowercase()))
            if (response.isSuccessful) {
                preferences.updateProfileLocally(
                    displayName = null,
                    email = null,
                    phone = null,
                    avatarUrl = null,
                    emailVerified = null,
                    phoneVerified = null,
                    pendingEmail = newEmail.trim().lowercase(),
                    pendingPhone = null,
                    createdAt = null
                )
                Result.success(response.body()?.get("message") ?: "Verification code dispatched")
            } else {
                Result.failure(Exception("Failed to request email change: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyEmailChange(token: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.verifyEmailChange(VerifyEmailChangeRequestDto(token.trim()))
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                preferences.updateProfileLocally(
                    displayName = dto.displayName,
                    email = dto.email,
                    phone = dto.phoneNumber,
                    avatarUrl = dto.avatarUrl,
                    emailVerified = dto.emailVerified,
                    phoneVerified = dto.phoneVerified,
                    pendingEmail = null,
                    pendingPhone = null,
                    createdAt = dto.createdAt
                )
                Result.success(
                    User(
                        id = dto.id,
                        displayName = dto.displayName,
                        email = dto.email,
                        phoneNumber = dto.phoneNumber,
                        avatarUrl = dto.avatarUrl,
                        isEmailVerified = dto.emailVerified,
                        isPhoneVerified = dto.phoneVerified,
                        pendingEmail = null,
                        pendingPhoneNumber = null,
                        createdAt = dto.createdAt
                    )
                )
            } else {
                Result.failure(Exception("Email verification failed. Invalid or expired token."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestPhoneChange(newPhone: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.requestPhoneChange(PhoneChangeRequestDto(newPhone.trim()))
            if (response.isSuccessful) {
                preferences.updateProfileLocally(
                    displayName = null,
                    email = null,
                    phone = null,
                    avatarUrl = null,
                    emailVerified = null,
                    phoneVerified = null,
                    pendingEmail = null,
                    pendingPhone = newPhone.trim(),
                    createdAt = null
                )
                Result.success(response.body()?.get("message") ?: "OTP dispatched")
            } else {
                Result.failure(Exception("Failed to request phone change: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPhoneChange(code: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.verifyPhoneChange(VerifyPhoneChangeRequestDto(code.trim()))
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                preferences.updateProfileLocally(
                    displayName = dto.displayName,
                    email = dto.email,
                    phone = dto.phoneNumber,
                    avatarUrl = dto.avatarUrl,
                    emailVerified = dto.emailVerified,
                    phoneVerified = dto.phoneVerified,
                    pendingEmail = null,
                    pendingPhone = null,
                    createdAt = dto.createdAt
                )
                Result.success(
                    User(
                        id = dto.id,
                        displayName = dto.displayName,
                        email = dto.email,
                        phoneNumber = dto.phoneNumber,
                        avatarUrl = dto.avatarUrl,
                        isEmailVerified = dto.emailVerified,
                        isPhoneVerified = dto.phoneVerified,
                        pendingEmail = null,
                        pendingPhoneNumber = null,
                        createdAt = dto.createdAt
                    )
                )
            } else {
                Result.failure(Exception("Phone verification failed. Invalid or expired OTP."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val mediaType = mimeType.toMediaTypeOrNull()
            val reqFile = imageBytes.toRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("file", "avatar.png", reqFile)

            val response = apiService.uploadAvatar(part)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                preferences.updateProfileLocally(
                    displayName = dto.displayName,
                    email = dto.email,
                    phone = dto.phoneNumber,
                    avatarUrl = dto.avatarUrl,
                    emailVerified = dto.emailVerified,
                    phoneVerified = dto.phoneVerified,
                    pendingEmail = dto.pendingEmail,
                    pendingPhone = dto.pendingPhoneNumber,
                    createdAt = dto.createdAt
                )
                Result.success(
                    User(
                        id = dto.id,
                        displayName = dto.displayName,
                        email = dto.email,
                        phoneNumber = dto.phoneNumber,
                        avatarUrl = dto.avatarUrl,
                        isEmailVerified = dto.emailVerified,
                        isPhoneVerified = dto.phoneVerified,
                        pendingEmail = dto.pendingEmail,
                        pendingPhoneNumber = dto.pendingPhoneNumber,
                        createdAt = dto.createdAt
                    )
                )
            } else {
                Result.failure(Exception("Failed to upload avatar: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAvatar(): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteAvatar()
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                preferences.updateProfileLocally(
                    displayName = dto.displayName,
                    email = dto.email,
                    phone = dto.phoneNumber,
                    avatarUrl = null,
                    emailVerified = dto.emailVerified,
                    phoneVerified = dto.phoneVerified,
                    pendingEmail = dto.pendingEmail,
                    pendingPhone = dto.pendingPhoneNumber,
                    createdAt = dto.createdAt
                )
                Result.success(
                    User(
                        id = dto.id,
                        displayName = dto.displayName,
                        email = dto.email,
                        phoneNumber = dto.phoneNumber,
                        avatarUrl = null,
                        isEmailVerified = dto.emailVerified,
                        isPhoneVerified = dto.phoneVerified,
                        pendingEmail = dto.pendingEmail,
                        pendingPhoneNumber = dto.pendingPhoneNumber,
                        createdAt = dto.createdAt
                    )
                )
            } else {
                Result.failure(Exception("Failed to delete avatar: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteAccount(password)
            if (response.isSuccessful) {
                preferences.clear()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete account. Please verify password."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

