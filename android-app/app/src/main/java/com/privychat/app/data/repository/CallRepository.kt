package com.privychat.app.data.repository

import com.privychat.app.BuildConfig
import com.privychat.app.core.network.CallSignalDto
import com.privychat.app.core.network.PrivyApiService
import com.privychat.app.core.network.StompWebSocketClient
import com.privychat.app.domain.model.CallSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.webrtc.PeerConnection
import java.time.Instant
import java.util.UUID

class CallRepository(
    private val apiService: PrivyApiService,
    private val stompClient: StompWebSocketClient
) {

    suspend fun sendCallSignal(signal: CallSignalDto) = withContext(Dispatchers.IO) {
        stompClient.sendCallSignal(signal)
    }

    suspend fun getIceServers(): List<PeerConnection.IceServer> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getIceServers()
            if (response.isSuccessful && response.body() != null) {
                val config = response.body()!!
                val list = mutableListOf<PeerConnection.IceServer>()
                for (server in config.iceServers) {
                    val builder = PeerConnection.IceServer.builder(server.urls)
                    if (!server.username.isNullOrBlank() && !server.credential.isNullOrBlank()) {
                        builder.setUsername(server.username)
                        builder.setPassword(server.credential)
                    }
                    list.add(builder.createIceServer())
                }
                if (list.isNotEmpty()) {
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            // Network fallback to configured STUN server
        }
        listOf(PeerConnection.IceServer.builder(BuildConfig.STUN_SERVER).createIceServer())
    }

    suspend fun getCallHistory(): Result<List<CallSession>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCallHistory(50)
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!.map { dto ->
                    CallSession(
                        id = dto.id,
                        otherPartyName = dto.otherParty.displayName,
                        otherPartyAvatar = dto.otherParty.avatarUrl,
                        isOutgoing = dto.isOutgoing,
                        callType = dto.callType,
                        status = dto.status,
                        timestamp = Instant.parse(dto.startedAt),
                        durationSeconds = dto.durationSeconds
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Failed to fetch call history"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
