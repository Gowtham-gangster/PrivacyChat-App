package com.privychat.app.core.network

import android.util.Log
import com.google.gson.Gson
import com.privychat.app.BuildConfig
import com.privychat.app.core.preferences.UserPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import java.util.UUID

class StompWebSocketClient(
    private val preferences: UserPreferences,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson = Gson()
) {
    private val tag = "StompWebSocketClient"
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    var onMessageReceived: ((MessageResponseDto) -> Unit)? = null
    var onReceiptReceived: ((Map<String, Any>) -> Unit)? = null
    var onCallSignalReceived: ((CallSignalDto) -> Unit)? = null

    enum class ConnectionState {
        CONNECTING, CONNECTED, DISCONNECTED
    }

    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED || _connectionState.value == ConnectionState.CONNECTING) return

        _connectionState.value = ConnectionState.CONNECTING
        scope.launch {
            val token = preferences.getAccessToken() ?: return@launch
            val userId = preferences.getUserId() ?: return@launch

            val request = Request.Builder()
                .url(BuildConfig.WS_URL)
                .build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(tag, "WebSocket TCP opened. Sending STOMP CONNECT frame...")
                    val connectFrame = "CONNECT\naccept-version:1.2\nheart-beat:10000,10000\nAuthorization:Bearer $token\n\n\u0000"
                    ws.send(connectFrame)
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleStompFrame(text, userId)
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Log.d(tag, "WebSocket closed: $reason")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    scheduleReconnect()
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.w(tag, "WebSocket failure: ${t.message}")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    scheduleReconnect()
                }
            })
        }
    }

    private fun handleStompFrame(frameText: String, userId: UUID) {
        val lines = frameText.split("\n")
        if (lines.isEmpty()) return

        val command = lines[0].trim()
        if (command == "CONNECTED") {
            Log.d(tag, "STOMP session connected. Subscribing to topics...")
            _connectionState.value = ConnectionState.CONNECTED

            // Subscribe to personal messages, receipts, and calls
            subscribe("/topic/user.$userId.messages", "sub-messages")
            subscribe("/topic/user.$userId.receipts", "sub-receipts")
            subscribe("/topic/user.$userId.call", "sub-calls")
        } else if (command == "MESSAGE") {
            // Find body (separated by blank line)
            val nullIdx = frameText.indexOf('\u0000')
            val bodyStart = frameText.indexOf("\n\n")
            if (bodyStart != -1) {
                val end = if (nullIdx != -1) nullIdx else frameText.length
                val body = frameText.substring(bodyStart + 2, end).trim()

                // Check destination
                for (line in lines) {
                    if (line.startsWith("destination:")) {
                        val dest = line.substring("destination:".length).trim()
                        when {
                            dest.endsWith(".messages") -> {
                                val msg = gson.fromJson(body, MessageResponseDto::class.java)
                                onMessageReceived?.invoke(msg)
                            }
                            dest.endsWith(".receipts") -> {
                                @Suppress("UNCHECKED_CAST")
                                val receipt = gson.fromJson(body, Map::class.java) as Map<String, Any>
                                onReceiptReceived?.invoke(receipt)
                            }
                            dest.endsWith(".call") -> {
                                val signal = gson.fromJson(body, CallSignalDto::class.java)
                                onCallSignalReceived?.invoke(signal)
                            }
                        }
                    }
                }
            }
        }
    }

    fun subscribe(destination: String, subId: String) {
        val frame = "SUBSCRIBE\nid:$subId\ndestination:$destination\nack:auto\n\n\u0000"
        webSocket?.send(frame)
    }

    fun sendMessage(request: SendMessageDto) {
        val json = gson.toJson(request)
        val frame = "SEND\ndestination:/app/chat.send\ncontent-type:application/json\n\n$json\u0000"
        webSocket?.send(frame)
    }

    fun sendCallSignal(signal: CallSignalDto) {
        val json = gson.toJson(signal)
        val frame = "SEND\ndestination:/app/call.signal\ncontent-type:application/json\n\n$json\u0000"
        webSocket?.send(frame)
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(5000)
            connect()
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}

