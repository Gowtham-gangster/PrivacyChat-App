package com.privychat.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessExpirationMinutes;
    private final long refreshExpirationDays;
    private final String issuer;

    public JwtService(
            @Value("${privychat.jwt.secret}") String secret,
            @Value("${privychat.jwt.access-expiration-minutes:15}") long accessExpirationMinutes,
            @Value("${privychat.jwt.refresh-expiration-days:30}") long refreshExpirationDays,
            @Value("${privychat.jwt.issuer:https://privychat.example.com}") String issuer) {
        
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpirationMinutes = accessExpirationMinutes;
        this.refreshExpirationDays = refreshExpirationDays;
        this.issuer = issuer;
    }

    public String generateAccessToken(UUID userId, UUID deviceId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessExpirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim("deviceId", deviceId != null ? deviceId.toString() : null)
                .claim("type", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId, UUID deviceId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshExpirationDays, ChronoUnit.DAYS);

        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim("deviceId", deviceId != null ? deviceId.toString() : null)
                .claim("type", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public UUID extractUserId(String token) {
        Claims claims = validateToken(token);
        if (claims != null && claims.getSubject() != null) {
            return UUID.fromString(claims.getSubject());
        }
        return null;
    }

    public UUID extractDeviceId(String token) {
        Claims claims = validateToken(token);
        if (claims != null && claims.get("deviceId") != null) {
            String deviceIdStr = (String) claims.get("deviceId");
            return deviceIdStr != null ? UUID.fromString(deviceIdStr) : null;
        }
        return null;
    }

    public boolean isRefreshToken(String token) {
        Claims claims = validateToken(token);
        return claims != null && "REFRESH".equals(claims.get("type"));
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMinutes * 60;
    }
}

