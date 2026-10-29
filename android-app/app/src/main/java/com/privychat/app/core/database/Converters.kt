package com.privychat.app.core.database

import androidx.room.TypeConverter
import java.time.Instant
import java.util.UUID

class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }

    @TypeConverter
    fun fromUuid(uuid: UUID?): String? {
        return uuid?.toString()
    }

    @TypeConverter
    fun toUuid(value: String?): UUID? {
        return value?.let { UUID.fromString(it) }
    }
}

