package com.privychat.app.core.database

import androidx.room.*
import java.util.UUID

@Dao
interface SessionDao {

    @Query("SELECT * FROM crypto_sessions WHERE recipientUserId = :userId")
    suspend fun getSessionByUserId(userId: UUID): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: SessionEntity)

    @Query("DELETE FROM crypto_sessions WHERE recipientUserId = :userId")
    suspend fun deleteSession(userId: UUID)

    @Query("DELETE FROM crypto_sessions")
    suspend fun clearAllSessions()
}

