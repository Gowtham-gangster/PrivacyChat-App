package com.privychat.app.data.repository

import com.privychat.app.core.network.IceConfigResponseDto
import com.privychat.app.core.network.IceServerDto
import com.privychat.app.core.network.PrivyApiService
import com.privychat.app.core.network.StompWebSocketClient
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import retrofit2.Response

class CallRepositoryTest {

    @Mock
    private lateinit var apiService: PrivyApiService

    @Mock
    private lateinit var stompClient: StompWebSocketClient

    private lateinit var repository: CallRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = CallRepository(apiService, stompClient)
    }

    @Test
    fun getIceServers_success_returnsDynamicTurnServers() = runTest {
        val dto = IceConfigResponseDto(
            iceServers = listOf(
                IceServerDto(urls = listOf("stun:stun.l.google.com:19302")),
                IceServerDto(
                    urls = listOf("turn:turn.example.com:3478?transport=udp"),
                    username = "1730000000:user1",
                    credential = "mockTurnCredential"
                )
            ),
            ttl = 86400,
            turnConfigured = true
        )

        `when`(apiService.getIceServers()).thenReturn(Response.success(dto))

        val iceServers = repository.getIceServers()

        assertEquals(2, iceServers.size)
        assertEquals("stun:stun.l.google.com:19302", iceServers[0].uri)
        assertEquals("turn:turn.example.com:3478?transport=udp", iceServers[1].uri)
        assertEquals("1730000000:user1", iceServers[1].username)
        assertEquals("mockTurnCredential", iceServers[1].password)
    }

    @Test
    fun getIceServers_apiError_fallsBackToDefaultStun() = runTest {
        `when`(apiService.getIceServers()).thenReturn(Response.error(500, "Server Error".toResponseBody()))

        val iceServers = repository.getIceServers()

        assertEquals(1, iceServers.size)
        assertTrue(iceServers[0].uri.startsWith("stun:"))
        assertTrue(iceServers[0].username.isNullOrEmpty())
        assertTrue(iceServers[0].password.isNullOrEmpty())
    }

    @Test
    fun getIceServers_exception_fallsBackToDefaultStun() = runTest {
        `when`(apiService.getIceServers()).thenThrow(RuntimeException("Network failure"))

        val iceServers = repository.getIceServers()

        assertEquals(1, iceServers.size)
        assertTrue(iceServers[0].uri.startsWith("stun:"))
    }
}
