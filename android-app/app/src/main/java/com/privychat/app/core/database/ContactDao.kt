package com.privychat.app.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY isRegistered DESC, displayName ASC")
    fun getAllContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isRegistered = 1 ORDER BY displayName ASC")
    fun getRegisteredContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE userId = :userId")
    suspend fun getContactByUserId(userId: UUID): ContactEntity?

    @Query("SELECT * FROM contacts WHERE phoneNumber = :phone")
    suspend fun getContactByPhone(phone: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Query("UPDATE contacts SET isBlocked = :blocked WHERE userId = :userId")
    suspend fun updateBlockedStatus(userId: UUID, blocked: Boolean)
}

