package com.privychat.backend.service;

import com.privychat.backend.dto.call.IceConfigResponse;
import com.privychat.backend.dto.call.IceServerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class TurnCredentialService {

    private static final Logger log = LoggerFactory.getLogger(TurnCredentialService.class);
    private static final String HMAC_SHA1 = "HmacSHA1";

    private final String stunServerUrl;
    private final boolean turnEnabled;
    private final String turnHost;
    private final int turnPort;
    private final int turnTlsPort;
    private final String turnStaticSecret;
    private final String turnRealm;
    private final long ttlSeconds;
    private final String staticTurnServerUrl;
    private final String staticTurnUsername;
    private final String staticTurnPassword;

    public TurnCredentialService(
            String stunServerUrl,
            boolean turnEnabled,
            String turnHost,
            int turnPort,
            int turnTlsPort,
            String turnStaticSecret,
            String turnRealm,
            long ttlSeconds) {
        this(stunServerUrl, turnEnabled, turnHost, turnPort, turnTlsPort, turnStaticSecret, turnRealm, ttlSeconds, "", "", "");
    }

    @Autowired
    public TurnCredentialService(
            @Value("${privychat.webrtc.stun-server:stun:stun.l.google.com:19302}") String stunServerUrl,
            @Value("${privychat.webrtc.coturn.enabled:false}") boolean turnEnabled,
            @Value("${privychat.webrtc.coturn.host:}") String turnHost,
            @Value("${privychat.webrtc.coturn.port:3478}") int turnPort,
            @Value("${privychat.webrtc.coturn.tls-port:5349}") int turnTlsPort,
            @Value("${privychat.webrtc.coturn.static-secret:}") String turnStaticSecret,
            @Value("${privychat.webrtc.coturn.realm:privychat.app}") String turnRealm,
            @Value("${privychat.webrtc.coturn.ttl-seconds:86400}") long ttlSeconds,
            @Value("${privychat.webrtc.turn-server:}") String staticTurnServerUrl,
            @Value("${privychat.webrtc.turn-username:}") String staticTurnUsername,
            @Value("${privychat.webrtc.turn-secret:}") String staticTurnPassword) {
        this.stunServerUrl = stunServerUrl;
        this.turnEnabled = turnEnabled;
        this.turnHost = turnHost != null ? turnHost.trim() : "";
        this.turnPort = turnPort;
        this.turnTlsPort = turnTlsPort;
        this.turnStaticSecret = turnStaticSecret != null ? turnStaticSecret.trim() : "";
        this.turnRealm = turnRealm != null ? turnRealm.trim() : "privychat.app";
        this.ttlSeconds = ttlSeconds > 0 ? ttlSeconds : 86400;
        this.staticTurnServerUrl = staticTurnServerUrl != null ? staticTurnServerUrl.trim() : "";
        this.staticTurnUsername = staticTurnUsername != null ? staticTurnUsername.trim() : "";
        this.staticTurnPassword = staticTurnPassword != null ? staticTurnPassword.trim() : "";
    }

    public IceConfigResponse getIceServers(UUID userId) {
        List<IceServerDto> servers = new ArrayList<>();

        // 1. Always provide reliable public STUN server for direct P2P NAT discovery
        servers.add(new IceServerDto(List.of(stunServerUrl)));

        // 2. If Coturn is configured and enabled, generate ephemeral RFC 5766 TURN REST credentials
        boolean hasValidTurn = turnEnabled && !turnHost.isBlank() && !turnStaticSecret.isBlank();

        if (hasValidTurn) {
            try {
                long expiryTimestamp = Instant.now().getEpochSecond() + ttlSeconds;
                String username = expiryTimestamp + ":" + (userId != null ? userId.toString() : "anonymous");
                String credential = generateHmacSha1(turnStaticSecret, username);

                // UDP relay
                servers.add(new IceServerDto(
                        List.of("turn:" + turnHost + ":" + turnPort + "?transport=udp"),
                        username,
                        credential
                ));

                // TCP relay (for UDP-restricted corporate firewalls)
                servers.add(new IceServerDto(
                        List.of("turn:" + turnHost + ":" + turnPort + "?transport=tcp"),
                        username,
                        credential
                ));

                // TURNS over TLS (port 5349, for restrictive networks with DPI)
                servers.add(new IceServerDto(
                        List.of("turns:" + turnHost + ":" + turnTlsPort + "?transport=tcp"),
                        username,
                        credential
                ));

                log.debug("Generated ephemeral TURN credentials for user: {} against host: {}", userId, turnHost);
            } catch (Exception e) {
                log.error("Failed to generate ephemeral TURN credentials, falling back to STUN-only: {}", e.getMessage());
                return new IceConfigResponse(servers, ttlSeconds, false);
            }
        } else if (!staticTurnServerUrl.isBlank()) {
            servers.add(new IceServerDto(
                    List.of(staticTurnServerUrl),
                    staticTurnUsername.isBlank() ? null : staticTurnUsername,
                    staticTurnPassword.isBlank() ? null : staticTurnPassword
            ));
            log.info("Provided configured static TURN server: {}", staticTurnServerUrl);
            return new IceConfigResponse(servers, ttlSeconds, true);
        } else {
            log.info("External TURN server is not configured or disabled; providing STUN-only ICE candidates.");
        }

        return new IceConfigResponse(servers, ttlSeconds, hasValidTurn);
    }

    private String generateHmacSha1(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA1);
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA1);
        mac.init(keySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(rawHmac);
    }

    public boolean isTurnConfigured() {
        return turnEnabled && !turnHost.isBlank() && !turnStaticSecret.isBlank();
    }

    public String getTurnHost() {
        return turnHost;
    }
}
