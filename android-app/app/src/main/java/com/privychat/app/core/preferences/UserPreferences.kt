package com.privychat.app.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "privychat_prefs")

class UserPreferences(private val context: Context) {

    private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
    private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
    private val KEY_PHONE_NUMBER = stringPreferencesKey("phone_number")
    private val KEY_EMAIL = stringPreferencesKey("email")
    private val KEY_READ_RECEIPTS = booleanPreferencesKey("read_receipts_enabled")

    suspend fun saveAuth(accessToken: String, refreshToken: String, userId: UUID, deviceId: UUID, displayName: String, phone: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_USER_ID] = userId.toString()
            prefs[KEY_DEVICE_ID] = deviceId.toString()
            prefs[KEY_DISPLAY_NAME] = displayName
            prefs[KEY_PHONE_NUMBER] = phone
            prefs[KEY_EMAIL] = email
        }
    }

    suspend fun getAccessToken(): String? = context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }.first()
    suspend fun getRefreshToken(): String? = context.dataStore.data.map { it[KEY_REFRESH_TOKEN] }.first()
    suspend fun getUserId(): UUID? = context.dataStore.data.map { it[KEY_USER_ID]?.let(UUID::fromString) }.first()
    suspend fun getDeviceId(): UUID? = context.dataStore.data.map { it[KEY_DEVICE_ID]?.let(UUID::fromString) }.first()
    suspend fun getDisplayName(): String? = context.dataStore.data.map { it[KEY_DISPLAY_NAME] }.first()
    suspend fun getPhoneNumber(): String? = context.dataStore.data.map { it[KEY_PHONE_NUMBER] }.first()
    suspend fun getEmail(): String? = context.dataStore.data.map { it[KEY_EMAIL] }.first()

    suspend fun isReadReceiptsEnabled(): Boolean = context.dataStore.data.map { it[KEY_READ_RECEIPTS] ?: true }.first()
    suspend fun setReadReceiptsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_READ_RECEIPTS] = enabled }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}

