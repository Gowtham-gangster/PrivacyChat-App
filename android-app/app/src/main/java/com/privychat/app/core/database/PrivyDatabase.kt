package com.privychat.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ContactEntity::class,
        SessionEntity::class,
        PreKeyEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PrivyDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun sessionDao(): SessionDao

    companion object {
        private const val DB_NAME = "privychat_encrypted.db"

        @Volatile
        private var INSTANCE: PrivyDatabase? = null

        fun getInstance(context: Context, passphraseBytes: ByteArray = "PrivyChatEncryptedDBKey2026!".toByteArray()): PrivyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, passphraseBytes).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context, passphraseBytes: ByteArray): PrivyDatabase {
            val factory = SupportOpenHelperFactory(passphraseBytes)

            return Room.databaseBuilder(
                context.applicationContext,
                PrivyDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
