package com.privychat.app.data.repository

import com.privychat.app.core.database.ContactDao
import com.privychat.app.core.database.ContactEntity
import com.privychat.app.core.network.ContactLookupRequestDto
import com.privychat.app.core.network.PrivyApiService
import com.privychat.app.core.network.SendInvitationDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ContactRepository(
    private val apiService: PrivyApiService,
    private val contactDao: ContactDao
) {

    fun getAllContactsFlow(): Flow<List<ContactEntity>> = contactDao.getAllContactsFlow()

    fun getRegisteredContactsFlow(): Flow<List<ContactEntity>> = contactDao.getRegisteredContactsFlow()

    suspend fun discoverContacts(phoneNumbers: List<String>): Result<List<ContactEntity>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.discoverContacts(ContactLookupRequestDto(phoneNumbers))
            if (response.isSuccessful && response.body() != null) {
                val entities = response.body()!!.map { dto ->
                    ContactEntity(
                        phoneNumber = dto.phoneNumber,
                        userId = dto.userId,
                        displayName = dto.displayName ?: dto.phoneNumber,
                        avatarUrl = dto.avatarUrl,
                        isRegistered = dto.registered
                    )
                }
                contactDao.insertAll(entities)
                Result.success(entities)
            } else {
                Result.failure(Exception("Failed to discover contacts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendInvitation(email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.sendInvitation(SendInvitationDto(email))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.status)
            } else {
                Result.failure(Exception("Failed to dispatch invitation email"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

