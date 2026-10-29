package com.privychat.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_prekeys")
data class PreKeyEntity(
    @PrimaryKey
    val keyId: Int,
    val type: String, // SIGNED_PREKEY, ONE_TIME_PREKEY
    val privateKeyBase64: String,
    val publicKeyBase64: String,
    val isConsumed: Boolean = false
)

