package com.privychat.app.data.repository

import com.privychat.app.core.network.*
import com.privychat.app.core.preferences.UserPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import retrofit2.Response
import java.util.UUID

class ProfileRepositoryTest {

    private lateinit var apiService: PrivyApiService
    private lateinit var preferences: UserPreferences
    private lateinit var repository: ProfileRepository
    private val userId = UUID.randomUUID()

    @Before
    fun setUp() {
        apiService = mock(PrivyApiService::class.java)
        preferences = mock(UserPreferences::class.java)
        repository = ProfileRepository(apiService, preferences)
    }

    @Test
    fun getProfile_success_updatesPreferences() = runBlocking {
        val dto = UserProfileDto(
            id = userId,
            displayName = "Alice",
            email = "alice@example.com",
            phoneNumber = "+14155550100",
            avatarUrl = "/api/v1/profile/avatar/$userId",
            emailVerified = true,
            phoneVerified = true,
            pendingEmail = null,
            pendingPhoneNumber = null,
            createdAt = "2026-10-29T10:00:00Z"
        )
        `when`(apiService.getProfile()).thenReturn(Response.success(dto))

        val result = repository.getProfile()

        assertTrue(result.isSuccess)
        val user = result.getOrNull()!!
        assertEquals("Alice", user.displayName)
        assertEquals("alice@example.com", user.email)
        assertTrue(user.isEmailVerified)

        verify(preferences).updateProfileLocally(
            displayName = "Alice",
            email = "alice@example.com",
            phone = "+14155550100",
            avatarUrl = "/api/v1/profile/avatar/$userId",
            emailVerified = true,
            phoneVerified = true,
            pendingEmail = null,
            pendingPhone = null,
            createdAt = "2026-10-29T10:00:00Z"
        )
    }

    @Test
    fun getProfile_networkFailure_fallsBackToPreferences() = runBlocking {
        `when`(apiService.getProfile()).thenReturn(
            Response.error(500, "Server Error".toResponseBody())
        )
        `when`(preferences.getUserId()).thenReturn(userId)
        `when`(preferences.getDisplayName()).thenReturn("Offline Alice")
        `when`(preferences.getEmail()).thenReturn("offline@example.com")
        `when`(preferences.getPhoneNumber()).thenReturn("+14155550199")
        `when`(preferences.isEmailVerified()).thenReturn(true)
        `when`(preferences.isPhoneVerified()).thenReturn(true)

        val result = repository.getProfile()

        assertTrue(result.isSuccess)
        val user = result.getOrNull()!!
        assertEquals("Offline Alice", user.displayName)
        assertEquals("offline@example.com", user.email)
    }

    @Test
    fun updateDisplayName_success_updatesPreferences() = runBlocking {
        val expectedReq = UpdateProfileDto(displayName = "Alice Renamed", avatarUrl = null)
        val dto = UserProfileDto(
            id = userId,
            displayName = "Alice Renamed",
            email = "alice@example.com",
            phoneNumber = "+14155550100",
            avatarUrl = null,
            emailVerified = true,
            phoneVerified = true
        )
        `when`(apiService.patchProfile(expectedReq)).thenReturn(Response.success(dto))

        val result = repository.updateDisplayName("Alice Renamed")

        assertTrue(result.isSuccess)
        assertEquals("Alice Renamed", result.getOrNull()?.displayName)
        verify(preferences).updateProfileLocally(
            displayName = "Alice Renamed",
            email = "alice@example.com",
            phone = "+14155550100",
            avatarUrl = null,
            emailVerified = true,
            phoneVerified = true,
            pendingEmail = null,
            pendingPhone = null,
            createdAt = null
        )
    }

    @Test
    fun verifyEmailChange_success_clearsPending() = runBlocking {
        val expectedReq = VerifyEmailChangeRequestDto("654321")
        val dto = UserProfileDto(
            id = userId,
            displayName = "Alice",
            email = "newalice@example.com",
            phoneNumber = "+14155550100",
            avatarUrl = null,
            emailVerified = true,
            phoneVerified = true
        )
        `when`(apiService.verifyEmailChange(expectedReq)).thenReturn(Response.success(dto))

        val result = repository.verifyEmailChange("654321")

        assertTrue(result.isSuccess)
        assertEquals("newalice@example.com", result.getOrNull()?.email)
        verify(preferences).updateProfileLocally(
            displayName = "Alice",
            email = "newalice@example.com",
            phone = "+14155550100",
            avatarUrl = null,
            emailVerified = true,
            phoneVerified = true,
            pendingEmail = null,
            pendingPhone = null,
            createdAt = null
        )
    }

    @Test
    fun verifyPhoneChange_success_updatesPhone() = runBlocking {
        val expectedReq = VerifyPhoneChangeRequestDto("112233")
        val dto = UserProfileDto(
            id = userId,
            displayName = "Alice",
            email = "alice@example.com",
            phoneNumber = "+14155550999",
            avatarUrl = null,
            emailVerified = true,
            phoneVerified = true
        )
        `when`(apiService.verifyPhoneChange(expectedReq)).thenReturn(Response.success(dto))

        val result = repository.verifyPhoneChange("112233")

        assertTrue(result.isSuccess)
        assertEquals("+14155550999", result.getOrNull()?.phoneNumber)
    }
}
