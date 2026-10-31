package com.privychat.backend.service;

import com.privychat.backend.dto.call.IceConfigResponse;
import com.privychat.backend.dto.call.IceServerDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TurnCredentialServiceTest {

    private static final String STUN_URL = "stun:stun.l.google.com:19302";
    private static final String TEST_SECRET = "test_turn_secret_token_1234567890abcdef";
    private static final String TEST_HOST = "turn.privychat.example.com";

    @Test
    @DisplayName("When Coturn is disabled, return STUN-only configuration")
    void testCoturnDisabledReturnsStunOnly() {
        TurnCredentialService service = new TurnCredentialService(
                STUN_URL,
                false,
                TEST_HOST,
                3478,
                5349,
                TEST_SECRET,
                "privychat.app",
                86400
        );

        UUID userId = UUID.randomUUID();
        IceConfigResponse response = service.getIceServers(userId);

        assertNotNull(response);
        assertFalse(response.isTurnConfigured());
        assertEquals(1, response.getIceServers().size());
        assertEquals(STUN_URL, response.getIceServers().get(0).getUrls().get(0));
        assertNull(response.getIceServers().get(0).getUsername());
        assertNull(response.getIceServers().get(0).getCredential());
    }

    @Test
    @DisplayName("When Coturn host is empty, return STUN-only configuration")
    void testEmptyHostReturnsStunOnly() {
        TurnCredentialService service = new TurnCredentialService(
                STUN_URL,
                true,
                "",
                3478,
                5349,
                TEST_SECRET,
                "privychat.app",
                86400
        );

        UUID userId = UUID.randomUUID();
        IceConfigResponse response = service.getIceServers(userId);

        assertNotNull(response);
        assertFalse(response.isTurnConfigured());
        assertEquals(1, response.getIceServers().size());
    }

    @Test
    @DisplayName("When Coturn is enabled with valid host and secret, generate valid ephemeral RFC 5766 credentials")
    void testCoturnEnabledGeneratesEphemeralCredentials() throws Exception {
        TurnCredentialService service = new TurnCredentialService(
                STUN_URL,
                true,
                TEST_HOST,
                3478,
                5349,
                TEST_SECRET,
                "privychat.app",
                3600
        );

        UUID userId = UUID.randomUUID();
        IceConfigResponse response = service.getIceServers(userId);

        assertNotNull(response);
        assertTrue(response.isTurnConfigured());
        assertEquals(4, response.getIceServers().size());

        // 1. STUN
        assertEquals(STUN_URL, response.getIceServers().get(0).getUrls().get(0));

        // 2. TURN UDP
        IceServerDto udpTurn = response.getIceServers().get(1);
        assertTrue(udpTurn.getUrls().get(0).startsWith("turn:" + TEST_HOST + ":3478?transport=udp"));
        assertNotNull(udpTurn.getUsername());
        assertNotNull(udpTurn.getCredential());
        assertTrue(udpTurn.getUsername().endsWith(":" + userId));

        // Verify HMAC-SHA1 credential match
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        String expectedHmac = Base64.getEncoder().encodeToString(mac.doFinal(udpTurn.getUsername().getBytes(StandardCharsets.UTF_8)));
        assertEquals(expectedHmac, udpTurn.getCredential());

        // 3. TURN TCP
        IceServerDto tcpTurn = response.getIceServers().get(2);
        assertTrue(tcpTurn.getUrls().get(0).startsWith("turn:" + TEST_HOST + ":3478?transport=tcp"));
        assertEquals(udpTurn.getUsername(), tcpTurn.getUsername());
        assertEquals(udpTurn.getCredential(), tcpTurn.getCredential());

        // 4. TURNS TLS
        IceServerDto tlsTurn = response.getIceServers().get(3);
        assertTrue(tlsTurn.getUrls().get(0).startsWith("turns:" + TEST_HOST + ":5349?transport=tcp"));
        assertEquals(udpTurn.getUsername(), tlsTurn.getUsername());
        assertEquals(udpTurn.getCredential(), tlsTurn.getCredential());

        // Verify the static secret itself is never leaked in the credential or username
        assertNotEquals(TEST_SECRET, udpTurn.getCredential());
        assertFalse(udpTurn.getUsername().contains(TEST_SECRET));
    }

    @Test
    @DisplayName("When static TURN server is configured and Coturn dynamic is disabled, return static TURN config")
    void testStaticTurnCredentialsFallback() {
        TurnCredentialService service = new TurnCredentialService(
                STUN_URL,
                false,
                "",
                3478,
                5349,
                "",
                "privychat.app",
                86400,
                "turn:static.turn.example.com:3478",
                "static_user",
                "static_pass"
        );

        UUID userId = UUID.randomUUID();
        IceConfigResponse response = service.getIceServers(userId);

        assertNotNull(response);
        assertTrue(response.isTurnConfigured());
        assertEquals(2, response.getIceServers().size());
        assertEquals(STUN_URL, response.getIceServers().get(0).getUrls().get(0));
        assertEquals("turn:static.turn.example.com:3478", response.getIceServers().get(1).getUrls().get(0));
        assertEquals("static_user", response.getIceServers().get(1).getUsername());
        assertEquals("static_pass", response.getIceServers().get(1).getCredential());
    }
}

