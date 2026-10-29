package com.privychat.app.core.network

import com.privychat.app.BuildConfig
import com.privychat.app.core.preferences.UserPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    fun createOkHttpClient(preferences: UserPreferences): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = runBlocking { preferences.getAccessToken() }

            val requestBuilder = original.newBuilder()
            if (!token.isNullOrEmpty() && !original.url.encodedPath.contains("/auth/login") && !original.url.encodedPath.contains("/auth/register")) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            chain.proceed(requestBuilder.build())
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun createRetrofit(okHttpClient: OkHttpClient): PrivyApiService {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PrivyApiService::class.java)
    }
}

