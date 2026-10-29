package com.privychat.app.di

import android.content.Context
import com.privychat.app.core.database.*
import com.privychat.app.core.network.NetworkClient
import com.privychat.app.core.network.PrivyApiService
import com.privychat.app.core.network.StompWebSocketClient
import com.privychat.app.core.preferences.UserPreferences
import com.privychat.app.data.repository.AuthRepository
import com.privychat.app.data.repository.CallRepository
import com.privychat.app.data.repository.ContactRepository
import com.privychat.app.data.repository.ConversationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    @Provides
    @Singleton
    fun providePrivyDatabase(@ApplicationContext context: Context): PrivyDatabase {
        return PrivyDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideConversationDao(db: PrivyDatabase): ConversationDao {
        return db.conversationDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(db: PrivyDatabase): MessageDao {
        return db.messageDao()
    }

    @Provides
    @Singleton
    fun provideContactDao(db: PrivyDatabase): ContactDao {
        return db.contactDao()
    }

    @Provides
    @Singleton
    fun provideSessionDao(db: PrivyDatabase): SessionDao {
        return db.sessionDao()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(preferences: UserPreferences): OkHttpClient {
        return NetworkClient.createOkHttpClient(preferences)
    }

    @Provides
    @Singleton
    fun providePrivyApiService(okHttpClient: OkHttpClient): PrivyApiService {
        return NetworkClient.createRetrofit(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideStompWebSocketClient(
        preferences: UserPreferences,
        okHttpClient: OkHttpClient
    ): StompWebSocketClient {
        return StompWebSocketClient(preferences, okHttpClient)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: PrivyApiService,
        preferences: UserPreferences
    ): AuthRepository {
        return AuthRepository(apiService, preferences)
    }

    @Provides
    @Singleton
    fun provideConversationRepository(
        apiService: PrivyApiService,
        stompClient: StompWebSocketClient,
        conversationDao: ConversationDao,
        messageDao: MessageDao,
        sessionDao: SessionDao,
        preferences: UserPreferences
    ): ConversationRepository {
        return ConversationRepository(
            apiService = apiService,
            stompClient = stompClient,
            conversationDao = conversationDao,
            messageDao = messageDao,
            sessionDao = sessionDao,
            preferences = preferences
        )
    }

    @Provides
    @Singleton
    fun provideContactRepository(
        apiService: PrivyApiService,
        contactDao: ContactDao
    ): ContactRepository {
        return ContactRepository(apiService, contactDao)
    }

    @Provides
    @Singleton
    fun provideCallRepository(
        apiService: PrivyApiService,
        stompClient: StompWebSocketClient
    ): CallRepository {
        return CallRepository(apiService, stompClient)
    }
}
