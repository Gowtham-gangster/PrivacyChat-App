package com.privychat.app.core.network

import com.privychat.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VariantNetworkConfigTest {

    @Test
    fun verifyNetworkConfigForActiveVariant() {
        when (BuildConfig.FLAVOR) {
            "prod" -> {
                assertTrue("Production BASE_URL must use HTTPS", BuildConfig.BASE_URL.startsWith("https://"))
                assertTrue("Production WS_URL must use WSS", BuildConfig.WS_URL.startsWith("wss://"))
                assertEquals("https://d3fv2w7uf8w2u4.cloudfront.net/api/v1/", BuildConfig.BASE_URL)
                assertEquals("wss://d3fv2w7uf8w2u4.cloudfront.net/ws", BuildConfig.WS_URL)
                assertTrue("REST endpoint must end with api/v1/", BuildConfig.BASE_URL.endsWith("/api/v1/"))
                assertTrue("WebSocket endpoint must end with /ws", BuildConfig.WS_URL.endsWith("/ws"))
            }
            "dev" -> {
                assertTrue("Development BASE_URL must use HTTP for local emulator", BuildConfig.BASE_URL.startsWith("http://"))
                assertTrue("Development WS_URL must use WS for local emulator", BuildConfig.WS_URL.startsWith("ws://"))
                assertEquals("http://10.0.2.2:8080/api/v1/", BuildConfig.BASE_URL)
                assertEquals("ws://10.0.2.2:8080/ws", BuildConfig.WS_URL)
            }
            else -> {
                throw IllegalStateException("Unexpected flavor: ${BuildConfig.FLAVOR}")
            }
        }
    }

    @Test
    fun verifyAvatarUrlResolutionMatchesBaseUrlProtocol() {
        val avatarPath = "avatars/user-123.png"
        val resolved = if (avatarPath.startsWith("http")) {
            avatarPath
        } else {
            BuildConfig.BASE_URL.removeSuffix("api/v1/") + avatarPath.removePrefix("/")
        }

        if (BuildConfig.FLAVOR == "prod") {
            assertEquals("https://d3fv2w7uf8w2u4.cloudfront.net/avatars/user-123.png", resolved)
            assertTrue("Resolved avatar URL in prod must be HTTPS", resolved.startsWith("https://"))
        } else {
            assertEquals("http://10.0.2.2:8080/avatars/user-123.png", resolved)
            assertTrue("Resolved avatar URL in dev must be HTTP", resolved.startsWith("http://"))
        }
    }
}

