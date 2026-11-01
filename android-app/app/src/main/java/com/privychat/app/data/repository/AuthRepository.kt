package com.privychat.app.data.repository

import com.privychat.app.core.crypto.X3DH
import com.privychat.app.core.network.*
import com.privychat.app.core.preferences.UserPreferences
import com.privychat.app.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64
import java.util.UUID

class AuthRepository(
    private val apiService: PrivyApiService,
    private val preferences: UserPreferences
) {

    suspend fun register(
        displayName: String,
        email: String,
        phone: String,
        pass: String
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val reqDto = RegisterRequestDto(displayName, email, phone, pass, true)
            val maskedPhone = if (phone.length > 5) phone.take(3) + "****" + phone.takeLast(2) else "****"
            android.util.Log.d(
                "AuthRepository",
                "Register request fields: [displayName: present=${displayName.isNotBlank()}, email: present=${email.isNotBlank()}, phoneNumber: present=${phone.isNotBlank()} ($maskedPhone), password: present=${pass.isNotBlank()} (len=${pass.length}), termsAccepted=${reqDto.termsAccepted}]"
            )
            val response = apiService.register(reqDto)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                Result.success(
                    User(
                        dto.id,
                        dto.displayName,
                        dto.email,
                        dto.phoneNumber,
                        dto.avatarUrl,
                        dto.emailVerified,
                        dto.phoneVerified
                    )
                )
            } else {
                val errorMessage = ApiErrorParser.parseError(response, "Registration failed")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyEmailOtp(email: String, otp: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.verifyEmailOtp(OtpVerificationDto(email, otp))
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errorMessage = ApiErrorParser.parseError(response, "Invalid verification code")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendVerification(email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.resendVerification(ResendVerificationRequestDto(email.trim().lowercase()))
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") ?: "Verification code resent")
            } else {
                val errorMessage = ApiErrorParser.parseError(response, "Failed to resend verification code")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.forgotPassword(ForgotPasswordRequestDto(email.trim().lowercase()))
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") ?: "Password reset instructions dispatched via email")
            } else {
                val errorMessage = ApiErrorParser.parseError(response, "Failed to request password reset")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String, token: String, newPass: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.resetPassword(ResetPasswordRequestDto(email.trim().lowercase(), token.trim(), newPass))
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") ?: "Password successfully reset")
            } else {
                val errorMessage = ApiErrorParser.parseError(response, "Failed to reset password")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(identifier: String, pass: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(LoginRequestDto(identifier, pass, "Android Native Client"))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                preferences.saveAuth(
                    auth.accessToken,
                    auth.refreshToken,
                    auth.user.id,
                    auth.deviceId,
                    auth.user.displayName,
                    auth.user.phoneNumber,
                    auth.user.email
                )

                // Publish initial cryptographic prekeys
                publishInitialKeys(auth.deviceId)

                Result.success(
                    User(
                        auth.user.id,
                        auth.user.displayName,
                        auth.user.email,
                        auth.user.phoneNumber,
                        auth.user.avatarUrl,
                        auth.user.emailVerified,
                        auth.user.phoneVerified
                    )
                )
            } else {
                val errorMessage = ApiErrorParser.parseError(response, "Invalid login credentials")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun publishInitialKeys(deviceId: UUID) {
        val bundle = X3DH.generateInitialKeyBundle(50)
        val idKeyB64 = Base64.getEncoder().encodeToString(bundle.identityKeyPair.second.encoded)
        val spkDto = SignedPreKeyDto(
            1,
            Base64.getEncoder().encodeToString(bundle.signedPreKeyPair.second.encoded),
            Base64.getEncoder().encodeToString(bundle.signedPreKeySignature)
        )
        val otpkDtos = bundle.oneTimePreKeyPairs.mapIndexed { idx, pair ->
            OneTimePreKeyDto(idx + 1, Base64.getEncoder().encodeToString(pair.second.encoded))
        }

        apiService.publishKeys(
            PublishKeysDto(
                deviceId = deviceId,
                identityKey = idKeyB64,
                registrationId = 1001,
                signedPreKey = spkDto,
                oneTimePreKeys = otpkDtos
            )
        )
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            apiService.logout()
        } catch (ignored: Exception) {}
        preferences.clear()
    }
}

