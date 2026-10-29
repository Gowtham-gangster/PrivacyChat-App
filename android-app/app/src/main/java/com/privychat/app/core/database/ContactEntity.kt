package com.privychat.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val phoneNumber: String,
    val userId: UUID?,
    val displayName: String,
    val avatarUrl: String?,
    val isRegistered: Boolean,
    val safetyNumber: String? = null,
    val isBlocked: Boolean = false
)

